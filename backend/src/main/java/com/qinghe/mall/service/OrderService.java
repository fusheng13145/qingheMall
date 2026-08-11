package com.qinghe.mall.service;

import com.qinghe.mall.model.Order;
import com.qinghe.mall.model.Paging;
import java.util.List;

public interface OrderService {

    /** 创建单笔订单（含扣库存；quantity 默认 1） */
    Order createOrder(Order order);

    /**
     * 创建单笔订单并核销优惠券：仅当 userCouponId 非空时生效。
     * 会在事务内将 totalPrice 改写为实付金额（原价 - 优惠），并锁定用户券。
     * discountAmount 为前端已算好的优惠额（后端二次校验），无券时传 null。
     */
    Order createOrder(Order order, String userCouponId, java.math.BigDecimal discountAmount);

    Order findByOrderNumber(String orderNumber);

    List<Order> findByUserIdAndStatus(Long userId, String status);

    /**
     * 按用户与状态分页查询订单（订单中心使用，消除一次拉全量）。
     * pageNum < 1 时重置为 1；pageSize 1-50，越界重置为 10。
     */
    Paging<Order> findPageByUserIdAndStatus(Long userId, String status, Integer pageNum, Integer pageSize);

    boolean updateOrderStatus(String orderNumber, String status);

    List<Order> findAll();

    /** P1-11：管理端订单分页查询（status 可空=全量；消除全表捞取） */
    Paging<Order> findAdminPage(Integer pagination, Integer pageSize, String status);

    /**
     * 取消订单：仅待付款可取消，事务内回滚库存并置 TRADE_CLOSED。
     * 归属校验：订单必须是当前用户。
     */
    boolean cancelOrder(String orderNumber, Long userId);

    /**
     * 批量下单（购物车结算）：逐笔创建订单，返回成功创建的订单列表。
     * 任一失败即中断（已创建的订单保留，部分成功语义，由前端提示）。
     */
    List<Order> batchCreateOrders(List<Order> orders);

    /**
     * 查询超时未支付的待付款订单（供定时任务关单）。
     */
    List<Order> findExpiredWaitPay(int expireMinutes);

    /**
     * 关闭超时订单并回滚库存（定时任务使用，不校验用户归属）。
     */
    boolean closeExpiredOrder(String orderNumber);

    /** 订单总数（看板聚合） */
    long countAll();

    /** 指定状态订单金额合计（看板聚合） */
    java.math.BigDecimal sumTotalPriceByStatus(String status);

    /** P1-7：统一已付营收口径——多状态订单金额合计 */
    java.math.BigDecimal sumTotalPriceByStatuses(java.util.List<String> statuses);

    /**
     * 仅当订单处于 WAIT_BUYER_PAY 时更新为指定状态（支付成功幂等落库用）。
     * 返回 true 表示本次调用完成了状态流转，false 表示订单已非待付款。
     */
    boolean updateStatusIfWaitPay(String orderNumber, String targetStatus);

    /**
     * 销售日报（P3 报表）：近 days 天内按天聚合已支付订单的销售额与订单数。
     * 返回 List<Map>：{ day: yyyy-MM-dd, orderCount: long, salesAmount: BigDecimal }。
     */
    java.util.List<java.util.Map<String, Object>> dailySalesReport(int days);

    /**
     * 发货：仅 TRADE_PAID_SUCCESS → TRADE_SHIPPED（管理员操作，状态机守卫）。
     */
    boolean shipOrder(String orderNumber);

    /**
     * 确认收货：仅 TRADE_SHIPPED → TRADE_COMPLETED（归属用户操作）。
     */
    boolean confirmReceipt(String orderNumber, Long userId);

    /**
     * 申请退款：仅未发货的 TRADE_PAID_SUCCESS → TRADE_REFUNDING（归属用户操作）。
     * 已发货订单需走退货流程（本期未实现），故不允许从此状态申请退款。
     */
    boolean applyRefund(String orderNumber, Long userId);

    /**
     * 处理退款：TRADE_REFUNDING → TRADE_REFUNDED(approve) 或回退 TRADE_PAID_SUCCESS(reject)（管理员操作）。
     * 退款仅变更状态（与模拟支付一致），不对接真实网关退款。
     */
    boolean processRefund(String orderNumber, boolean approve);

    /**
     * 商家店铺订单分页（M6：按 merchant_id 归属过滤，status 可空）。
     */
    Paging<Order> listByMerchant(Long merchantId, String status, int pageNum, int pageSize);

    /**
     * 商家发货：校验订单归属本店后执行（PAID_SUCCESS → SHIPPED）。
     */
    boolean shipMerchantOrder(Long merchantId, String orderNumber);

    /**
     * 商家处理退款：校验订单归属本店后执行（REFUNDING → REFUNDED / 回退 PAID_SUCCESS）。
     */
    boolean processMerchantRefund(Long merchantId, String orderNumber, boolean approve);

    /**
     * 商家店铺统计（M6）：{ productCount, orderCount, paidRevenue, todayOrderCount, todayRevenue }。
     */
    java.util.Map<String, Object> merchantStats(Long merchantId);
}
