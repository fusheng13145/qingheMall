package com.qinghe.mall.exception;

/**
 * 鉴权/授权异常（P1-8，2026-08-11）。
 *
 * 背景：此前 MerchantController.checkMerchant 用 {@link RuntimeException} 表达
 * "未登录 / 无商家权限 / 未入驻"等语义，被 GlobalExceptionHandler 统一兜底为 code 500，
 * 与其余 Controller 的 401/403 语义不一致，前端也无法据此做跳转/提示分流。
 *
 * 本异常携带 HTTP 风格状态码：401=未登录（Unauthorized），403=已登录但无权限（Forbidden），
 * 由 GlobalExceptionHandler 映射为同构 Result.fail(code, message)。
 */
public class AuthException extends RuntimeException {

    private final int code;

    public AuthException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    /** 未登录（401） */
    public static AuthException unauthorized(String message) {
        return new AuthException(401, message);
    }

    /** 已登录但无权限 / 资质不满足（403） */
    public static AuthException forbidden(String message) {
        return new AuthException(403, message);
    }
}
