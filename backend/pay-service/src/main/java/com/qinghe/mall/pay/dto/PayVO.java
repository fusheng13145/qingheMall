package com.qinghe.mall.pay.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 支付信息VO
 */
@Data
public class PayVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 支付记录ID
     */
    private Long paymentId;

    /**
     * 订单编号
     */
    private String orderSn;

    /**
     * 支付金额
     */
    private BigDecimal amount;

    /**
     * 支付类型：1-微信支付 2-支付宝 3-银联支付
     */
    private Integer payType;

    /**
     * 支付二维码链接
     */
    private String qrCode;

    /**
     * 支付状态：0-待支付 1-已支付 2-已退款
     */
    private Integer payStatus;
}