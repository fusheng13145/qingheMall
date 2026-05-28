package com.qinghe.mall.pay.mapper;

import com.qinghe.mall.pay.entity.Payment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 支付记录Mapper
 */
@Mapper
public interface PaymentMapper {

    /**
     * 插入支付记录
     */
    int insert(Payment payment);

    /**
     * 根据ID查询
     */
    Payment selectById(@Param("id") Long id);

    /**
     * 根据订单ID查询
     */
    Payment selectByOrderId(@Param("orderId") Long orderId);

    /**
     * 更新支付状态
     */
    int updatePayStatus(@Param("id") Long id, @Param("payStatus") Integer payStatus, 
                        @Param("tradeNo") String tradeNo, @Param("callbackContent") String callbackContent);

    /**
     * 根据订单ID更新状态
     */
    int updateByOrderId(@Param("orderId") Long orderId, @Param("payStatus") Integer payStatus,
                        @Param("tradeNo") String tradeNo, @Param("callbackContent") String callbackContent);
}