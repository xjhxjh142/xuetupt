package com.xuetupt.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xuetupt.config.RabbitMQConfig;
import com.xuetupt.dto.Result;
import com.xuetupt.entity.CourseOrder;
import com.xuetupt.entity.Material;
import com.xuetupt.mapper.CourseOrderMapper;
import com.xuetupt.mapper.MaterialMapper;
import com.xuetupt.service.IMaterialService;
import com.xuetupt.utils.CacheClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.xuetupt.utils.RedisConstants.*;

/**
 * 学习资料服务实现
 * <p>
 * 实现资料查询、购买、下载链接生成等功能。
 * 使用缓存穿透防护策略，RabbitMQ 异步处理订单。
 */
@Service
public class MaterialServiceImpl extends ServiceImpl<MaterialMapper, Material> implements IMaterialService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private CourseOrderMapper courseOrderMapper;

    /**
     * 根据 ID 查询资料详情
     * <p>
     * 使用缓存穿透防护策略查询。
     *
     * @param id 资料ID
     * @return 资料信息
     */
    @Override
    public Result queryMaterialById(Long id) {
        Material material = cacheClient.queryWithPassThrough(
                CACHE_MATERIAL_KEY, id, Material.class,
                this::getById,
                CACHE_MATERIAL_TTL, TimeUnit.MINUTES
        );
        if (material == null) {
            return Result.fail("资料不存在");
        }
        return Result.ok(material);
    }

    /**
     * 分页查询资料列表
     *
     * @param type    资料类型（可选）
     * @param current 当前页码
     * @return 资料列表（分页）
     */
    @Override
    public Result queryMaterialList(Integer type, Integer current) {
        Page<Material> page = query()
                .eq(type != null && type > 0, "type", type)
                .orderByDesc("create_time")
                .page(new Page<>(current, 10));
        return Result.ok(page.getRecords(), page.getTotal());
    }

    /**
     * 秒杀购买资料
     * <p>
     * 简化版秒杀：直接检查库存并发送消息到 RabbitMQ 异步创建订单。
     *
     * @param materialId 资料ID
     * @param userId     用户ID
     * @return 购买结果
     */
    @Override
    public Result seckillMaterial(Long materialId, Long userId) {
        // 简化版秒杀：直接检查库存并创建订单
        Material material = getById(materialId);
        if (material == null || material.getStock() <= 0) {
            return Result.fail("库存不足");
        }

        // 异步创建订单
        Map<String, Object> msg = new HashMap<>();
        msg.put("materialId", materialId);
        msg.put("userId", userId);
        msg.put("orderNo", "MO" + System.currentTimeMillis() + RandomUtil.randomNumbers(4));
        rabbitTemplate.convertAndSend(RabbitMQConfig.MATERIAL_EXCHANGE, RabbitMQConfig.MATERIAL_ROUTING_KEY, JSONUtil.toJsonStr(msg));

        return Result.ok("购买成功");
    }

    /**
     * 获取资料下载链接
     * <p>
     * 验证订单已支付后，生成临时下载 token 并返回下载地址。
     *
     * @param orderNo 订单号
     * @param userId  用户ID
     * @return 下载地址
     */
    @Override
    public Result getDownloadUrl(String orderNo, Long userId) {
        CourseOrder order = courseOrderMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<CourseOrder>()
                        .eq("order_no", orderNo)
                        .eq("user_id", userId)
                        .eq("status", 1)
        );
        if (order == null) {
            return Result.fail("订单不存在或未支付");
        }

        // 生成临时下载 token
        String token = UUID.randomUUID().toString();
        stringRedisTemplate.opsForValue().set(DOWNLOAD_TOKEN_KEY + token, orderNo, DOWNLOAD_TOKEN_TTL, TimeUnit.MINUTES);

        return Result.ok("/api/material/file/" + token);
    }
}
