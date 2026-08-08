package com.qinghe.mall.service;

import com.qinghe.mall.model.ChannelPayResult;
import com.qinghe.mall.model.Order;

/**
 * 支付渠道抽象（M2-9 真实支付）。
 *
 * 每个渠道实现创建支付的能力；渠道未配置时由 MockPayChannel 回退，
 * 前端展示「模拟支付」按钮，不影响业务流程闭环。
 */
public interface PayChannel {

    /** 渠道编码：WECHAT / ALIPAY / MOCK */
    String code();

    /** 该渠道是否已配置齐全（凭证齐备才启用真实支付） */
    boolean enabled();

    /** 创建支付：真实渠道返回二维码/跳转参数，模拟渠道返回 mock=true */
    ChannelPayResult createPay(Order order);
}
