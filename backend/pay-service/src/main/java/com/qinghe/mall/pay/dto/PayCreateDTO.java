package com.qinghe.mall.pay.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 创建支付请求参数
 */
@Data
public class PayCreateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 支付类型：1-微信支付 2-支付宝 3-银联支付
     */
    private Integer payType;
}