package com.qinghe.mall.dao;

import com.qinghe.mall.dataobject.RefundRequestDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RefundRequestDAO {

    int insert(RefundRequestDO requestDO);

    RefundRequestDO findById(@Param("id") String id);

    /** 订单当前待审核的申请（同一订单至多一条 PENDING，防重复申请守卫） */
    RefundRequestDO findPendingByOrderNumber(@Param("orderNumber") String orderNumber);

    /** 订单维度的申请历史（审核留痕展示） */
    List<RefundRequestDO> findByOrderNumber(@Param("orderNumber") String orderNumber);

    /** 用户的申请列表（按创建时间倒序） */
    List<RefundRequestDO> findByUserId(@Param("userId") Long userId);

    /**
     * CAS 审核落库：仅 PENDING 的申请可流转为 APPROVED/REJECTED。
     * 返回受影响行数（0 表示已被审核/并发竞争失败）。
     */
    int reviewWithGuard(@Param("id") String id,
                        @Param("targetStatus") String targetStatus,
                        @Param("reviewComment") String reviewComment);

    /** 仅测试数据清理使用 */
    int deleteByOrderNumberForTest(@Param("orderNumber") String orderNumber);
}
