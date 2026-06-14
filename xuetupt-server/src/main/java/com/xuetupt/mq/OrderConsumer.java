package com.xuetupt.mq;

import cn.hutool.json.JSONUtil;
import com.xuetupt.config.RabbitMQConfig;
import com.xuetupt.entity.CourseOrder;
import com.xuetupt.mapper.CourseOrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
public class OrderConsumer {

    @Resource
    private CourseOrderMapper courseOrderMapper;

    /**
     * 秒杀订单创建（异步削峰）
     */
    @RabbitListener(queues = RabbitMQConfig.ORDER_QUEUE)
    @Transactional
    public void handleSeckillOrder(String message) {
        log.info("收到秒杀订单消息: {}", message);
        try {
            Map<String, Object> msg = JSONUtil.toBean(message, Map.class);
            Long courseId = Long.valueOf(msg.get("courseId").toString());
            Long userId = Long.valueOf(msg.get("userId").toString());
            String orderNo = msg.get("orderNo").toString();

            CourseOrder order = new CourseOrder();
            order.setOrderNo(orderNo);
            order.setUserId(userId);
            order.setCourseId(courseId);
            order.setPrice(new BigDecimal("0.01"));
            order.setStatus(0);
            order.setCreateTime(LocalDateTime.now());
            courseOrderMapper.insert(order);

            log.info("订单创建成功: orderNo={}, userId={}, courseId={}", orderNo, userId, courseId);
        } catch (Exception e) {
            log.error("订单创建失败: {}", message, e);
        }
    }

    /**
     * 支付超时处理（死信队列）
     */
    @RabbitListener(queues = RabbitMQConfig.DLX_QUEUE)
    @Transactional
    public void handlePayTimeout(String message) {
        log.info("收到支付超时消息: {}", message);
        try {
            Map<String, Object> msg = JSONUtil.toBean(message, Map.class);
            String orderNo = msg.get("orderNo").toString();

            CourseOrder order = courseOrderMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<CourseOrder>()
                            .eq("order_no", orderNo)
                            .eq("status", 0)
            );
            if (order != null) {
                order.setStatus(2); // 已取消
                courseOrderMapper.updateById(order);
                log.info("订单已取消: orderNo={}", orderNo);
            }
        } catch (Exception e) {
            log.error("支付超时处理失败: {}", message, e);
        }
    }
}
