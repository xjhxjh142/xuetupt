package com.xuetupt.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 全局 AOP 日志切面
 * - 自动为每个请求注入 traceId (correlation_id)
 * - 记录 Controller 层所有请求的入参与出参
 * - 记录 Service 层关键方法的耗时
 * - 记录秒杀、缓存等关键操作
 */
@Aspect
@Component
public class LogAspect {

    private static final Logger log = LoggerFactory.getLogger("xuetupt");
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 切点：所有 Controller
     */
    @Pointcut("execution(* com.xuetupt.controller.*.*(..))")
    public void controllerPointcut() {}

    /**
     * 切点：所有 Service 实现类
     */
    @Pointcut("execution(* com.xuetupt.service.impl.*.*(..))")
    public void servicePointcut() {}

    /**
     * 切点：秒杀相关方法
     */
    @Pointcut("execution(* com.xuetupt.service.impl.CourseServiceImpl.seckillCourse(..))")
    public void seckillPointcut() {}

    /**
     * 切点：缓存操作
     */
    @Pointcut("execution(* com.xuetupt.utils.CacheClient.*(..))")
    public void cachePointcut() {}

    /**
     * Controller 环绕通知：记录请求 + 响应 + 耗时
     */
    @Around("controllerPointcut()")
    public Object aroundController(ProceedingJoinPoint joinPoint) throws Throwable {
        // 设置 traceId (correlation_id)
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        MDC.put("traceId", traceId);

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String params = Arrays.stream(joinPoint.getArgs())
                .map(arg -> {
                    try {
                        return arg == null ? "null" : objectMapper.writeValueAsString(arg);
                    } catch (Exception e) {
                        return arg == null ? "null" : arg.toString();
                    }
                })
                .collect(Collectors.joining(", "));

        // 截断过长参数
        if (params.length() > 500) {
            params = params.substring(0, 500) + "...";
        }

        long start = System.currentTimeMillis();
        String resultStr = "";

        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;

            try {
                resultStr = objectMapper.writeValueAsString(result);
                if (resultStr.length() > 300) {
                    resultStr = resultStr.substring(0, 300) + "...";
                }
            } catch (Exception e) {
                resultStr = result == null ? "null" : result.getClass().getSimpleName();
            }

            log.info("[API] {} {} | args=[{}] | {}ms | result={}",
                    method, uri, params, elapsed, resultStr);

            return result;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("[API] {} {} | args=[{}] | {}ms | error={}",
                    method, uri, params, elapsed, e.getMessage());
            throw e;
        } finally {
            MDC.remove("traceId");
        }
    }

    /**
     * Service 耗时监控（仅记录超过 200ms 的慢调用）
     */
    @Around("servicePointcut()")
    public Object aroundService(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;

            if (elapsed > 200) {
                log.warn("[SLOW] {}.{} | {}ms", className, methodName, elapsed);
            } else if (elapsed > 50) {
                log.info("[SVC] {}.{} | {}ms", className, methodName, elapsed);
            }
            return result;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("[SVC] {}.{} | {}ms | error={}", className, methodName, elapsed, e.getMessage());
            throw e;
        }
    }

    /**
     * 秒杀操作专用日志
     */
    @Around("seckillPointcut()")
    public Object aroundSeckill(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        long courseId = args.length > 0 ? (long) args[0] : -1;
        Long userId = args.length > 1 ? (Long) args[1] : null;

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;
            log.info("[SECKILL] courseId={} userId={} | {}ms | result={}",
                    courseId, userId, elapsed, result);
            return result;
        } catch (Exception e) {
            log.error("[SECKILL] courseId={} userId={} | error={}",
                    courseId, userId, e.getMessage());
            throw e;
        }
    }

    /**
     * 缓存操作日志
     */
    @Around("cachePointcut()")
    public Object aroundCache(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String key = joinPoint.getArgs().length > 0 ? String.valueOf(joinPoint.getArgs()[0]) : "";

        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long elapsed = System.currentTimeMillis() - start;

        boolean hit = result != null;
        log.info("[CACHE] {}(key={}) | hit={} | {}ms", methodName, key, hit, elapsed);
        return result;
    }
}
