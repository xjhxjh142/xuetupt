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
public class MaterialConsumer {

    @Resource
    private CourseOrderMapper courseOrderMapper;

    @RabbitListener(queues = RabbitMQConfig.MATERIAL_QUEUE)
    @Transactional
    public void handleMaterialOrder(String message) {
        log.info("收到资料订单消息: {}", message);
        try {
            Map<String, Object> msg = JSONUtil.toBean(message, Map.class);
            Long materialId = Long.valueOf(msg.get("materialId").toString());
            Long userId = Long.valueOf(msg.get("userId").toString());
            String orderNo = msg.get("orderNo").toString();

            CourseOrder order = new CourseOrder();
            order.setOrderNo(orderNo);
            order.setUserId(userId);
            order.setCourseId(materialId);
            order.setPrice(new BigDecimal("0.01"));
            order.setStatus(1); // 资料直接支付成功
            order.setPayTime(LocalDateTime.now());
            order.setCreateTime(LocalDateTime.now());
            courseOrderMapper.insert(order);

            log.info("资料订单创建成功: orderNo={}", orderNo);
        } catch (Exception e) {
            log.error("资料订单创建失败: {}", message, e);
        }
    }
}
