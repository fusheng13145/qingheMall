package com.qinghe.mall.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qinghe.mall.dao.SettlementBillDAO;
import com.qinghe.mall.dao.SettlementLedgerDAO;
import com.qinghe.mall.dataobject.OrderDO;
import com.qinghe.mall.dataobject.SettlementBillDO;
import com.qinghe.mall.dataobject.SettlementLedgerDO;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 商家结算分账单元测试（A2，v1.5）。
 *
 * 覆盖：EARN 分账（费率快照/幂等/自营跳过）、REVERSAL 冲销（无 EARN 跳过/幂等）、
 * 余额汇总、结算单生成（汇总+标记入单）、审核放款/驳回退回流水、对账恒等式。
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class SettlementServiceImplTest {

    @Mock
    private SettlementLedgerDAO ledgerDAO;

    @Mock
    private SettlementBillDAO billDAO;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private SettlementServiceImpl settlementService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(settlementService, "commissionRate", new BigDecimal("0.05"));
    }

    private OrderDO order(String orderNumber, Long merchantId, String price) {
        OrderDO o = new OrderDO();
        o.setOrderNumber(orderNumber);
        o.setMerchantId(merchantId);
        o.setTotalPrice(new BigDecimal(price));
        return o;
    }

    private SettlementLedgerDO earn(String orderNumber, Long merchantId, String gross, String commission) {
        SettlementLedgerDO e = new SettlementLedgerDO();
        e.setId("L-" + orderNumber);
        e.setOrderNumber(orderNumber);
        e.setMerchantId(merchantId);
        e.setType(SettlementLedgerDO.TYPE_EARN);
        e.setGross(new BigDecimal(gross));
        e.setCommissionRate(new BigDecimal("0.05"));
        e.setCommission(new BigDecimal(commission));
        e.setNet(new BigDecimal(gross).subtract(new BigDecimal(commission)));
        return e;
    }

    private void directTransaction() {
        when(transactionTemplate.execute(any(TransactionCallback.class))).thenAnswer(inv -> {
            TransactionCallback<?> cb = inv.getArgument(0);
            return cb.doInTransaction(null);
        });
    }

    // ============ recordEarning ============

    @Test
    @DisplayName("recordEarning 正常分账：commission=gross×5%，net=gross−commission")
    void recordEarning_computesNet() {
        when(ledgerDAO.findByOrderNumberAndType("QH1", "EARN")).thenReturn(null);

        settlementService.recordEarning(order("QH1", 5L, "100.00"));

        ArgumentCaptor<SettlementLedgerDO> captor = ArgumentCaptor.forClass(SettlementLedgerDO.class);
        verify(ledgerDAO).insert(captor.capture());
        SettlementLedgerDO ledger = captor.getValue();
        assertEquals(5L, ledger.getMerchantId());
        assertEquals(0, new BigDecimal("100.00").compareTo(ledger.getGross()));
        assertEquals(0, new BigDecimal("5.00").compareTo(ledger.getCommission()));
        assertEquals(0, new BigDecimal("95.00").compareTo(ledger.getNet()));
        assertEquals("EARN", ledger.getType());
        assertEquals(0, new BigDecimal("0.05").compareTo(ledger.getCommissionRate()));
    }

    @Test
    @DisplayName("recordEarning 幂等：同单已有 EARN 不重复入账")
    void recordEarning_idempotent() {
        when(ledgerDAO.findByOrderNumberAndType("QH1", "EARN"))
                .thenReturn(earn("QH1", 5L, "100.00", "5.00"));

        settlementService.recordEarning(order("QH1", 5L, "100.00"));

        verify(ledgerDAO, never()).insert(any(SettlementLedgerDO.class));
    }

    @Test
    @DisplayName("recordEarning 平台自营（merchant_id 为 NULL）与空订单跳过")
    void recordEarning_skipsPlatformOrder() {
        settlementService.recordEarning(order("QH2", null, "100.00"));
        settlementService.recordEarning(null);
        verify(ledgerDAO, never()).insert(any(SettlementLedgerDO.class));
    }

    // ============ recordReversal ============

    @Test
    @DisplayName("recordReversal 有 EARN 时冲销：net 为负、金额随原分账")
    void recordReversal_negatesNet() {
        when(ledgerDAO.findByOrderNumberAndType("QH1", "EARN"))
                .thenReturn(earn("QH1", 5L, "100.00", "5.00"));
        when(ledgerDAO.findByOrderNumberAndType("QH1", "REVERSAL")).thenReturn(null);

        settlementService.recordReversal(order("QH1", 5L, "100.00"));

        ArgumentCaptor<SettlementLedgerDO> captor = ArgumentCaptor.forClass(SettlementLedgerDO.class);
        verify(ledgerDAO).insert(captor.capture());
        SettlementLedgerDO reversal = captor.getValue();
        assertEquals("REVERSAL", reversal.getType());
        assertEquals(0, new BigDecimal("95.00").compareTo(reversal.getNet().abs()));
        assertTrue(reversal.getNet().signum() < 0);
        assertEquals(0, new BigDecimal("5.00").compareTo(reversal.getCommission()));
    }

    @Test
    @DisplayName("recordReversal 无 EARN（确认收货前退款）跳过；已有 REVERSAL 幂等")
    void recordReversal_skipsAndIdempotent() {
        when(ledgerDAO.findByOrderNumberAndType("QH1", "EARN")).thenReturn(null);
        settlementService.recordReversal(order("QH1", 5L, "100.00"));

        when(ledgerDAO.findByOrderNumberAndType("QH2", "EARN")).thenReturn(earn("QH2", 5L, "80.00", "4.00"));
        when(ledgerDAO.findByOrderNumberAndType("QH2", "REVERSAL"))
                .thenReturn(new SettlementLedgerDO());
        settlementService.recordReversal(order("QH2", 5L, "80.00"));

        verify(ledgerDAO, never()).insert(any(SettlementLedgerDO.class));
    }

    // ============ merchantSummary ============

    @Test
    @DisplayName("merchantSummary 余额 = Σ流水净额 − Σ已放款结算单")
    void merchantSummary_computesBalance() {
        when(ledgerDAO.sumNetByMerchant(5L)).thenReturn(new BigDecimal("190.00"));
        when(billDAO.sumPaidByMerchant(5L)).thenReturn(new BigDecimal("190.00"));
        when(ledgerDAO.sumCommissionByMerchant(5L)).thenReturn(new BigDecimal("10.00"));

        var summary = settlementService.merchantSummary(5L);

        assertEquals(0, new BigDecimal("0.00").compareTo((BigDecimal) summary.get("balance")));
        assertEquals(0, new BigDecimal("10.00").compareTo((BigDecimal) summary.get("totalCommission")));
        assertThrows(RuntimeException.class, () -> settlementService.merchantSummary(null));
    }

    // ============ generateBill ============

    @Test
    @DisplayName("generateBill 汇总未结算 EARN 并标记入单")
    void generateBill_sumsUnbilledEarn() {
        directTransaction();
        when(ledgerDAO.findUnbilledEarn(5L)).thenReturn(java.util.List.of(
                earn("QH1", 5L, "100.00", "5.00"),
                earn("QH2", 5L, "80.00", "4.00")));

        SettlementBillDO bill = settlementService.generateBill(5L, 1L);

        assertEquals(5L, bill.getMerchantId());
        assertEquals(0, new BigDecimal("180.00").compareTo(bill.getTotalGross()));
        assertEquals(0, new BigDecimal("9.00").compareTo(bill.getTotalCommission()));
        assertEquals(0, new BigDecimal("171.00").compareTo(bill.getTotalNet()));
        assertEquals(2, bill.getEntryCount());
        assertEquals("PENDING", bill.getStatus());
        verify(billDAO).insert(bill);
        verify(ledgerDAO).markBilled(eq(bill.getId()), anyList());

        assertThrows(RuntimeException.class, () -> settlementService.generateBill(null, 1L));
    }

    @Test
    @DisplayName("generateBill 无可结算流水拒绝")
    void generateBill_noEntries_throws() {
        directTransaction();
        when(ledgerDAO.findUnbilledEarn(5L)).thenReturn(java.util.List.of());
        assertThrows(RuntimeException.class, () -> settlementService.generateBill(5L, 1L));
    }

    // ============ reviewBill ============

    @Test
    @DisplayName("reviewBill 放款：PENDING→PAID")
    void reviewBill_approve_paid() {
        SettlementBillDO bill = new SettlementBillDO();
        bill.setId("B1");
        bill.setStatus("PENDING");
        when(billDAO.findById("B1")).thenReturn(bill);
        when(billDAO.updateReview(eq("B1"), eq("PAID"), any(), eq(1L))).thenReturn(1);

        settlementService.reviewBill("B1", true, "对账无误", 1L);

        verify(billDAO).updateReview(eq("B1"), eq("PAID"), eq("对账无误"), eq(1L));
        verify(ledgerDAO, never()).markBilled(any(), anyList());
    }

    @Test
    @DisplayName("reviewBill 驳回：REJECTED 并将流水退回未结算")
    void reviewBill_reject_releasesEntries() {
        SettlementBillDO bill = new SettlementBillDO();
        bill.setId("B1");
        bill.setStatus("PENDING");
        when(billDAO.findById("B1")).thenReturn(bill);
        when(billDAO.updateReview(eq("B1"), eq("REJECTED"), any(), eq(1L))).thenReturn(1);
        SettlementLedgerDO e1 = earn("QH1", 5L, "100.00", "5.00");
        e1.setBillId("B1");
        when(ledgerDAO.findByBillId("B1")).thenReturn(List.of(e1));

        settlementService.reviewBill("B1", false, "金额存疑", 1L);

        ArgumentCaptor<String> billIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(ledgerDAO).markBilled(billIdCaptor.capture(), anyList());
        assertNull(billIdCaptor.getValue()); // billId=null 退回未结算池
    }

    @Test
    @DisplayName("reviewBill 非法入参与状态守卫")
    void reviewBill_guards() {
        assertThrows(RuntimeException.class, () -> settlementService.reviewBill("", true, null, 1L));

        SettlementBillDO bill = new SettlementBillDO();
        bill.setId("B1");
        bill.setStatus("PENDING");
        when(billDAO.findById("B1")).thenReturn(bill);
        when(billDAO.updateReview(eq("B1"), eq("PAID"), any(), eq(1L))).thenReturn(0);
        assertThrows(RuntimeException.class, () -> settlementService.reviewBill("B1", true, null, 1L));

        when(billDAO.findById("B404")).thenReturn(null);
        assertThrows(RuntimeException.class, () -> settlementService.reviewBill("B404", true, null, 1L));

        when(billDAO.findById("B2")).thenReturn(bill);
        assertThrows(RuntimeException.class,
                () -> settlementService.reviewBill("B2", true, "x".repeat(256), 1L));
    }

    // ============ 分页方法与参数收敛 ============

    @Test
    @DisplayName("merchantLedger/merchantBills 参数收敛并透传 DAO（越界回退默认）")
    void paging_clampsAndDelegates() {
        settlementService.merchantLedger(5L, 1, 10);
        verify(ledgerDAO).findByMerchant(5L);

        settlementService.merchantBills(5L, 0, 999); // 越界 → (1, 10)
        verify(billDAO).findByMerchant(5L);

        assertThrows(RuntimeException.class, () -> settlementService.merchantLedger(null, 1, 10));
        assertThrows(RuntimeException.class, () -> settlementService.merchantBills(null, 1, 10));
    }

    @Test
    @DisplayName("adminBills 状态过滤透传；billEntries 透传与守卫")
    void adminPaging_andEntries() {
        settlementService.adminBills("PENDING", 1, 10);
        verify(billDAO).findPage("PENDING");

        settlementService.adminBills(null, 2, 60);
        verify(billDAO).findPage(null);

        when(ledgerDAO.findByBillId("B1")).thenReturn(List.of(earn("QH1", 5L, "100.00", "5.00")));
        assertEquals(1, settlementService.billEntries("B1").size());
        assertThrows(RuntimeException.class, () -> settlementService.billEntries(" "));
    }

    // ============ 对账恒等式 ============

    @Test
    @DisplayName("对账恒等式：Σ流水净额 == Σ已放款结算单 + 未入单净额（含冲销抵扣）")
    void reconciliation_identity() {
        // 场景：QH1 分账 95（100−5）→ 退款冲销 −95；QH2 分账 190（200−10）
        BigDecimal qh1Net = new BigDecimal("95.00");
        BigDecimal qh2Net = new BigDecimal("190.00");
        BigDecimal sumLedgerNet = qh1Net.add(qh2Net).subtract(qh1Net); // 190

        // 生成结算单：仅 QH2 的未结算 EARN（190）入单并放款
        when(ledgerDAO.findUnbilledEarn(5L)).thenReturn(java.util.List.of(earn("QH2", 5L, "200.00", "10.00")));
        directTransaction();
        SettlementBillDO bill = settlementService.generateBill(5L, 1L);
        when(billDAO.sumPaidByMerchant(5L)).thenReturn(bill.getTotalNet());
        when(ledgerDAO.sumNetByMerchant(5L)).thenReturn(sumLedgerNet);
        when(ledgerDAO.sumCommissionByMerchant(5L)).thenReturn(new BigDecimal("15.00"));

        var summary = settlementService.merchantSummary(5L);

        // Σ流水(190) == 已放款(190) + 未入单净额(0) —— 余额归零，冲销已抵扣
        assertEquals(0, ((BigDecimal) summary.get("balance")).compareTo(BigDecimal.ZERO));
        assertEquals(0, sumLedgerNet.compareTo(
                ((BigDecimal) summary.get("paidBills")).add((BigDecimal) summary.get("balance"))));
        assertNotNull(summary.get("totalCommission"));
    }
}
