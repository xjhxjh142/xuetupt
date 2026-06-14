package com.xuetupt.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.xuetupt.config.RabbitMQConfig;
import com.xuetupt.dto.Result;
import com.xuetupt.entity.Course;
import com.xuetupt.entity.CourseOrder;
import com.xuetupt.mapper.CourseMapper;
import com.xuetupt.mapper.CourseOrderMapper;
import com.xuetupt.service.ICourseService;
import com.xuetupt.utils.CacheClient;
import com.xuetupt.utils.FlowLimitService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.xuetupt.utils.RedisConstants.*;

/**
 * 课程服务实现
 * <p>
 * 实现课程查询、秒杀预约、支付等功能。
 * 使用布隆过滤器防止缓存穿透，Lua 脚本保证秒杀原子性，
 * RabbitMQ 异步削峰处理订单创建。
 */
@Slf4j
@Service
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements ICourseService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    @Autowired(required = false)
    private RedissonClient redissonClient;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private FlowLimitService flowLimitService;

    @Resource
    private CourseOrderMapper courseOrderMapper;

    // ========== 布隆过滤器 ==========
    /** 课程布隆过滤器，用于快速判断课程是否存在 */
    private BloomFilter<Long> courseBloomFilter;
    /** 布隆过滤器是否已初始化 */
    private volatile boolean bloomFilterInitialized = false;

    // ========== 秒杀 Lua 脚本 ==========
    /** 秒杀 Lua 脚本：扣减库存 + 记录订单 */
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    /**
     * 初始化布隆过滤器
     * <p>
     * 项目启动时从数据库加载所有课程 ID 到布隆过滤器中，
     * 用于拦截不存在的课程查询请求，防止缓存穿透。
     */
    @PostConstruct
    public void initBloomFilter() {
        if (bloomFilterInitialized) return;
        // 双重检查锁定，确保线程安全且只初始化一次
        synchronized (this) {
            if (bloomFilterInitialized) return;
            // 预计课程数量为10万，误判率为1%
            courseBloomFilter = BloomFilter.create(Funnels.longFunnel(), 100_000, 0.01);
            List<Course> courses = list();
            for (Course course : courses) {
                courseBloomFilter.put(course.getId());
            }
            bloomFilterInitialized = true;
            log.info("课程布隆过滤器初始化完成，加载 {} 条数据", courses.size());
        }
    }

    /**
     * 根据 ID 查询课程详情
     * <p>
     * 使用布隆过滤器 + 本地缓存 + Redis 缓存 + 数据库四级缓存策略。
     *
     * @param id 课程ID
     * @return 课程信息
     */
    @Override
    public Result queryCourseById(Long id) {
        // 布隆过滤器拦截不存在的课程
        if (!courseBloomFilter.mightContain(id)) {
            return Result.fail("课程不存在");
        }

        Course course = cacheClient.queryWithPassThrough(
                CACHE_COURSE_KEY, id, Course.class,
                this::getById,
                CACHE_COURSE_TTL, TimeUnit.MINUTES
        );

        if (course == null) {
            return Result.fail("课程不存在");
        }
        return Result.ok(course);
    }

    /**
     * 分页查询课程列表
     *
     * @param typeId  课程类型（可选）
     * @param current 当前页码
     * @return 课程列表（分页）
     */
    @Override
    public Result queryCourseList(Integer typeId, Integer current) {
        Page<Course> page = query()
                .eq(typeId != null && typeId > 0, "type", typeId)
                .orderByDesc("create_time")
                .page(new Page<>(current, 10));
        return Result.ok(page.getRecords(), page.getTotal());
    }

    /**
     * 获取热门课程列表（前8名）
     * <p>
     * 按库存紧张程度排序（库存越少越热门），
     * 同时从 Redis 获取实时库存数据。
     */
    @Override
    public Result getHotCourseList() {
        List<Course> courses = query()
                .eq("status", 1)
                .orderByAsc("stock")
                .last("limit 8")
                .list();
        // 补充 Redis 中的实时库存
        for (Course course : courses) {
            String stockStr = stringRedisTemplate.opsForValue().get(SECKILL_STOCK_KEY + course.getId());
            if (stockStr != null) {
                course.setStock(Integer.parseInt(stockStr));
            }
        }
        return Result.ok(courses);
    }


    /**
     * 秒杀预约课程
     * <p>
     * 流程：限流检查 → 布隆过滤器拦截 → Lua 脚本扣库存 →
     * RabbitMQ 异步创建订单 → 发送延迟消息用于支付超时检测
     *
     * @param courseId 课程ID
     * @param userId   用户ID
     * @return 预约结果
     */
    @Override
    public Result seckillCourse(Long courseId, Long userId) {
        // 1. 限流检查（滑动窗口限流）
        String limitKey = FLOW_LIMIT_KEY + "seckill:" + userId;
        if (!flowLimitService.tryAcquire(limitKey, FLOW_LIMIT_WINDOW_SIZE, FLOW_LIMIT_MAX_COUNT)) {
            return Result.fail("请求过于频繁，请稍后再试");
        }

        // 2. 布隆过滤器拦截不存在的课程
        if (!courseBloomFilter.mightContain(courseId)) {
            return Result.fail("课程不存在");
        }

        // 3. 执行 Lua 脚本扣减库存（保证原子性）
        String stockKey = SECKILL_STOCK_KEY + courseId;
        String orderKey = SECKILL_ORDER_KEY + courseId;
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                stockKey,
                orderKey,
                userId.toString()
        );

        int r = result != null ? result.intValue() : -1;
        if (r == 0) {
            return Result.fail("库存不足");
        }
        // r == 1 表示扣库存成功，订单记录成功
        if (r == 2) {
            return Result.fail("您已预约过该课程，请勿重复预约");
        }

        // 4. 发送消息到 RabbitMQ 异步创建订单（削峰填谷）
        Map<String, Object> msg = new HashMap<>();
        msg.put("courseId", courseId);
        msg.put("userId", userId);
        msg.put("orderNo", "CO" + System.currentTimeMillis() + RandomUtil.randomNumbers(4));
        rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE, RabbitMQConfig.ORDER_ROUTING_KEY, JSONUtil.toJsonStr(msg));

        // 5. 发送延迟消息用于支付超时检测（30分钟未支付自动取消）
        rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE, RabbitMQConfig.DELAY_ROUTING_KEY, JSONUtil.toJsonStr(msg));

        return Result.ok("预约成功，请在30分钟内完成支付");
    }

    /**
     * 支付订单
     *
     * @param orderNo 订单号
     * @param userId  用户ID
     * @return 支付结果
     */
    @Override
    public Result payCourse(String orderNo, Long userId) {
        QueryWrapper<CourseOrder> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_no", orderNo).eq("user_id", userId);
        CourseOrder order = courseOrderMapper.selectOne(queryWrapper);
        if (order == null) {
            return Result.fail("订单不存在");
        }
        if (order.getStatus() != 0) {
            return Result.fail("订单状态异常");
        }

        order.setStatus(1);
        order.setPayTime(LocalDateTime.now());
        courseOrderMapper.updateById(order);

        return Result.ok("支付成功");
    }

    /**
     * 初始化秒杀库存到 Redis
     * <p>
     * 项目启动时将数据库中的课程库存同步到 Redis，
     * 供秒杀 Lua 脚本使用。
     */
    @PostConstruct
    public void initSeckillStock() {
        List<Course> courses = list();
        for (Course course : courses) {
            if (course.getStock() != null && course.getStock() > 0) {
                stringRedisTemplate.opsForValue().set(
                        SECKILL_STOCK_KEY + course.getId(),
                        String.valueOf(course.getStock())
                );
            }
        }
        log.info("秒杀库存初始化完成，共加载 {} 门课程", courses.size());
    }
}
