package com.qinghe.mall.goods.vo;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品详情VO
 */
@Data
public class GoodsDetailVO implements Serializable {
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
     * 分类名称
     */
    private String categoryName;

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
     * 商品轮播图列表
     */
    private List<String> images;

    /**
     * 商品描述
     */
    private String description;

    /**
     * 销量
     */
    private Integer sales;

    /**
     * 商品状态: 0-下架, 1-上架
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
