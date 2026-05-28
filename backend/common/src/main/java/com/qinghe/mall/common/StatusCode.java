package com.qinghe.mall.common;

/**
 * 响应状态码常量
 */
public class StatusCode {

    // 成功
    public static final int SUCCESS = 200;

    // 客户端错误
    public static final int PARAMS_ERROR = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;

    // 服务器错误
    public static final int SERVER_ERROR = 500;

    // ========== 业务错误码 ==========

    // 用户服务错误码 (1001-1999)
    public static final int USER_NOT_FOUND = 1001;
    public static final int USER_ALREADY_EXISTS = 1002;
    public static final int USERNAME_PASSWORD_ERROR = 1003;
    public static final int TOKEN_EXPIRED = 1004;
    public static final int TOKEN_INVALID = 1005;
    public static final int USER_DISABLED = 1006;

    // 商品服务错误码 (2001-2999)
    public static final int GOODS_NOT_FOUND = 2001;
    public static final int GOODS_STOCK_NOT_ENOUGH = 2002;
    public static final int GOODS_OFF_SHELF = 2003;
    public static final int GOODS_PRICE_CHANGED = 2004;

    // 购物车服务错误码 (3001-3999)
    public static final int CART_EMPTY = 3001;
    public static final int CART_GOODS_NOT_FOUND = 3002;
    public static final int CART_GOODS_STOCK_NOT_ENOUGH = 3003;

    // 订单服务错误码 (4001-4999)
    public static final int ORDER_NOT_FOUND = 4001;
    public static final int ORDER_STATUS_ERROR = 4002;
    public static final int ORDER_PRICE_CHANGED = 4003;
    public static final int ORDER_CANCELLED = 4004;
    public static final int ORDER_COMPLETED = 4005;

    // 支付服务错误码 (5001-5999)
    public static final int PAY_FAILED = 5001;
    public static final int PAY_TIMEOUT = 5002;
    public static final int PAY_CANCELLED = 5003;
    public static final int PAY_AMOUNT_ERROR = 5004;
    public static final int PAY_ORDER_NOT_FOUND = 5005;

    // 营销服务错误码 (6001-6999)
    public static final int COUPON_NOT_FOUND = 6001;
    public static final int COUPON_EXPIRED = 6002;
    public static final int COUPON_USED = 6003;
    public static final int COUPON_LIMIT_REACHED = 6004;
    public static final int PROMOTION_NOT_FOUND = 6005;
    public static final int PROMOTION_NOT_STARTED = 6006;
    public static final int PROMOTION_ENDED = 6007;
}
