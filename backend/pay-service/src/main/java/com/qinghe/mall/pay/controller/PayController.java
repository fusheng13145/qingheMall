package com.qinghe.mall.pay.controller;

import com.qinghe.mall.common.R;
import com.qinghe.mall.pay.dto.PayCreateDTO;
import com.qinghe.mall.pay.dto.PayVO;
import com.qinghe.mall.pay.service.PayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 支付控制器
 */
@RestController
@RequestMapping("/api/pay")
public class PayController {

    @Autowired
    private PayService payService;

    /**
     * 创建支付
     */
    @PostMapping("/create")
    public R<PayVO> createPay(@RequestBody PayCreateDTO payCreateDTO,
                              @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        // 模拟从请求头获取用户ID，实际项目中可从Token解析
        if (userId == null) {
            userId = 1L; // 默认测试用户
        }
        PayVO payVO = payService.createPay(payCreateDTO, userId);
        return R.success(payVO);
    }

    /**
     * 支付回调
     */
    @PostMapping("/callback/{payType}")
    public String callback(@PathVariable Integer payType,
                           @RequestParam Map<String, String> params) {
        return payService.callback(payType, params);
    }

    /**
     * 查询支付状态
     */
    @GetMapping("/status/{orderId}")
    public R<Integer> getPayStatus(@PathVariable Long orderId) {
        Integer payStatus = payService.getPayStatus(orderId);
        return R.success(payStatus);
    }
}