package com.qinghe.mall.service.impl;

import com.qinghe.mall.model.ChannelPayResult;
import com.qinghe.mall.model.Order;
import com.qinghe.mall.service.PayChannel;
import org.springframework.stereotype.Component;

/**
 * 模拟支付渠道：始终可用（学习/演示环境回退），
 * 不产生真实资金流，点击后由前端调用 mockPay 直接置成功。
 */
@Component
public class MockPayChannel implements PayChannel {

    @Override
    public String code() {
        return "MOCK";
    }

    @Override
    public boolean enabled() {
        return true;
    }

    @Override
    public ChannelPayResult createPay(Order order) {
        return ChannelPayResult.mock("MOCK", order.getOrderNumber());
    }
}
