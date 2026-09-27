package com.qinghe.mall.dao;

import com.qinghe.mall.dataobject.OrderDO;
import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrderDAO {

    int insert(OrderDO orderDO);

    OrderDO findByOrderNumber(@Param("orderNumber") String orderNumber);

    List<OrderDO> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);

    /** 商家店铺订单（M6：按 merchant_id 归属过滤，status 可空） */
    List<OrderDO> findByMerchantId(@Param("merchantId") Long merchantId, @Param("status") String status);

    /** 商家店铺订单总数（M6 统计） */
    long countByMerchantId(@Param("merchantId") Long merchantId);

    /** 商家某时刻后新订单数（M6 今日统计） */
    long countByMerchantIdAndCreatedAfter(@Param("merchantId") Long merchantId, @Param("after") java.util.Date after);

    /** 商家指定状态订单金额合计（M6 统计，SUM） */
    java.math.BigDecimal sumTotalPriceByMerchantAndStatus(@Param("merchantId") Long merchantId,
                                                          @Param("status") String status);

    /** 商家指定状态某时刻后订单金额合计（M6 今日统计，SUM） */
    java.math.BigDecimal sumTotalPriceByMerchantAndStatusAndCreatedAfter(
            @Param("merchantId") Long merchantId, @Param("status") String status, @Param("after") java.util.Date after);

    int updateStatus(@Param("orderNumber") String orderNumber, @Param("status") String status);

    /**
     * 仅当订单处于 WAIT_BUYER_PAY 时更新为指定状态，返回受影响行数。
     * 用于取消订单/超时关单：避免将已支付订单误置为关闭（并发安全）。
     */
    int updateStatusIfWaitPay(@Param("orderNumber") String orderNumber, @Param("targetStatus") String targetStatus);

    /**
     * 仅当订单处于 expectedStatus 时更新为 targetStatus，返回受影响行数（CAS 语义，并发安全）。
     * 用于发货/确认收货/退款等状态流转：WHERE status = expected 保证不会误改其他状态订单。
     */
    int updateStatusWithGuard(@Param("orderNumber") String orderNumber,
                              @Param("expectedStatus") String expectedStatus,
                              @Param("targetStatus") String targetStatus);

    /**
     * 多来源 CAS：仅当订单当前状态属于 expectedStatuses 之一时更新为 targetStatus，
     * 返回受影响行数。P2-18 退货退款申请可从 已付款/已发货/已完成 多状态发起。
     */
    int updateStatusWithGuardIn(@Param("orderNumber") String orderNumber,
                                @Param("expectedStatuses") java.util.List<String> expectedStatuses,
                                @Param("targetStatus") String targetStatus);

    /**
     * 查询超时未支付的待付款订单（供定时任务关单）。
     */
    List<OrderDO> findExpiredWaitPay(@Param("expireTime") Date expireTime);

    List<OrderDO> findAll();

    /** P1-11：管理端订单分页查询（status 可空=全量，PageHelper 分页） */
    List<OrderDO> queryAdminPage(@Param("status") String status);

    /** 订单总数（看板聚合） */
    long countAll();

    /** 指定状态订单金额合计（看板聚合，SUM） */
    java.math.BigDecimal sumTotalPriceByStatus(@Param("status") String status);

    /** P1-7：多状态订单金额合计（统一已付营收口径，SUM ... WHERE status IN (...)） */
    java.math.BigDecimal sumTotalPriceByStatuses(@Param("statuses") java.util.List<String> statuses);

    /** P1-7：多状态销售日报（近 days 天按天聚合，status IN (...)） */
    java.util.List<java.util.Map<String, Object>> dailySalesReportByStatuses(
            @Param("days") int days, @Param("statuses") java.util.List<String> statuses);

    /** 按订单号删除（仅测试数据清理使用） */
    int deleteByOrderNumberForTest(@Param("orderNumber") String orderNumber);

    /**
     * 销售日报（P3 报表）：近 days 天内按天聚合指定状态订单的销售额与订单数。
     * 返回 Map：day(yyyy-MM-dd) / orderCount / salesAmount。
     */
    java.util.List<java.util.Map<String, Object>> dailySalesReport(@Param("days") int days, @Param("status") String status);

    /**
     * P2：商家已付口径按状态分组聚合（金额 + 单数），一次查询替代循环 N 次。
     * after 非空时限定 gmt_created >= after（今日统计）。
     * 返回 Map：status / orderCount / amount。
     */
    java.util.List<java.util.Map<String, Object>> sumByMerchantAndStatuses(
            @Param("merchantId") Long merchantId,
            @Param("statuses") java.util.List<String> statuses,
            @Param("after") java.util.Date after);

    /**
     * 购物车级优惠券（v1.8）释放守卫：统计同用户仍持有该券、且订单未到终态的其它订单数。
     * 仅当计数为 0（本单是最后一张持券在途单）时方可释放券，防止部分退款/取消导致整券误还。
     */
    int countActiveByCouponExcluding(@Param("couponId") String couponId,
                                     @Param("userId") Long userId,
                                     @Param("excludeOrderNumber") String excludeOrderNumber);


/**
 * 个性化推荐（v1.10）：用户历史购买的品牌偏好分布（排除取消/超时关闭单）。
 * 返回 [{brand, orderCount}]，按购买次数降序，取前 limit 个品牌。
 */
List<java.util.Map<String, Object>> brandAffinity(@Param("userId") Long userId,
                                                  @Param("limit") int limit);
}
