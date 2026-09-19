package com.qinghe.mall.service;

import com.qinghe.mall.dataobject.OrderDO;
import com.qinghe.mall.dataobject.SettlementBillDO;
import com.qinghe.mall.dataobject.SettlementLedgerDO;
import com.qinghe.mall.model.Paging;
import java.math.BigDecimal;
import java.util.Map;

/**
 * 商家结算分账（A2，v1.5）。
 *
 * 记账规则：
 * - 确认收货（COMPLETED）触发 EARN 分账：net = 实付 × (1 - 佣金费率)；
 * - 退款审核通过触发 REVERSAL 冲销：仅当该订单已存在 EARN 时入账（负 net）；
 * - 结算单由管理端按商家生成，仅汇总未结算的 EARN；审核放款 PAID / 驳回 REJECTED 并退回流水。
 *
 * 对账恒等式：Σ(流水 net) == Σ(PAID 结算单 total_net) + 未入单流水净额（含冲销抵扣）。
 */
public interface SettlementService {

    /** 确认收货分账（幂等：同单重复确认/重复调用不重复入账；平台自营订单 merchant_id 为空不分账） */
    void recordEarning(OrderDO order);

    /** 退款冲销（幂等；订单无 EARN 记录时跳过——确认收货前的退款无需冲销） */
    void recordReversal(OrderDO order);

    /** 商家结算概览：余额 / 累计货款 / 累计佣金 / 待审核结算单合计 / 已放款合计 */
    Map<String, Object> merchantSummary(Long merchantId);

    /** 商家流水分页 */
    Paging<SettlementLedgerDO> merchantLedger(Long merchantId, int pageNum, int pageSize);

    /** 商家结算单分页 */
    Paging<SettlementBillDO> merchantBills(Long merchantId, int pageNum, int pageSize);

    /** 管理端：为商家生成结算单（汇总全部未结算 EARN 流水） */
    SettlementBillDO generateBill(Long merchantId, Long operatorId);

    /** 管理端：审核结算单（approve=true 放款 PAID；false 驳回 REJECTED 并退回流水） */
    void reviewBill(String billId, boolean approve, String reviewNote, Long operatorId);

    /** 管理端：结算单分页（可按状态过滤） */
    Paging<SettlementBillDO> adminBills(String status, int pageNum, int pageSize);

    /** 管理端：结算单包含的流水明细 */
    java.util.List<SettlementLedgerDO> billEntries(String billId);
}
