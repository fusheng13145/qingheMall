package com.qinghe.mall.dao;

import com.qinghe.mall.dataobject.UserCouponDO;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface UserCouponDAO {

    int insert(UserCouponDO userCoupon);

    UserCouponDO findById(@Param("id") String id);

    /** 用户持有券（JOIN coupon 展示字段），可按状态过滤 */
    List<UserCouponDO> findByUserIdAndStatus(@Param("userId") Long userId,
                                             @Param("status") String status);

    /** 结算可用券：未使用 + 券上架 + 在时间窗内 + 门槛 ≤ amount（金额比较在 Service 层二次校验） */
    List<UserCouponDO> findUsable(@Param("userId") Long userId,
                                  @Param("amount") java.math.BigDecimal amount);

    /** 核销锁定：仅 UNUSED 且归属当前用户时置为 USED；返回影响行数（CAS 守卫） */
    int lock(@Param("id") String id,
             @Param("userId") Long userId,
             @Param("orderNumber") String orderNumber);

    /** 释放：仅 USED 时复位为 UNUSED 并清空订单绑定（取消/退款拒绝时调用） */
    int release(@Param("id") String id);

    /** P2：统计用户已领取某券数量（perLimit 限领校验） */
    int countByUserAndCoupon(@Param("userId") Long userId, @Param("couponId") String couponId);
}
