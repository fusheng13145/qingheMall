package com.qinghe.mall.service.impl;

import com.qinghe.mall.dao.PaymentRecordDAO;
import com.qinghe.mall.dataobject.PaymentRecordDO;
import com.qinghe.mall.model.Order;
import com.qinghe.mall.model.OrderStatus;
import com.qinghe.mall.model.PaymentRecord;
import com.qinghe.mall.model.PaymentStatus;
import com.qinghe.mall.model.PayType;
import com.qinghe.mall.model.Result;
import com.qinghe.mall.param.PaymentParam;
import com.qinghe.mall.service.OrderService;
import com.qinghe.mall.service.PayService;
import com.qinghe.mall.service.PaymentRecordService;
import com.qinghe.mall.util.UUIDUtils;
import java.util.Date;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PayServiceImpl implements PayService {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentRecordService paymentRecordService;

    @Override
    @Transactional
    public Result<String> pay(Long userId, PaymentParam paymentParam) {
        String orderNumber = paymentParam.getOrderNumber();
        String payType = paymentParam.getPayType();

        if (StringUtils.isBlank(orderNumber)) {
            return Result.fail("订单号不能为空");
        }

        Order order = orderService.findByOrderNumber(orderNumber);
        if (order == null) {
            return Result.fail("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            return Result.fail("无权操作此订单");
        }
        if (order.getStatus() != OrderStatus.WAIT_BUYER_PAY) {
            return Result.fail("订单状态异常，无法支付");
        }

        // 创建支付记录
        PaymentRecord record = new PaymentRecord();
        record.setId(UUIDUtils.uuid());
        record.setUserId(userId);
        record.setOrderNumber(orderNumber);
        record.setAmount(order.getTotalPrice());
        if (StringUtils.isNotBlank(payType)) {
            record.setPayType(PayType.valueOf(payType));
        } else {
            record.setPayType(PayType.ALIPAY);
        }
        record.setPayStatus(PaymentStatus.PAYING);
        record.setChannelType(record.getPayType().name());
        record.setGmtCreated(new Date());
        record.setGmtModified(new Date());
        paymentRecordService.insert(record);

        // 模拟支付：直接更新订单状态为支付成功
        orderService.updateOrderStatus(orderNumber, OrderStatus.TRADE_PAID_SUCCESS.name());
        // 更新支付记录状态
        String channelPaymentId = "SIM_" + System.currentTimeMillis();
        paymentRecordService.updatePayStatus(orderNumber, PaymentStatus.SUCCESS.name(), channelPaymentId);

        return Result.success("支付成功");
    }

    @Override
    @Transactional
    public Result<String> callback(String orderNumber) {
        if (StringUtils.isBlank(orderNumber)) {
            return Result.fail("订单号不能为空");
        }

        Order order = orderService.findByOrderNumber(orderNumber);
        if (order == null) {
            return Result.fail("订单不存在");
        }

        // 模拟回调：更新订单状态
        orderService.updateOrderStatus(orderNumber, OrderStatus.TRADE_PAID_SUCCESS.name());
        String channelPaymentId = "SIM_CB_" + System.currentTimeMillis();
        paymentRecordService.updatePayStatus(orderNumber, PaymentStatus.SUCCESS.name(), channelPaymentId);

        return Result.success("回调处理成功");
    }
}
