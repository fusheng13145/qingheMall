package com.qinghe.mall.model;

import java.util.Arrays;
import java.util.List;

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

    /**
     * P1-7（2026-08-11）：已付营收统一口径——"已付款且未退款"的订单状态集合。
     * 订单支付后沿 已付款→已发货→已完成 流转，三个状态都表示真实成交；
     * 退款中/已退款/已关闭不计入营收。管理端看板、销售日报、商家统计一律以此为准，
     * 避免此前「看板只算已付款、商家端算三状态」导致的口径不一致。
     */
    public static List<String> paidRevenueStatuses() {
        return Arrays.asList(
                TRADE_PAID_SUCCESS.name(),
                TRADE_SHIPPED.name(),
                TRADE_COMPLETED.name());
    }
}
