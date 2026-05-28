package com.qinghe.mall.cart.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 添加购物车参数
 */
@Data
public class CartAddDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 商品ID
     */
    private Long goodsId;

    /**
     * 数量
     */
    private Integer quantity;
}
