package com.qinghe.mall.dao;

import com.qinghe.mall.dataobject.OrderDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrderDAO {

    int insert(OrderDO orderDO);

    OrderDO findByOrderNumber(@Param("orderNumber") String orderNumber);

    List<OrderDO> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);

    int updateStatus(@Param("orderNumber") String orderNumber, @Param("status") String status);

    List<OrderDO> findAll();
}
