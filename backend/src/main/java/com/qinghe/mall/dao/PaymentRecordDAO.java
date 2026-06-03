package com.qinghe.mall.dao;

import com.qinghe.mall.dataobject.PaymentRecordDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PaymentRecordDAO {

    int insert(PaymentRecordDO paymentRecordDO);

    PaymentRecordDO findByOrderNumber(@Param("orderNumber") String orderNumber);

    List<PaymentRecordDO> findByPayStatus(@Param("payStatus") String payStatus);

    int updatePayStatus(@Param("orderNumber") String orderNumber, @Param("payStatus") String payStatus, @Param("channelPaymentId") String channelPaymentId);
}
