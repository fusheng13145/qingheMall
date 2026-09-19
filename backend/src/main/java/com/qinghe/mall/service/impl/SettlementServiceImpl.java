package com.qinghe.mall.service.impl;

import com.qinghe.mall.dao.SettlementBillDAO;
import com.qinghe.mall.dao.SettlementLedgerDAO;
import com.qinghe.mall.dataobject.OrderDO;
import com.qinghe.mall.dataobject.SettlementBillDO;
import com.qinghe.mall.dataobject.SettlementLedgerDO;
import com.qinghe.mall.exception.BusinessException;
import com.qinghe.mall.model.Paging;
import com.qinghe.mall.service.SettlementService;
import com.qinghe.mall.util.UUIDUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 商家结算分账实现（A2，v1.5）。
 *
 * 金额口径（全部 DECIMAL 半进舍入 2 位）：
 * - EARN：gross = 订单实付（total_price，已扣券）；commission = gross × rate；net = gross − commission；
 * - REVERSAL：gross = 原 EARN 的 gross；commission = 原 EARN 的 commission；net = −(gross − commission)。
 *
 * 余额 = Σ(全部流水 net) − Σ(PAID 结算单 total_net)：冲销负流水直接抵减余额。
 */
@Service
public class SettlementServiceImpl implements SettlementService {

    @Autowired
    private SettlementLedgerDAO ledgerDAO;

    @Autowired
    private SettlementBillDAO billDAO;

    @Autowired
    private TransactionTemplate transactionTemplate;

    /** 平台佣金费率（0~1，分账时点快照入流水） */
    @Value("${app.settlement.commission-rate:0.05}")
    private BigDecimal commissionRate;

    @Override
    public void recordEarning(OrderDO order) {
        if (order == null || order.getMerchantId() == null) {
            // 平台自营（merchant_id NULL）无商家货款，不分账
            return;
        }
        SettlementLedgerDO existing =
                ledgerDAO.findByOrderNumberAndType(order.getOrderNumber(), SettlementLedgerDO.TYPE_EARN);
        if (existing != null) {
            return; // 幂等：重复确认收货/重复触发不重复入账
        }
        BigDecimal gross = order.getTotalPrice() == null ? BigDecimal.ZERO : order.getTotalPrice();
        BigDecimal commission = gross.multiply(commissionRate).setScale(2, RoundingMode.HALF_UP);
        SettlementLedgerDO ledger = new SettlementLedgerDO();
        ledger.setId(UUIDUtils.uuid());
        ledger.setOrderNumber(order.getOrderNumber());
        ledger.setMerchantId(order.getMerchantId());
        ledger.setType(SettlementLedgerDO.TYPE_EARN);
        ledger.setGross(gross);
        ledger.setCommissionRate(commissionRate);
        ledger.setCommission(commission);
        ledger.setNet(gross.subtract(commission));
        ledger.setGmtCreated(new Date());
        ledgerDAO.insert(ledger);
    }

    @Override
    public void recordReversal(OrderDO order) {
        if (order == null || order.getMerchantId() == null) {
            return;
        }
        SettlementLedgerDO earn =
                ledgerDAO.findByOrderNumberAndType(order.getOrderNumber(), SettlementLedgerDO.TYPE_EARN);
        if (earn == null) {
            // 确认收货前退款：从未分账，无需冲销
            return;
        }
        SettlementLedgerDO existing =
                ledgerDAO.findByOrderNumberAndType(order.getOrderNumber(), SettlementLedgerDO.TYPE_REVERSAL);
        if (existing != null) {
            return; // 幂等
        }
        SettlementLedgerDO reversal = new SettlementLedgerDO();
        reversal.setId(UUIDUtils.uuid());
        reversal.setOrderNumber(order.getOrderNumber());
        reversal.setMerchantId(order.getMerchantId());
        reversal.setType(SettlementLedgerDO.TYPE_REVERSAL);
        reversal.setGross(earn.getGross());
        reversal.setCommissionRate(earn.getCommissionRate());
        reversal.setCommission(earn.getCommission());
        reversal.setNet(earn.getNet().negate());
        reversal.setGmtCreated(new Date());
        ledgerDAO.insert(reversal);
    }

    @Override
    public Map<String, Object> merchantSummary(Long merchantId) {
        if (merchantId == null) {
            throw new BusinessException("商家身份缺失");
        }
        BigDecimal ledgerNet = ledgerDAO.sumNetByMerchant(merchantId);
        BigDecimal paidBills = billDAO.sumPaidByMerchant(merchantId);
        BigDecimal balance = ledgerNet.subtract(paidBills);
        Map<String, Object> summary = new HashMap<>();
        summary.put("balance", balance);
        summary.put("totalNet", ledgerNet);
        summary.put("totalCommission", ledgerDAO.sumCommissionByMerchant(merchantId));
        summary.put("paidBills", paidBills);
        return summary;
    }

    @Override
    public Paging<SettlementLedgerDO> merchantLedger(Long merchantId, int pageNum, int pageSize) {
        if (merchantId == null) {
            throw new BusinessException("商家身份缺失");
        }
        int[] ps = clamp(pageNum, pageSize);
        com.github.pagehelper.Page<SettlementLedgerDO> page = com.github.pagehelper.PageHelper
                .startPage(ps[0], ps[1])
                .doSelectPage(() -> ledgerDAO.findByMerchant(merchantId));
        return toPaging(ps[0], ps[1], page);
    }

    @Override
    public Paging<SettlementBillDO> merchantBills(Long merchantId, int pageNum, int pageSize) {
        if (merchantId == null) {
            throw new BusinessException("商家身份缺失");
        }
        int[] ps = clamp(pageNum, pageSize);
        com.github.pagehelper.Page<SettlementBillDO> page = com.github.pagehelper.PageHelper
                .startPage(ps[0], ps[1])
                .doSelectPage(() -> billDAO.findByMerchant(merchantId));
        return toPaging(ps[0], ps[1], page);
    }

    @Override
    public SettlementBillDO generateBill(Long merchantId, Long operatorId) {
        if (merchantId == null) {
            throw new BusinessException("商家ID不能为空");
        }
        return transactionTemplate.execute(status -> {
            List<SettlementLedgerDO> entries = ledgerDAO.findUnbilledEarn(merchantId);
            if (entries.isEmpty()) {
                throw new BusinessException("该商家没有可结算的流水");
            }
            BigDecimal totalGross = BigDecimal.ZERO;
            BigDecimal totalCommission = BigDecimal.ZERO;
            BigDecimal totalNet = BigDecimal.ZERO;
            List<String> ids = new ArrayList<>();
            for (SettlementLedgerDO e : entries) {
                totalGross = totalGross.add(e.getGross());
                totalCommission = totalCommission.add(e.getCommission());
                totalNet = totalNet.add(e.getNet());
                ids.add(e.getId());
            }
            SettlementBillDO bill = new SettlementBillDO();
            bill.setId(UUIDUtils.uuid());
            bill.setMerchantId(merchantId);
            bill.setTotalGross(totalGross);
            bill.setTotalCommission(totalCommission);
            bill.setTotalNet(totalNet);
            bill.setEntryCount(entries.size());
            bill.setStatus(SettlementBillDO.STATUS_PENDING);
            bill.setReviewerId(operatorId);
            bill.setGmtCreated(new Date());
            billDAO.insert(bill);
            ledgerDAO.markBilled(bill.getId(), ids);
            return bill;
        });
    }

    @Override
    public void reviewBill(String billId, boolean approve, String reviewNote, Long operatorId) {
        if (StringUtils.isBlank(billId)) {
            throw new BusinessException("结算单ID不能为空");
        }
        String note = StringUtils.isBlank(reviewNote) ? null : reviewNote.trim();
        if (note != null && note.length() > 255) {
            throw new BusinessException("审核备注不能超过 255 字");
        }
        SettlementBillDO bill = billDAO.findById(billId);
        if (bill == null) {
            throw new BusinessException("结算单不存在");
        }
        String target = approve ? SettlementBillDO.STATUS_PAID : SettlementBillDO.STATUS_REJECTED;
        int updated = billDAO.updateReview(billId, target, note, operatorId);
        if (updated <= 0) {
            throw new BusinessException("该结算单已审核，请刷新后重试");
        }
        if (!approve) {
            // 驳回：流水退回未结算池，可重新生成结算单
            List<SettlementLedgerDO> entries = ledgerDAO.findByBillId(billId);
            List<String> ids = new ArrayList<>();
            for (SettlementLedgerDO e : entries) {
                ids.add(e.getId());
            }
            if (!ids.isEmpty()) {
                ledgerDAO.markBilled(null, ids);
            }
        }
    }

    @Override
    public Paging<SettlementBillDO> adminBills(String status, int pageNum, int pageSize) {
        int[] ps = clamp(pageNum, pageSize);
        com.github.pagehelper.Page<SettlementBillDO> page = com.github.pagehelper.PageHelper
                .startPage(ps[0], ps[1])
                .doSelectPage(() -> billDAO.findPage(status));
        return toPaging(ps[0], ps[1], page);
    }

    @Override
    public List<SettlementLedgerDO> billEntries(String billId) {
        if (StringUtils.isBlank(billId)) {
            throw new BusinessException("结算单ID不能为空");
        }
        return ledgerDAO.findByBillId(billId);
    }

    private int[] clamp(int pageNum, int pageSize) {
        if (pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize < 1 || pageSize > 50) {
            pageSize = 10;
        }
        return new int[]{pageNum, pageSize};
    }

    private <T> Paging<T> toPaging(int pageNum, int pageSize, com.github.pagehelper.Page<T> page) {
        Paging<T> paging = new Paging<>();
        paging.setPageNum(pageNum);
        paging.setPageSize(pageSize);
        paging.setTotalPage(page.getPages());
        paging.setTotalCount(page.getTotal());
        paging.setData(page.getResult());
        return paging;
    }
}
