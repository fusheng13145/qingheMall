package com.qinghe.mall.cart.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 购物车项VO
 */
@Data
public class CartItemVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 购物车ID
     */
    private Long id;

    /**
     * 商品ID
     */
    private Long goodsId;

    /**
     * SKU ID
     */
    private Long skuId;

    /**
     * 商品名称
     */
    private String goodsName;

    /**
     * 商品图片
     */
    private String goodsImage;

    /**
     * 商品价格
     */
    private BigDecimal price;

    /**
     * 数量
     */
    private Integer quantity;

    /**
     * 是否选中：0-否，1-是
     */
    private Integer checked;

    /**
     * 小计金额
     */
    private BigDecimal subtotal;
}
