package com.qinghe.mall.config;

import com.qinghe.mall.exception.RateLimitException;
import com.qinghe.mall.model.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 全局异常处理（M3-7）。
 *
 * 统一将异常转换为 Result<T> 结构，与业务失败（Result.fail）保持同构：
 * - 业务异常 RuntimeException：code 500 + message
 * - 限流异常 RateLimitException：code 429 + 提示（P2-3）
 * - 参数类异常（缺失/类型不匹配/JSON 不可读）：code 400 + 友好提示
 * - 未预期异常：code 500 + 通用提示（日志记录完整堆栈）
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RuntimeException.class)
    public Result<Void> handleRuntimeException(RuntimeException e) {
        // 业务异常：透出 message 给前端（业务提示依赖该语义），同时记录日志便于排查（P2-4）
        log.warn("业务异常: {}", e.getMessage(), e);
        return Result.fail(e.getMessage());
    }

    @ExceptionHandler(RateLimitException.class)
    public Result<Void> handleRateLimit(RateLimitException e) {
        // 限流：HTTP 语义 429 Too Many Requests
        return Result.fail(429, e.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return Result.fail(400, "缺少必要参数: " + e.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return Result.fail(400, "参数格式不正确: " + e.getName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleNotReadable(HttpMessageNotReadableException e) {
        return Result.fail(400, "请求体格式错误");
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("未预期异常", e);
        return Result.fail(500, "系统繁忙，请稍后重试");
    }
}
