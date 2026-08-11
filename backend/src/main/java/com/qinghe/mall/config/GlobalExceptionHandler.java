package com.qinghe.mall.config;

import com.qinghe.mall.exception.AuthException;
import com.qinghe.mall.exception.BusinessException;
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
 * - 鉴权异常 AuthException：code 401/403 + message（P1-8）
 * - 业务异常 BusinessException：code 500 + message（有意抛出，消息可展示给用户）
 * - 限流异常 RateLimitException：code 429 + 提示（P2-3）
 * - 参数类异常（缺失/类型不匹配/JSON 不可读）：code 400 + 友好提示
 * - 未预期 RuntimeException / Exception：code 500 + 通用提示（P2-11 脱敏，
 *   真实细节仅进日志，不向前端泄露 SQL/类名/堆栈等内部信息）
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AuthException.class)
    public Result<Void> handleAuth(AuthException e) {
        // P1-8：鉴权/授权失败按真实语义返回 401/403，不再被 RuntimeException 兜底成 500
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        // 业务异常：message 是有意写给用户的提示，可安全透出；记录日志便于排查（P2-4）
        log.warn("业务异常: {}", e.getMessage());
        return Result.fail(e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public Result<Void> handleRuntimeException(RuntimeException e) {
        // P2-11：未预期运行时异常不再把 getMessage() 透出前端（可能含 SQL/内部细节），
        // 完整堆栈仅记录到日志，前端统一看到脱敏文案
        log.error("未预期运行时异常", e);
        return Result.fail(500, "操作失败，请稍后重试");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgument(IllegalArgumentException e) {
        // P2：参数非法统一 400（原被 RuntimeException 兜底为 500，语义错误）
        return Result.fail(400, e.getMessage());
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
