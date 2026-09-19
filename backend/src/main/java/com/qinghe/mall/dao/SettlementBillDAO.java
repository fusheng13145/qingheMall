package com.qinghe.mall.dao;

import com.qinghe.mall.dataobject.SettlementBillDO;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SettlementBillDAO {

    int insert(SettlementBillDO bill);

    SettlementBillDO findById(@Param("id") String id);

    /** 商家的结算单（分页由 PageHelper 包装） */
    List<SettlementBillDO> findByMerchant(@Param("merchantId") Long merchantId);

    /** 管理端按状态查（status 为空查全部，分页由 PageHelper 包装） */
    List<SettlementBillDO> findPage(@Param("status") String status);

    /** 商家已放款结算单合计 */
    BigDecimal sumPaidByMerchant(@Param("merchantId") Long merchantId);

    /** CAS 审核：仅 PENDING 可流转 */
    int updateReview(@Param("id") String id, @Param("status") String status,
                     @Param("reviewNote") String reviewNote, @Param("reviewerId") Long reviewerId);
}
