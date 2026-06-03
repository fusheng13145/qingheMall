package com.qinghe.mall.service;

import com.qinghe.mall.model.PaymentRecord;
import java.util.List;

public interface PaymentRecordService {

    int insert(PaymentRecord paymentRecord);

    PaymentRecord findByOrderNumber(String orderNumber);

    List<PaymentRecord> findByPayStatus(String payStatus);

    boolean updatePayStatus(String orderNumber, String payStatus, String channelPaymentId);
}
