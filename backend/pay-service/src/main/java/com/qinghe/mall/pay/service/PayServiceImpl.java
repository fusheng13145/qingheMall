package com.qinghe.mall.pay.service;

import com.qinghe.mall.common.R;
import com.qinghe.mall.pay.dto.PayCreateDTO;
import com.qinghe.mall.pay.dto.PayVO;
import com.qinghe.mall.pay.entity.Payment;
import com.qinghe.mall.pay.mapper.PaymentMapper;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 支付服务实现类
 */
@Slf4j
@Service
public class PayServiceImpl implements PayService {

    @Autowired
    private PaymentMapper paymentMapper;

    @Autowired
    private OrderRemoteService orderRemoteService;

    @Override
    @GlobalTransactional(rollbackFor = Exception.class)
    @Transactional
    public PayVO createPay(PayCreateDTO payCreateDTO, Long userId) {
        Long orderId = payCreateDTO.getOrderId();
        Integer payType = payCreateDTO.getPayType();

        log.info("创建支付订单, orderId={}, payType={}, userId={}", orderId, payType, userId);

        // 防重复支付：检查是否已支付
        Payment existPayment = paymentMapper.selectByOrderId(orderId);
        if (existPayment != null && existPayment.getStatus() != null && existPayment.getStatus() == 1) {
            log.warn("订单已支付, orderId={}", orderId);
            throw new RuntimeException("订单已支付");
        }

        // 远程调用order-service获取订单信息
        R<Map<String, Object>> orderResult = orderRemoteService.getOrderById(orderId);
        if (orderResult == null || orderResult.getCode() != 200 || orderResult.getData() == null) {
            log.error("获取订单信息失败, orderId={}", orderId);
            throw new RuntimeException("获取订单信息失败");
        }

        Map<String, Object> orderInfo = orderResult.getData();
        String orderSn = (String) orderInfo.get("orderSn");
        java.math.BigDecimal amount = new java.math.BigDecimal(orderInfo.get("totalAmount").toString());

        // 生成支付流水号
        String paymentNo = "PAY" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8);

        // 创建支付记录
        Payment payment = new Payment();
        payment.setPaymentNo(paymentNo);
        payment.setOrderId(orderId);
        payment.setOrderSn(orderSn);
        payment.setUserId(userId);
        payment.setAmount(amount);
        payment.setPayType(payType);
        payment.setStatus(0); // 待支付
        payment.setCreateTime(LocalDateTime.now());
        payment.setUpdateTime(LocalDateTime.now());

        paymentMapper.insert(payment);

        // 生成支付二维码链接（模拟实现）
        String qrCode = generateQrCode(payment.getId(), payType, amount);

        // 构建返回VO
        PayVO payVO = new PayVO();
        payVO.setPaymentId(payment.getId());
        payVO.setOrderSn(orderSn);
        payVO.setAmount(amount);
        payVO.setPayType(payType);
        payVO.setQrCode(qrCode);
        payVO.setPayStatus(0);

        log.info("支付订单创建成功, paymentId={}", payment.getId());
        return payVO;
    }

    @Override
    @GlobalTransactional(rollbackFor = Exception.class)
    @Transactional
    public String callback(Integer payType, Map<String, String> params) {
        log.info("支付回调, payType={}, params={}", payType, params);

        String tradeNo = params.get("trade_no");
        String orderIdStr = params.get("order_id");
        String status = params.get("status");

        // 根据payType处理不同的回调
        // 实际项目中会根据不同的支付渠道解析不同的参数
        if ("SUCCESS".equals(status)) {
            // 使用orderId查询
            Long orderId = Long.parseLong(orderIdStr);
            // 更新支付状态
            Payment payment = paymentMapper.selectByOrderId(orderId);
            if (payment != null) {
                paymentMapper.updatePayStatus(payment.getId(), 1, tradeNo, params.toString());

                // 远程调用order-service更新订单状态
                R<?> result = orderRemoteService.updateOrderStatus(payment.getOrderId(), 2);
                if (result == null || result.getCode() != 200) {
                    log.error("更新订单状态失败, orderId={}", payment.getOrderId());
                    throw new RuntimeException("更新订单状态失败");
                }

                log.info("支付回调处理成功, orderId={}", orderId);
                return "success";
            }
        }

        return "fail";
    }

    @Override
    public Integer getPayStatus(Long orderId) {
        Payment payment = paymentMapper.selectByOrderId(orderId);
        if (payment == null) {
            return null;
        }
        return payment.getStatus();
    }

    /**
     * 生成支付二维码链接（模拟实现）
     */
    private String generateQrCode(Long paymentId, Integer payType, java.math.BigDecimal amount) {
        String qrCode = "";
        switch (payType) {
            case 1: // 微信支付
                qrCode = "weixin://wxpay/bizpay?orderId=" + paymentId + "&amount=" + amount;
                break;
            case 2: // 支付宝
                qrCode = "https://qr.alipay.com/bizpay?orderId=" + paymentId + "&amount=" + amount;
                break;
            case 3: // 银联支付
                qrCode = "https://unionpay.com/pay?orderId=" + paymentId + "&amount=" + amount;
                break;
            default:
                qrCode = "https://pay.qinghe.com/pay?orderId=" + paymentId + "&amount=" + amount;
        }
        // 模拟生成一个唯一的二维码ID
        return qrCode + "&uuid=" + UUID.randomUUID().toString();
    }
}