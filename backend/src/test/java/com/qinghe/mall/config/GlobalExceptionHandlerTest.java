package com.qinghe.mall.config;

import com.qinghe.mall.exception.AuthException;
import com.qinghe.mall.exception.BusinessException;
import com.qinghe.mall.exception.RateLimitException;
import com.qinghe.mall.model.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 全局异常处理语义测试（P2-4 补齐：原零覆盖）。
 * 验证各异常类型到 HTTP 语义 code 的映射：
 * BusinessException→500 且透出 message、未预期 RuntimeException→500 脱敏文案（P2-11）、
 * AuthException→401/403（P1-8）、IllegalArgumentException→400、
 * 限流→429、参数缺失/类型不匹配/请求体不可读→400、未知异常→500。
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("业务异常 BusinessException 透出 message 且 code=500")
    void businessExceptionPassesMessage() {
        Result<Void> result = handler.handleBusinessException(new BusinessException("库存不足"));
        assertThat(result.getCode()).isEqualTo(500);
        assertThat(result.getMessage()).isEqualTo("库存不足");
    }

    @Test
    @DisplayName("未预期 RuntimeException 脱敏：不透出原始 message（P2-11）")
    void runtimeExceptionSanitizedTo500() {
        Result<Void> result = handler.handleRuntimeException(
                new RuntimeException("SQL syntax error near 'DROP TABLE user'"));
        assertThat(result.getCode()).isEqualTo(500);
        assertThat(result.getMessage()).isEqualTo("操作失败，请稍后重试");
        assertThat(result.getMessage()).doesNotContain("SQL");
    }

    @Test
    @DisplayName("鉴权异常映射 401/403（P1-8）")
    void authExceptionMapsToRealCode() {
        Result<Void> unauthorized = handler.handleAuth(AuthException.unauthorized("未登录"));
        assertThat(unauthorized.getCode()).isEqualTo(401);
        Result<Void> forbidden = handler.handleAuth(AuthException.forbidden("无商家权限"));
        assertThat(forbidden.getCode()).isEqualTo(403);
        assertThat(forbidden.getMessage()).isEqualTo("无商家权限");
    }

    @Test
    @DisplayName("IllegalArgumentException 语义修正为 400")
    void illegalArgumentMapsTo400() {
        Result<Void> result = handler.handleIllegalArgument(new IllegalArgumentException("商品不存在"));
        assertThat(result.getCode()).isEqualTo(400);
        assertThat(result.getMessage()).isEqualTo("商品不存在");
    }

    @Test
    @DisplayName("限流异常映射 429")
    void rateLimitMapsTo429() {
        Result<Void> result = handler.handleRateLimit(new RateLimitException("操作过于频繁，请稍后再试"));
        assertThat(result.getCode()).isEqualTo(429);
    }

    @Test
    @DisplayName("缺失参数映射 400 且提示含参数名")
    void missingParamMapsTo400() throws Exception {
        MissingServletRequestParameterException e =
                new MissingServletRequestParameterException("page", "int");
        Result<Void> result = handler.handleMissingParam(e);
        assertThat(result.getCode()).isEqualTo(400);
        assertThat(result.getMessage()).contains("page");
    }

    @Test
    @DisplayName("参数类型不匹配映射 400 且提示含参数名")
    void typeMismatchMapsTo400() {
        MethodArgumentTypeMismatchException e =
                new MethodArgumentTypeMismatchException("abc", Integer.class, "id", null, null);
        Result<Void> result = handler.handleTypeMismatch(e);
        assertThat(result.getCode()).isEqualTo(400);
        assertThat(result.getMessage()).contains("id");
    }

    @Test
    @DisplayName("请求体不可读映射 400")
    void notReadableMapsTo400() {
        Result<Void> result = handler.handleNotReadable(new HttpMessageNotReadableException("bad json"));
        assertThat(result.getCode()).isEqualTo(400);
    }

    @Test
    @DisplayName("未知异常映射 500 且不泄露内部信息")
    void unknownExceptionMapsTo500() {
        Result<Void> result = handler.handleException(new IllegalStateException("secret internal detail"));
        assertThat(result.getCode()).isEqualTo(500);
        assertThat(result.getMessage()).isEqualTo("系统繁忙，请稍后重试");
        assertThat(result.getMessage()).doesNotContain("secret");
    }
}
