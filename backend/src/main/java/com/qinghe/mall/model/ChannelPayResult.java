package com.qinghe.mall.model;

/**
 * 渠道下单结果。
 *
 * @param mock    true 表示当前为模拟支付通道（前端展示「模拟支付」按钮）；
 *                false 表示真实渠道（codeUrl 为微信 Native 二维码链接）
 */
public class ChannelPayResult {

    private String channel;
    private Boolean mock;
    private String codeUrl;
    private String orderNumber;

    public static ChannelPayResult mock(String channel, String orderNumber) {
        ChannelPayResult r = new ChannelPayResult();
        r.channel = channel;
        r.mock = true;
        r.orderNumber = orderNumber;
        return r;
    }

    public static ChannelPayResult real(String channel, String codeUrl, String orderNumber) {
        ChannelPayResult r = new ChannelPayResult();
        r.channel = channel;
        r.mock = false;
        r.codeUrl = codeUrl;
        r.orderNumber = orderNumber;
        return r;
    }

    public String getChannel() {
        return channel;
    }

    public Boolean getMock() {
        return mock;
    }

    public String getCodeUrl() {
        return codeUrl;
    }

    public String getOrderNumber() {
        return orderNumber;
    }
}
