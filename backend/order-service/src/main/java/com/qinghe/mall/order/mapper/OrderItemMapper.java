package com.qinghe.mall.order.mapper;

import com.qinghe.mall.order.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 订单明细Mapper
 */
@Mapper
public interface OrderItemMapper {

    /**
     * 批量插入订单明细
     */
    int insertBatch(@Param("items") List<OrderItem> items);

    /**
     * 根据订单ID查询订单明细列表
     */
    List<OrderItem> selectByOrderId(@Param("orderId") Long orderId);

    /**
     * 根据订单ID删除订单明细
     */
    int deleteByOrderId(@Param("orderId") Long orderId);
}
