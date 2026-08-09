package com.qinghe.mall.controller;

import com.qinghe.mall.config.RateLimit;
import com.qinghe.mall.model.ChannelPayResult;
import com.qinghe.mall.model.Result;
import com.qinghe.mall.param.PaymentParam;
import com.qinghe.mall.service.PayService;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/pay")
public class PayController {

    @Autowired
    private PayService payService;

    private Long requireUserId(HttpServletRequest request) {
        Object userIdObj = request.getSession().getAttribute("userId");
        if (userIdObj == null) {
            return null;
        }
        return (Long) userIdObj;
    }

    /**
     * 创建支付：按渠道返回二维码（微信 Native）/ 模拟标记。
     */
    @RateLimit(rate = 20, message = "操作过于频繁，请稍后再试")
    @PostMapping("/create")
    public Result<ChannelPayResult> create(@RequestBody PaymentParam paymentParam, HttpServletRequest request) {
        Long userId = requireUserId(request);
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        // 业务异常由 GlobalExceptionHandler 统一转为 Result.fail
        return payService.createPay(userId, paymentParam.getOrderNumber(), paymentParam.getPayType());
    }

    /**
     * 模拟支付（仅模拟通道使用）。
     */
    @RateLimit(rate = 20, message = "操作过于频繁，请稍后再试")
    @PostMapping("/mockPay")
    public Result<String> mockPay(@RequestBody PaymentParam paymentParam, HttpServletRequest request) {
        Long userId = requireUserId(request);
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        return payService.mockPay(userId, paymentParam.getOrderNumber());
    }

    /**
     * 查询支付状态（前端轮询用），返回订单状态枚举。
     */
    @GetMapping("/query")
    public Result<String> query(@RequestParam("orderNumber") String orderNumber, HttpServletRequest request) {
        Long userId = requireUserId(request);
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        return payService.queryPayStatus(userId, orderNumber);
    }

    /**
     * 微信支付异步回调（微信服务器 → 本项目）。
     *
     * 注意：该接口不走 CSRF 白名单（已在 CsrfRefererFilter 放行），
     * 响应格式为微信规定的 {"code":"SUCCESS","message":"成功"}。
     */
    @PostMapping("/wechatNotify")
    public Map<String, String> wechatNotify(@RequestBody String body, HttpServletRequest request) {
        String serial = request.getHeader("Wechatpay-Serial");
        String timestamp = request.getHeader("Wechatpay-Timestamp");
        String nonce = request.getHeader("Wechatpay-Nonce");
        String signature = request.getHeader("Wechatpay-Signature");

        Result<String> result = payService.handleWechatNotify(serial, timestamp, nonce, signature, body);
        Map<String, String> resp = new HashMap<>();
        if (result.isSuccess()) {
            resp.put("code", "SUCCESS");
            resp.put("message", "成功");
        } else {
            resp.put("code", "FAIL");
            resp.put("message", result.getMessage());
        }
        return resp;
    }

    /**
     * 支付宝异步回调（支付宝服务器 → 本项目，form 表单）。
     *
     * 该接口已从 CSRF 校验中排除（服务器间调用）；验签失败返回 failure。
     */
    @PostMapping("/alipayNotify")
    public String alipayNotify(@RequestParam Map<String, String> params) {
        Result<String> result = payService.handleAlipayNotify(params);
        return result.isSuccess() ? "success" : "failure";
    }
}
