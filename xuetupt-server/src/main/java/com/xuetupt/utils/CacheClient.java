package com.xuetupt.utils;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.function.Function;

import static com.xuetupt.utils.RedisConstants.*;

@Component
public class CacheClient {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // Caffeine 本地缓存
    private final Cache<String, Object> caffeineCache = Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    // 缓存重建线程池
    private static final ScheduledExecutorService CACHE_REBUILD_EXECUTOR = Executors.newScheduledThreadPool(10);

    /**
     * 缓存穿透防护：缓存空对象 + 布隆过滤器兜底
     */
    public <R, ID> R queryWithPassThrough(
            String keyPrefix, ID id, Class<R> type,
            Function<ID, R> dbFallback, Long ttl, TimeUnit unit) {

        String key = keyPrefix + id;
        String cacheKey = keyPrefix + id.toString();

        // 1. 查 Caffeine 本地缓存
        Object caffeineResult = caffeineCache.getIfPresent(cacheKey);
        if (caffeineResult != null) {
            return (R) caffeineResult;
        }

        // 2. 查 Redis
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json != null && !json.isEmpty()) {
            R result = JSONUtil.toBean(json, type);
            caffeineCache.put(cacheKey, result);
            return result;
        }
        if (json != null && json.isEmpty()) {
            // 缓存空对象
            return null;
        }

        // 3. 查数据库
        R r = dbFallback.apply(id);
        if (r == null) {
            // 缓存空对象，防止缓存穿透
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }

        // 4. 写入 Redis（带随机 TTL 防止雪崩）
        long randomTtl = ttl + RandomUtil.randomLong(CACHE_TTL_RANDOM_MIN, CACHE_TTL_RANDOM_MAX);
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(r), randomTtl, unit);
        caffeineCache.put(cacheKey, r);
        return r;
    }

    /**
     * 逻辑过期解决缓存击穿
     */
    public <R, ID> R queryWithLogicalExpire(
            String keyPrefix, ID id, Class<R> type,
            Function<ID, R> dbFallback, Long ttl, TimeUnit unit) {

        String key = keyPrefix + id;
        String cacheKey = keyPrefix + id.toString();

        // 1. 查 Caffeine
        Object caffeineResult = caffeineCache.getIfPresent(cacheKey);
        if (caffeineResult != null) {
            return (R) caffeineResult;
        }

        // 2. 查 Redis
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json == null || json.isEmpty()) {
            return null;
        }

        // 3. 判断是否逻辑过期
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        R r = JSONUtil.toBean((String) redisData.getData(), type);
        LocalDateTime expireTime = redisData.getExpireTime();

        if (expireTime.isAfter(LocalDateTime.now())) {
            // 未过期，直接返回
            caffeineCache.put(cacheKey, r);
            return r;
        }

        // 4. 已过期，缓存重建
        String lockKey = LOCK_ORDER_KEY + id;
        boolean isLock = tryLock(lockKey);
        if (isLock) {
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    R newR = dbFallback.apply(id);
                    RedisData newRedisData = new RedisData();
                    newRedisData.setData(JSONUtil.toJsonStr(newR));
                    newRedisData.setExpireTime(LocalDateTime.now().plusSeconds(ttl));
                    stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(newRedisData), ttl + 30, unit);
                    caffeineCache.put(cacheKey, newR);
                } finally {
                    unlock(lockKey);
                }
            });
        }

        return r;
    }

    /**
     * 延迟双删：更新数据库后先删缓存，延迟后再删一次
     */
    public void delayDoubleDelete(String keyPrefix, Long id) {
        String key = keyPrefix + id;
        String cacheKey = keyPrefix + id.toString();

        // 第一次删除
        stringRedisTemplate.delete(key);
        caffeineCache.invalidate(cacheKey);

        // 延迟后第二次删除
        CACHE_REBUILD_EXECUTOR.schedule(() -> {
            stringRedisTemplate.delete(key);
            caffeineCache.invalidate(cacheKey);
        }, CACHE_DELAY_DELETE_TIME, CACHE_DELAY_DELETE_UNIT);
    }

    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(flag);
    }

    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }
}
