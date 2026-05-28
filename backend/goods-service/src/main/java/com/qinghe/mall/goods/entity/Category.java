package com.qinghe.mall.goods.entity;

import lombok.Data;
import java.io.Serializable;

/**
 * 商品分类实体
 */
@Data
public class Category implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 分类ID
     */
    private Long id;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 父分类ID
     */
    private Long parentId;

    /**
     * 分类层级(1-一级, 2-二级, 3-三级)
     */
    private Integer level;

    /**
     * 分类图标
     */
    private String icon;

    /**
     * 排序
     */
    private Integer sort;
}
