package com.qinghe.mall.order.entity;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体
 */
@Data
public class Order implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 订单ID
     */
    private Long id;

    /**
     * 订单编号
     */
    private String orderSn;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 订单总金额
     */
    private BigDecimal totalAmount;

    /**
     * 实际支付金额
     */
    private BigDecimal payAmount;

    /**
     * 运费金额
     */
    private BigDecimal freightAmount;

    /**
     * 优惠券金额
     */
    private BigDecimal couponAmount;

    /**
     * 订单状态：0-待付款，1-已付款，2-已发货，3-已完成，4-已取消
     */
    private Integer status;

    /**
     * 收货人姓名
     */
    private String receiver;

    /**
     * 收货人电话
     */
    private String receiverPhone;

    /**
     * 收货人地址
     */
    private String receiverAddress;

    /**
     * 订单备注
     */
    private String remark;

    /**
     * 支付时间
     */
    private LocalDateTime payTime;

    /**
     * 发货时间
     */
    private LocalDateTime deliveryTime;

    /**
     * 完成时间
     */
    private LocalDateTime finishTime;

    /**
     * 取消时间
     */
    private LocalDateTime cancelTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
