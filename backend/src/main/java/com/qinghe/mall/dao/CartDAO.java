package com.qinghe.mall.dao;

import com.qinghe.mall.dataobject.CartDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CartDAO {

    int insert(CartDO cartDO);

    int updateQuantity(@Param("id") Long id, @Param("quantity") Integer quantity);

    int updateSelected(@Param("id") Long id, @Param("selected") Integer selected);

    int deleteById(@Param("id") Long id);

    int deleteByUserAndDetail(@Param("userId") Long userId, @Param("productDetailId") String productDetailId);

    /** 删除指定用户购物车中所有已勾选条目（结算后调用） */
    int deleteSelectedByUserId(@Param("userId") Long userId);

    CartDO findByUserAndDetail(@Param("userId") Long userId, @Param("productDetailId") String productDetailId);

    List<CartDO> findByUserId(@Param("userId") Long userId);

    int countByUserId(@Param("userId") Long userId);
}
