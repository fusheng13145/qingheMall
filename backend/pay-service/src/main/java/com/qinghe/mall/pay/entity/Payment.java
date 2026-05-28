package com.qinghe.mall.pay.entity;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付记录实体
 */
@Data
public class Payment implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 支付流水号
     */
    private String paymentNo;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 订单编号
     */
    private String orderSn;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 支付金额
     */
    private BigDecimal amount;

    /**
     * 支付类型：1-微信支付 2-支付宝 3-银联支付
     */
    private Integer payType;

    /**
     * 支付状态：0-待支付 1-已支付 2-已退款
     */
    private Integer status;

    /**
     * 支付时间
     */
    private LocalDateTime payTime;

    /**
     * 通知时间
     */
    private LocalDateTime notifyTime;

    /**
     * 通知数据
     */
    private String notifyData;

    /**
     * 第三方交易号
     */
    private String tradeNo;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}