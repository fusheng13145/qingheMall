package com.qinghe.mall.service;

import com.qinghe.mall.model.Result;
import com.qinghe.mall.param.PaymentParam;

public interface PayService {

    Result<String> pay(Long userId, PaymentParam paymentParam);

    Result<String> callback(String orderNumber);
}
