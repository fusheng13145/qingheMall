package com.qinghe.mall.order.vo;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单明细VO
 */
@Data
public class OrderItemVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 订单明细ID
     */
    private Long id;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 商品ID
     */
    private Long goodsId;

    /**
     * 商品名称
     */
    private String goodsName;

    /**
     * 商品图片
     */
    private String goodsImage;

    /**
     * 商品单价
     */
    private BigDecimal price;

    /**
     * 购买数量
     */
    private Integer quantity;

    /**
     * 小计金额
     */
    private BigDecimal subtotal;
}
