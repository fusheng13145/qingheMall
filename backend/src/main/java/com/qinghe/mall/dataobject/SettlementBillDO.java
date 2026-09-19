package com.qinghe.mall.dataobject;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 商家结算单（A2，v1.5）。
 *
 * 由管理端按商家生成：汇总该商家全部未结算（bill_id 为空）的 EARN 流水；
 * 审核放款置 PAID，驳回置 REJECTED 并将流水退回未结算（bill_id 置空）。
 * 冲销负流水不进结算单，直接抵减商家余额。
 */
public class SettlementBillDO {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PAID = "PAID";
    public static final String STATUS_REJECTED = "REJECTED";

    private String id;
    private Long merchantId;
    private BigDecimal totalGross;
    private BigDecimal totalCommission;
    private BigDecimal totalNet;
    private Integer entryCount;
    private String status;
    private String reviewNote;
    private Long reviewerId;
    private Date gmtCreated;
    private Date gmtReviewed;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getMerchantId() { return merchantId; }
    public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }
    public BigDecimal getTotalGross() { return totalGross; }
    public void setTotalGross(BigDecimal totalGross) { this.totalGross = totalGross; }
    public BigDecimal getTotalCommission() { return totalCommission; }
    public void setTotalCommission(BigDecimal totalCommission) { this.totalCommission = totalCommission; }
    public BigDecimal getTotalNet() { return totalNet; }
    public void setTotalNet(BigDecimal totalNet) { this.totalNet = totalNet; }
    public Integer getEntryCount() { return entryCount; }
    public void setEntryCount(Integer entryCount) { this.entryCount = entryCount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }
    public Long getReviewerId() { return reviewerId; }
    public void setReviewerId(Long reviewerId) { this.reviewerId = reviewerId; }
    public Date getGmtCreated() { return gmtCreated; }
    public void setGmtCreated(Date gmtCreated) { this.gmtCreated = gmtCreated; }
    public Date getGmtReviewed() { return gmtReviewed; }
    public void setGmtReviewed(Date gmtReviewed) { this.gmtReviewed = gmtReviewed; }
}
