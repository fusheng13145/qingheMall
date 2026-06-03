package com.qinghe.mall.controller;

import com.qinghe.mall.model.Result;
import com.qinghe.mall.param.PaymentParam;
import com.qinghe.mall.service.PayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/pay")
public class PayController {

    @Autowired
    private PayService payService;

    @PostMapping("/pay")
    public Result<String> pay(@RequestBody PaymentParam paymentParam, HttpServletRequest request) {
        Object userIdObj = request.getSession().getAttribute("userId");
        if (userIdObj == null) {
            return Result.fail(401, "未登录");
        }
        Long userId = (Long) userIdObj;
        return payService.pay(userId, paymentParam);
    }

    @PostMapping("/callback")
    public Result<String> callback(@RequestBody PaymentParam paymentParam) {
        return payService.callback(paymentParam.getOrderNumber());
    }
}
