package com.qinghe.mall.cart.mapper;

import com.qinghe.mall.cart.entity.Cart;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 购物车Mapper
 */
@Mapper
public interface CartMapper {

    /**
     * 根据用户ID查询购物车列表
     */
    List<Cart> findByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID和商品ID查询购物车项
     */
    Cart findByUserIdAndGoodsId(@Param("userId") Long userId, @Param("goodsId") Long goodsId);

    /**
     * 添加购物车
     */
    int insert(Cart cart);

    /**
     * 更新购物车数量
     */
    int updateQuantity(@Param("id") Long id, @Param("quantity") Integer quantity);

    /**
     * 更新选中状态
     */
    int updateChecked(@Param("id") Long id, @Param("checked") Integer checked);

    /**
     * 批量更新选中状态
     */
    int updateCheckedByUserId(@Param("userId") Long userId, @Param("checked") Integer checked);

    /**
     * 删除购物车项
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据用户ID删除所有购物车项
     */
    int deleteByUserId(@Param("userId") Long userId);
}
