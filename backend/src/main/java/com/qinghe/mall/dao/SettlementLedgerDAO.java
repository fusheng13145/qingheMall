package com.qinghe.mall.dao;

import com.qinghe.mall.dataobject.SettlementLedgerDO;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SettlementLedgerDAO {

    int insert(SettlementLedgerDO ledger);

    /** 按订单号 + 类型查（幂等守卫：同单同类型至多一条） */
    SettlementLedgerDO findByOrderNumberAndType(@Param("orderNumber") String orderNumber, @Param("type") String type);

    /** 商家未结算的 EARN 流水（生成结算单候选项） */
    List<SettlementLedgerDO> findUnbilledEarn(@Param("merchantId") Long merchantId);

    /** 商家全部流水（分页由 PageHelper 包装） */
    List<SettlementLedgerDO> findByMerchant(@Param("merchantId") Long merchantId);

    /** 商家累计净入（含冲销负流水） */
    BigDecimal sumNetByMerchant(@Param("merchantId") Long merchantId);

    /** 商家累计佣金 */
    BigDecimal sumCommissionByMerchant(@Param("merchantId") Long merchantId);

    /** 批量标记入结算单（驳回时传 billId=null 退回） */
    int markBilled(@Param("billId") String billId, @Param("ids") List<String> ids);

    /** 结算单包含的流水 */
    List<SettlementLedgerDO> findByBillId(@Param("billId") String billId);
}
