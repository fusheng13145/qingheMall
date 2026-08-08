package com.qinghe.mall.dao;

import com.qinghe.mall.dataobject.SeckillOrderDO;
import org.apache.ibatis.annotations.Param;

/**
 * 秒杀订单 DAO（M5-A3）。
 */
public interface SeckillOrderDAO {

    int insert(SeckillOrderDO seckillOrder);

    SeckillOrderDO findByOrderNumber(@Param("orderNumber") String orderNumber);

    SeckillOrderDO findByUserAndActivity(@Param("userId") Long userId, @Param("activityId") String activityId);

    /**
     * 取消/超时回滚：仅当状态为 CREATED 时置 CANCELLED，返回受影响行数（CAS 守卫）。
     */
    int updateStatus(@Param("orderNumber") String orderNumber, @Param("status") String status);

    /** 仅测试清理使用 */
    int deleteByActivityIdForTest(@Param("activityId") String activityId);
}
