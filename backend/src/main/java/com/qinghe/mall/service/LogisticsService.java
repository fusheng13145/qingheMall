package com.qinghe.mall.service;

import com.qinghe.mall.model.Logistics;

/**
 * 物流服务（P2-18）：发货建档 + 轨迹推进 + 轨迹查询。
 *
 * 状态机：SHIPPED → IN_TRANSIT → DELIVERING → SIGNED（终态）。
 * 本系统未对接真实承运商接口，轨迹推进由商家/管理员手动触发（确定性管道），
 * 每次推进追加一条轨迹事件，全程留痕可追溯。
 */
public interface LogisticsService {

    /**
     * 发货并建立物流档案：订单 CAS 已付款→已发货 + 写物流记录 + 首条轨迹，同一事务原子提交。
     *
     * @param orderNumber    订单号
     * @param company        承运商（必填）
     * @param trackingNumber 运单号（必填）
     * @return 发货后的物流视图（含轨迹）
     */
    Logistics ship(String orderNumber, String company, String trackingNumber);

    /**
     * 沿固定管道推进物流状态：SHIPPED→IN_TRANSIT→DELIVERING→SIGNED，每次追加一条轨迹。
     * 终态 SIGNED 再推进将抛出业务异常。
     */
    Logistics advance(String orderNumber);

    /**
     * 查询物流信息与轨迹（时间正序）。订单无物流档案时返回 null。
     */
    Logistics track(String orderNumber);
}
