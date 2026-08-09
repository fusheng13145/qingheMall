package com.qinghe.mall.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.qinghe.mall.dao.ProductDAO;
import com.qinghe.mall.model.ChannelPayResult;
import com.qinghe.mall.model.Order;
import com.qinghe.mall.model.OrderStatus;
import com.qinghe.mall.model.PaymentRecord;
import com.qinghe.mall.model.PaymentStatus;
import com.qinghe.mall.model.PayType;
import com.qinghe.mall.model.ProductDetail;
import com.qinghe.mall.model.Result;
import com.qinghe.mall.service.AlipayClient;
import com.qinghe.mall.service.OrderService;
import com.qinghe.mall.service.PayChannel;
import com.qinghe.mall.service.PayService;
import com.qinghe.mall.service.PaymentRecordService;
import com.qinghe.mall.util.UUIDUtils;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class PayServiceImpl implements PayService {

    private static final Logger log = LoggerFactory.getLogger(PayServiceImpl.class);

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentRecordService paymentRecordService;

    @Autowired
    private ProductDAO productDAO;

    @Autowired
    private List<PayChannel> payChannels;

    @Autowired
    private WechatNativePayChannel wechatChannel;

    @Autowired
    private AlipayPayChannel alipayChannel;

    @Autowired
    private AlipayClient alipayClient;

    /**
     * 事务模板：支付成功落库（订单状态 + 支付流水 + 销量）必须同一事务。
     * 不使用 @Transactional 自调用（Spring 代理不生效），显式模板包裹（P0-1 修复）。
     */
    @Autowired
    private TransactionTemplate transactionTemplate;

    /**
     * 模拟支付开关（P1 修复）：生产 profile 置 false，防止绕过真实支付。
     * 默认 true（dev 演示可用）；生产通过 PAY_MOCK_ENABLED=false 关闭。
     */
    @Value("${app.pay.mock-enabled:true}")
    private boolean mockEnabled;

    @Override
    @Transactional
    public Result<ChannelPayResult> createPay(Long userId, String orderNumber, String payType) {
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

        // 选择渠道：请求微信且已配置 → 真实渠道；否则回退模拟
        PayChannel channel = resolveChannel(payType);
        // 生产环境禁模拟：真实渠道未配置时直接拒绝，禁止绕过支付
        if ("MOCK".equals(channel.code()) && !mockEnabled) {
            return Result.fail("未配置真实支付渠道（生产环境模拟支付已禁用）");
        }
        ChannelPayResult payResult = channel.createPay(order);

        // 记录支付流水（PAYING）
        PaymentRecord record = new PaymentRecord();
        record.setId(UUIDUtils.uuid());
        record.setUserId(userId);
        record.setOrderNumber(orderNumber);
        record.setAmount(order.getTotalPrice());
        record.setChannelPaymentId("");
        record.setPayType(parsePayType(payType));
        record.setPayStatus(PaymentStatus.PAYING);
        record.setChannelType(channel.code());
        record.setGmtCreated(new Date());
        record.setGmtModified(new Date());
        paymentRecordService.insert(record);

        return Result.success(payResult);
    }

    @Override
    @Transactional
    public Result<String> mockPay(Long userId, String orderNumber) {
        if (!mockEnabled) {
            return Result.fail("模拟支付已禁用（生产环境请配置真实支付渠道）");
        }
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
        // 幂等：仅待付款可置成功
        if (order.getStatus() != OrderStatus.WAIT_BUYER_PAY) {
            return Result.fail("订单状态异常，无法支付");
        }

        boolean updated = orderService.updateStatusIfWaitPay(orderNumber, OrderStatus.TRADE_PAID_SUCCESS.name());
        if (!updated) {
            return Result.fail("订单状态已变化，请刷新后重试");
        }
        String channelPaymentId = "SIM_" + System.currentTimeMillis();
        paymentRecordService.updatePayStatus(orderNumber, PaymentStatus.SUCCESS.name(), channelPaymentId);
        // 支付成功：累加销量（幂等：仅本方法调用前订单为待付款时执行）
        increasePurchaseNum(orderService.findByOrderNumber(orderNumber));
        return Result.success("支付成功");
    }

    @Override
    public Result<String> queryPayStatus(Long userId, String orderNumber) {
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
        // 已成功直接返回；仍待付款时按已配置渠道主动查询一次渠道状态
        if (order.getStatus() == OrderStatus.TRADE_PAID_SUCCESS) {
            return Result.success(order.getStatus().name());
        }
        if (order.getStatus() == OrderStatus.WAIT_BUYER_PAY) {
            if (alipayChannel.enabled()) {
                String tradeStatus = alipayClient.queryTradeStatus(orderNumber);
                if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                    markPaid(orderNumber, "ALI_QUERY_" + System.currentTimeMillis());
                    return Result.success(OrderStatus.TRADE_PAID_SUCCESS.name());
                }
            }
            if (wechatChannel.enabled()) {
                String tradeState = wechatChannel.queryTradeState(orderNumber);
                if ("SUCCESS".equals(tradeState)) {
                    markPaid(orderNumber, "WX_QUERY_" + System.currentTimeMillis());
                    return Result.success(OrderStatus.TRADE_PAID_SUCCESS.name());
                }
            }
        }
        return Result.success(order.getStatus().name());
    }

    @Override
    public Result<String> handleAlipayNotify(Map<String, String> params) {
        try {
            // 验签失败拒绝
            if (!alipayClient.verifyNotify(params)) {
                log.warn("支付宝回调验签失败");
                return Result.fail("签名验证失败");
            }
            String outTradeNo = params.get("out_trade_no");
            String tradeStatus = params.get("trade_status");
            String tradeNo = params.get("trade_no");
            if (StringUtils.isBlank(outTradeNo)) {
                return Result.fail("回调缺少订单号");
            }
            // 仅成功状态落库（TRADE_SUCCESS / TRADE_FINISHED），其余忽略
            if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
                log.info("支付宝回调非成功状态 orderNumber={}, tradeStatus={}", outTradeNo, tradeStatus);
                return Result.success("已忽略");
            }
            // 金额核对（P2-2）：回调金额须与订单实付一致，防金额篡改。
            // P0-2 修复：金额字段为必选项，缺失直接拒绝（原实现缺失时跳过核对，存在旁路）
            String totalAmount = params.get("total_amount");
            if (StringUtils.isBlank(totalAmount)) {
                log.warn("支付宝回调缺少金额字段 orderNumber={}", outTradeNo);
                return Result.fail("回调缺少金额字段");
            }
            Order order = orderService.findByOrderNumber(outTradeNo);
            if (order == null) {
                return Result.fail("订单不存在");
            }
            if (new BigDecimal(totalAmount).compareTo(order.getTotalPrice()) != 0) {
                log.warn("支付宝回调金额不符 orderNumber={}, notify={}, expected={}",
                        outTradeNo, totalAmount, order.getTotalPrice());
                return Result.fail("回调金额不一致");
            }
            markPaid(outTradeNo, StringUtils.defaultIfBlank(tradeNo, "ALI_" + System.currentTimeMillis()));
            return Result.success("处理成功");
        } catch (Exception e) {
            log.error("支付宝回调处理异常", e);
            return Result.fail("回调处理失败");
        }
    }

    @Override
    public Result<String> handleWechatNotify(String serial, String timestamp, String nonce, String signature, String body) {
        try {
            JSONObject biz = wechatChannel.parseNotify(serial, timestamp, nonce, signature, body);
            String outTradeNo = biz.getString("out_trade_no");
            String transactionId = biz.getString("transaction_id");
            String tradeState = biz.getString("trade_state");
            if (StringUtils.isBlank(outTradeNo)) {
                return Result.fail("回调缺少订单号");
            }
            if (!"SUCCESS".equals(tradeState)) {
                log.info("微信回调非成功状态 orderNumber={}, tradeState={}", outTradeNo, tradeState);
                return Result.success("已忽略");
            }
            // 金额核对（P2-2，微信单位分）：amount.total 须与订单实付×100 一致。
            // P0-2 修复：金额字段为必选项，缺失直接拒绝（原实现缺失时跳过核对，存在旁路）
            JSONObject amountObj = biz.getJSONObject("amount");
            Long totalFen = amountObj == null ? null : amountObj.getLong("total");
            if (totalFen == null) {
                log.warn("微信回调缺少金额字段 orderNumber={}", outTradeNo);
                return Result.fail("回调缺少金额字段");
            }
            Order order = orderService.findByOrderNumber(outTradeNo);
            if (order == null) {
                return Result.fail("订单不存在");
            }
            BigDecimal expectedFen = order.getTotalPrice().multiply(new BigDecimal(100));
            if (BigDecimal.valueOf(totalFen).compareTo(expectedFen) != 0) {
                log.warn("微信回调金额不符 orderNumber={}, notify={}, expected={}",
                        outTradeNo, totalFen, expectedFen);
                return Result.fail("回调金额不一致");
            }
            markPaid(outTradeNo, StringUtils.defaultIfBlank(transactionId, "WX_" + System.currentTimeMillis()));
            return Result.success("处理成功");
        } catch (IllegalArgumentException e) {
            log.warn("微信回调校验失败: {}", e.getMessage());
            return Result.fail("回调校验失败");
        } catch (Exception e) {
            log.error("微信回调处理异常", e);
            return Result.fail("回调处理失败");
        }
    }

    /**
     * 支付成功落库（幂等）：仅当订单处于待付款时置成功并累加销量。
     * P0-1 修复：原 @Transactional 因同类自调用导致 Spring 代理事务不生效，
     * 现改用 TransactionTemplate 显式包裹，保证「订单置已支付 + 支付流水更新 + 销量累加」
     * 三笔写操作处于同一事务边界，任一步失败整体回滚。
     */
    protected void markPaid(String orderNumber, String channelPaymentId) {
        transactionTemplate.execute(status -> {
            boolean updated = orderService.updateStatusIfWaitPay(orderNumber, OrderStatus.TRADE_PAID_SUCCESS.name());
            if (updated) {
                paymentRecordService.updatePayStatus(orderNumber, PaymentStatus.SUCCESS.name(), channelPaymentId);
                Order order = orderService.findByOrderNumber(orderNumber);
                increasePurchaseNum(order);
                log.info("订单支付成功 orderNumber={}, channelPaymentId={}", orderNumber, channelPaymentId);
            } else {
                log.info("订单已处理过，跳过幂等更新 orderNumber={}", orderNumber);
            }
            return null;
        });
    }

    /**
     * 按请求渠道解析通道：支付宝/微信已配置则走真实渠道，否则回退模拟。
     */
    private PayChannel resolveChannel(String payType) {
        if ("ALIPAY".equalsIgnoreCase(payType) && alipayChannel.enabled()) {
            return alipayChannel;
        }
        if ("WECHAT".equalsIgnoreCase(payType) && wechatChannel.enabled()) {
            return wechatChannel;
        }
        for (PayChannel channel : payChannels) {
            if ("MOCK".equals(channel.code())) {
                return channel;
            }
        }
        throw new IllegalStateException("无可用支付通道");
    }

    private PayType parsePayType(String payType) {
        if (StringUtils.isNotBlank(payType)) {
            try {
                return PayType.valueOf(payType.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // 非法渠道按默认处理
            }
        }
        return PayType.ALIPAY;
    }

    /**
     * 按订单的商品规格累加销量（购买数量）。
     */
    private void increasePurchaseNum(Order order) {
        if (order == null) {
            return;
        }
        ProductDetail productDetail = order.getProductDetail();
        if (productDetail == null || StringUtils.isBlank(productDetail.getProductId())) {
            return;
        }
        int quantity = order.getQuantity() != null && order.getQuantity() > 0 ? order.getQuantity() : 1;
        productDAO.increasePurchaseNum(productDetail.getProductId(), quantity);
    }
}
