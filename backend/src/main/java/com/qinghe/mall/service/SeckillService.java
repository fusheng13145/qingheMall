package com.qinghe.mall.service;

import com.qinghe.mall.dataobject.SeckillActivityDO;
import com.qinghe.mall.model.Paging;
import java.util.List;

/**
 * 秒杀服务（M5-A3）。
 *
 * 设计要点：
 * - 防超卖三重保障：活动库存 CAS 预扣（主防线）+ 商品 SKU 二次 CAS + uk_user_activity 唯一约束兜底。
 * - 秒杀生成的仍是普通 order（便于复用支付/退款/售后），seckill_order 仅作去重与库存对账。
 * - 金额口径：秒杀订单 totalPrice 直接取「秒杀价 × 数量」，支付链路无需改动。
 * - 超时/取消回滚：复用 OrderTimeoutQueue；closeAndRestoreStock 在回滚商品库存后调用 {@link #rollbackIfUnpaid} 恢复活动库存。
 */
public interface SeckillService {

    /** 后台创建活动（校验 SKU 存在、起止时间合法、库存>0） */
    SeckillActivityDO createActivity(SeckillActivityDO activity);

    /** 管理端分页列表（status 为空查全部） */
    Paging<SeckillActivityDO> listActivities(String status, int pageNum, int pageSize);

    /** 用户端：进行中且在时间窗内的活动列表（含剩余库存与起止时间，前端据此倒计时） */
    List<SeckillActivityDO> listOngoing();

    /** 活动详情 */
    SeckillActivityDO getActivity(String id);

    /** 开启/关闭活动（status: ONGOING / CLOSED） */
    void toggle(String activityId, String status);

    /**
     * 秒杀下单（核心，含 @RateLimit 限流在 Controller 层）。
     *
     * @return 生成的普通订单号（前端跳转收银台 /pay 完成支付）
     */
    String createOrder(String activityId, Long userId, int quantity,
                        String receiverName, String receiverPhone, String receiverAddress);

    /**
     * 超时未付或用户取消时回滚：仅当 seckill_order 为 CREATED 才恢复活动库存并置 CANCELLED。
     * 由 OrderServiceImpl.closeAndRestoreStock 在已开启的事务内调用。
     */
    void rollbackIfUnpaid(String orderNumber);
}
