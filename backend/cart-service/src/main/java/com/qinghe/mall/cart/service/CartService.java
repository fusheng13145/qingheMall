package com.qinghe.mall.cart.service;

import com.qinghe.mall.cart.dto.CartAddDTO;
import com.qinghe.mall.cart.dto.CartItemVO;
import com.qinghe.mall.cart.dto.CartUpdateDTO;
import java.util.List;

/**
 * 购物车服务接口
 */
public interface CartService {

    /**
     * 添加购物车
     */
    void addCart(Long userId, CartAddDTO cartAddDTO);

    /**
     * 获取购物车列表
     */
    List<CartItemVO> getCartList(Long userId);

    /**
     * 更新购物车数量
     */
    void updateQuantity(Long userId, CartUpdateDTO cartUpdateDTO);

    /**
     * 删除购物车项
     */
    void deleteCart(Long userId, Long cartId);

    /**
     * 切换选中状态
     */
    void toggleChecked(Long userId, Long cartId);

    /**
     * 全选/取消全选
     */
    void selectAll(Long userId, Boolean checked);
}
