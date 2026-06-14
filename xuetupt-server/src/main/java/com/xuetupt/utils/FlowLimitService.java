package com.xuetupt.utils;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;

@Component
public class FlowLimitService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private DefaultRedisScript<Long> slideWindowScript;

    @PostConstruct
    public void init() {
        slideWindowScript = new DefaultRedisScript<>();
        slideWindowScript.setLocation(new ClassPathResource("slide-window.lua"));
        slideWindowScript.setResultType(Long.class);
    }

    /**
     * 滑动窗口限流
     * @param key 限流key
     * @param windowSize 窗口大小（秒）
     * @param maxCount 最大请求数
     * @return true=放行，false=限流
     */
    public boolean tryAcquire(String key, Long windowSize, Long maxCount) {
        Long result = stringRedisTemplate.execute(
                slideWindowScript,
                Collections.singletonList(key),
                windowSize.toString(),
                maxCount.toString()
        );
        return result != null && result == 1;
    }
}
