package com.xuetupt.config;

import com.xuetupt.dto.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理
 * <p>
 * 捕获所有 Controller 层抛出的 RuntimeException，
 * 统一返回标准错误响应，避免直接暴露异常堆栈给前端。
 */
@RestControllerAdvice
public class WebExceptionAdvice {

    /**
     * 处理运行时异常
     *
     * @param e 运行时异常
     * @return 统一错误响应
     */
    @ExceptionHandler(RuntimeException.class)
    public Result handleRuntimeException(RuntimeException e) {
        e.printStackTrace();
        return Result.fail("服务器异常：" + e.getMessage());
    }
}
