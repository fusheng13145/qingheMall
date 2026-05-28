package com.qinghe.mall.goods.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商品列表项DTO
 */
@Data
public class GoodsDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 商品ID
     */
    private Long id;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 商品价格
     */
    private BigDecimal price;

    /**
     * 商品原价
     */
    private BigDecimal originalPrice;

    /**
     * 库存数量
     */
    private Integer stock;

    /**
     * 商品主图
     */
    private String image;

    /**
     * 销量
     */
    private Integer sales;

    /**
     * 商品状态: 0-下架, 1-上架
     */
    private Integer status;
}
