package com.xuetupt.config;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 分布式锁配置
 * <p>
 * 配置 Redisson 客户端连接 Redis 服务器，
 * 用于实现分布式锁和秒杀场景下的并发控制。
 * 如果 Redis 连接失败，会自动降级运行。
 */
@Slf4j
@Configuration
public class RedissonConfig {

    /** Redis 主机地址 */
    @Value("${spring.redis.host}")
    private String redisHost;

    /** Redis 端口号 */
    @Value("${spring.redis.port}")
    private int redisPort;

    /** Redis 密码 */
    @Value("${spring.redis.password}")
    private String redisPassword;

    /**
     * 创建 Redisson 客户端
     * <p>
     * 通过 redisson.enabled 配置控制是否启用，
     * 默认启用。连接失败时返回 null，不影响主流程。
     */
    @Bean
    @ConditionalOnProperty(name = "redisson.enabled", havingValue = "true", matchIfMissing = true)
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + redisHost + ":" + redisPort)
                .setPassword(redisPassword)
                .setConnectionPoolSize(10)
                .setConnectionMinimumIdleSize(5)
                .setConnectTimeout(3000)
                .setTimeout(3000)
                .setRetryAttempts(1);
        try {
            RedissonClient client = Redisson.create(config);
            client.getKeys().count();
            log.info("Redisson 连接 Redis 成功: {}:{}", redisHost, redisPort);
            return client;
        } catch (Exception e) {
            log.warn("Redisson 连接 Redis 失败: {}:{}, 已降级运行（分布式锁功能不可用）", redisHost, redisPort);
            return null;
        }
    }
}
