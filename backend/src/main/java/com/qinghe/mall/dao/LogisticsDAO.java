package com.qinghe.mall.dao;

import com.qinghe.mall.dataobject.LogisticsDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LogisticsDAO {

    int insert(LogisticsDO logisticsDO);

    /** 按订单号查询物流记录（一单一记录，不存在返回 null） */
    LogisticsDO findByOrderNumber(@Param("orderNumber") String orderNumber);

    /**
     * CAS 更新物流状态：仅当前状态等于 expectedStatus 时更新为 targetStatus。
     * 返回受影响行数（0 表示状态已变化/并发竞争失败）。
     */
    int updateStatusWithGuard(@Param("orderNumber") String orderNumber,
                              @Param("expectedStatus") String expectedStatus,
                              @Param("targetStatus") String targetStatus);

    /** 仅测试数据清理使用 */
    int deleteByOrderNumberForTest(@Param("orderNumber") String orderNumber);
}
