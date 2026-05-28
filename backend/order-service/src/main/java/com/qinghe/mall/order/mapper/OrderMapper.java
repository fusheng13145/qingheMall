package com.qinghe.mall.order.mapper;

import com.qinghe.mall.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 订单Mapper
 */
@Mapper
public interface OrderMapper {

    /**
     * 创建订单
     */
    int insert(Order order);

    /**
     * 更新订单
     */
    int update(Order order);

    /**
     * 根据ID查询订单
     */
    Order selectById(@Param("id") Long id);

    /**
     * 根据订单编号查询订单
     */
    Order selectByOrderSn(@Param("orderSn") String orderSn);

    /**
     * 查询用户订单列表
     */
    List<Order> selectList(@Param("userId") Long userId, @Param("status") Integer status);

    /**
     * 更新订单状态
     */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 根据用户ID和状态统计订单数量
     */
    int countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") Integer status);
}
