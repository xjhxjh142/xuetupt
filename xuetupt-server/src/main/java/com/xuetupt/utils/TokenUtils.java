package com.xuetupt.utils;

import cn.hutool.core.util.StrUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

import static com.xuetupt.utils.RedisConstants.LOGIN_TOKEN_KEY;

/**
 * Token 工具类
 * <p>
 * 从请求头中的 authorization token 解析出用户 ID。
 * Token 存储在 Redis 中，格式为：login:token:{token} -> {userId}
 */
@Component
public class TokenUtils {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 从 token 中解析用户 ID
     *
     * @param token 请求头中的 authorization 值
     * @return 用户 ID，如果 token 无效则返回 null
     */
    public Long getUserIdFromToken(String token) {
        if (StrUtil.isBlank(token)) {
            return null;
        }
        // 兼容 "Bearer " 前缀
        String actualToken = token;
        if (token.startsWith("Bearer ")) {
            actualToken = token.substring(7);
        }
        String userIdStr = stringRedisTemplate.opsForValue().get(LOGIN_TOKEN_KEY + actualToken);
        if (StrUtil.isBlank(userIdStr)) {
            return null;
        }
        return Long.parseLong(userIdStr);
    }
}
