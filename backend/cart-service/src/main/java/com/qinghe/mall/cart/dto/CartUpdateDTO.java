package com.qinghe.mall.cart.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 更新购物车数量参数
 */
@Data
public class CartUpdateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 购物车ID
     */
    private Long cartId;

    /**
     * 数量
     */
    private Integer quantity;
}
