package com.xuetupt.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ 消息队列配置
 * <p>
 * 配置了四种消息队列：
 * <ul>
 *   <li>秒杀订单队列 - 异步处理秒杀订单创建</li>
 *   <li>延迟队列（死信队列） - 处理支付超时取消订单</li>
 *   <li>日志队列 - 异步记录操作日志</li>
 *   <li>资料下载队列 - 异步处理资料购买订单</li>
 * </ul>
 */
@Configuration
public class RabbitMQConfig {

    // ========== 秒杀订单交换机与队列 ==========
    /** 秒杀订单交换机名称 */
    public static final String ORDER_EXCHANGE = "order.exchange";
    /** 秒杀订单队列名称 */
    public static final String ORDER_QUEUE = "order.seckill.queue";
    /** 秒杀订单路由键 */
    public static final String ORDER_ROUTING_KEY = "order.seckill";

    // ========== 延迟队列（支付超时） ==========
    /** 延迟队列交换机名称（死信交换机） */
    public static final String DELAY_EXCHANGE = "delay.exchange";
    /** 延迟队列名称（消息过期后进入死信队列） */
    public static final String DELAY_QUEUE = "order.delay.queue";
    /** 死信队列名称（处理支付超时） */
    public static final String DLX_QUEUE = "order.dlx.queue";
    /** 延迟队列路由键 */
    public static final String DELAY_ROUTING_KEY = "order.delay";
    /** 死信队列路由键 */
    public static final String DLX_ROUTING_KEY = "order.dlx";
    /** 支付超时时间：30分钟 */
    public static final long DELAY_TTL = 30 * 60 * 1000;

    // ========== 日志队列 ==========
    /** 日志交换机名称 */
    public static final String LOG_EXCHANGE = "log.exchange";
    /** 日志队列名称 */
    public static final String LOG_QUEUE = "log.dialog.queue";
    /** 日志路由键 */
    public static final String LOG_ROUTING_KEY = "log.dialog";

    // ========== 资料下载队列 ==========
    /** 资料下载交换机名称 */
    public static final String MATERIAL_EXCHANGE = "material.exchange";
    /** 资料下载队列名称 */
    public static final String MATERIAL_QUEUE = "material.download.queue";
    /** 资料下载路由键 */
    public static final String MATERIAL_ROUTING_KEY = "material.download";

    // ===== 定义交换机 =====
    /** 创建秒杀订单交换机 */
    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE);
    }

    /** 创建延迟队列交换机（死信交换机） */
    @Bean
    public DirectExchange delayExchange() {
        return new DirectExchange(DELAY_EXCHANGE);
    }

    /** 创建日志交换机 */
    @Bean
    public DirectExchange logExchange() {
        return new DirectExchange(LOG_EXCHANGE);
    }

    /** 创建资料下载交换机 */
    @Bean
    public DirectExchange materialExchange() {
        return new DirectExchange(MATERIAL_EXCHANGE);
    }

    // ===== 定义队列 =====
    /** 创建秒杀订单队列 */
    @Bean
    public Queue orderQueue() {
        return QueueBuilder.durable(ORDER_QUEUE).build();
    }

    /**
     * 创建延迟队列
     * <p>
     * 消息过期后通过死信交换机转发到死信队列，
     * 用于处理支付超时取消订单。
     */
    @Bean
    public Queue delayQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", DELAY_EXCHANGE);
        args.put("x-dead-letter-routing-key", DLX_ROUTING_KEY);
        args.put("x-message-ttl", DELAY_TTL);
        return QueueBuilder.durable(DELAY_QUEUE).withArguments(args).build();
    }

    /** 创建死信队列（处理支付超时） */
    @Bean
    public Queue dlxQueue() {
        return QueueBuilder.durable(DLX_QUEUE).build();
    }

    /** 创建日志队列 */
    @Bean
    public Queue logQueue() {
        return QueueBuilder.durable(LOG_QUEUE).build();
    }

    /** 创建资料下载队列 */
    @Bean
    public Queue materialQueue() {
        return QueueBuilder.durable(MATERIAL_QUEUE).build();
    }

    // ===== 绑定关系 =====
    /** 绑定秒杀订单队列到交换机 */
    @Bean
    public Binding orderBinding(Queue orderQueue, DirectExchange orderExchange) {
        return BindingBuilder.bind(orderQueue).to(orderExchange).with(ORDER_ROUTING_KEY);
    }

    /** 绑定延迟队列到秒杀订单交换机 */
    @Bean
    public Binding delayBinding(Queue delayQueue, DirectExchange orderExchange) {
        return BindingBuilder.bind(delayQueue).to(orderExchange).with(DELAY_ROUTING_KEY);
    }

    /** 绑定死信队列到延迟交换机 */
    @Bean
    public Binding dlxBinding(Queue dlxQueue, DirectExchange delayExchange) {
        return BindingBuilder.bind(dlxQueue).to(delayExchange).with(DLX_ROUTING_KEY);
    }

    /** 绑定日志队列到日志交换机 */
    @Bean
    public Binding logBinding(Queue logQueue, DirectExchange logExchange) {
        return BindingBuilder.bind(logQueue).to(logExchange).with(LOG_ROUTING_KEY);
    }

    /** 绑定资料下载队列到资料交换机 */
    @Bean
    public Binding materialBinding(Queue materialQueue, DirectExchange materialExchange) {
        return BindingBuilder.bind(materialQueue).to(materialExchange).with(MATERIAL_ROUTING_KEY);
    }
}
