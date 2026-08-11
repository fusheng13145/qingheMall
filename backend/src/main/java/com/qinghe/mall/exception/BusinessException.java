package com.qinghe.mall.exception;

/**
 * 业务异常（P2-11 异常信息脱敏）。
 *
 * 语义：由业务代码"有意抛出"、消息可直接展示给用户的异常
 * （如「商品名称不能为空」「库存不足」）。
 *
 * 与 RuntimeException 的分工：
 * - BusinessException：{@code message} 会原样透出给前端（GlobalExceptionHandler）；
 * - 其他 RuntimeException：视为未预期异常，前端只看到通用文案，
 *   真实细节仅进日志，避免 SQL/类名/堆栈等内部信息泄露。
 *
 * 注意：继承 RuntimeException 以保持 @Transactional 默认回滚语义不变。
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
