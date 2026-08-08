package com.qinghe.mall.model;

public enum OrderStatus {

    WAIT_BUYER_PAY,
    TRADE_CLOSED,
    TRADE_PAID_SUCCESS,
    TRADE_PAID_FAILED,
    TRADE_SHIPPED,      // 已发货 / 待收货
    TRADE_COMPLETED,    // 已完成（交易成功）
    TRADE_REFUNDING,    // 退款中
    TRADE_REFUNDED;     // 已退款

    /**
     * 校验给定状态字符串是否为合法订单状态（用于 Admin 改单等入口的枚举约束，
     * 杜绝写入任意 status 字符串破坏订单状态机）。
     */
    public static boolean isValid(String status) {
        if (status == null) {
            return false;
        }
        for (OrderStatus value : values()) {
            if (value.name().equals(status)) {
                return true;
            }
        }
        return false;
    }
}
