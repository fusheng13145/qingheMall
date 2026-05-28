package com.qinghe.mall.cart.entity;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 购物车实体
 */
@Data
public class Cart implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 购物车ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

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
    private java.math.BigDecimal price;

    /**
     * 数量
     */
    private Integer quantity;

    /**
     * 是否选中：0-否，1-是
     */
    private Integer checked;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
