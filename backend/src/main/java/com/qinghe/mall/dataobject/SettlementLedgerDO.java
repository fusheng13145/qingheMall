package com.qinghe.mall.dataobject;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 商家分账流水（A2，v1.5）。
 *
 * 类型：
 * - EARN：订单确认收货（COMPLETED）时按实付金额分账，net = gross - commission（正）；
 * - REVERSAL：退款审核通过时冲销对应订单的 EARN，net 为负，gross/commission 恒正。
 *
 * uk_order_type 保证同一订单至多一条 EARN + 一条 REVERSAL（幂等兜底）。
 * bill_id 非空表示已纳入结算单；驳回时由结算单审核退回 NULL。
 */
public class SettlementLedgerDO {

    public static final String TYPE_EARN = "EARN";
    public static final String TYPE_REVERSAL = "REVERSAL";

    private String id;
    private String orderNumber;
    private Long merchantId;
    private String type;
    private BigDecimal gross;
    private BigDecimal commissionRate;
    private BigDecimal commission;
    private BigDecimal net;
    private String billId;
    private Date gmtCreated;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    public Long getMerchantId() { return merchantId; }
    public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public BigDecimal getGross() { return gross; }
    public void setGross(BigDecimal gross) { this.gross = gross; }
    public BigDecimal getCommissionRate() { return commissionRate; }
    public void setCommissionRate(BigDecimal commissionRate) { this.commissionRate = commissionRate; }
    public BigDecimal getCommission() { return commission; }
    public void setCommission(BigDecimal commission) { this.commission = commission; }
    public BigDecimal getNet() { return net; }
    public void setNet(BigDecimal net) { this.net = net; }
    public String getBillId() { return billId; }
    public void setBillId(String billId) { this.billId = billId; }
    public Date getGmtCreated() { return gmtCreated; }
    public void setGmtCreated(Date gmtCreated) { this.gmtCreated = gmtCreated; }
}
