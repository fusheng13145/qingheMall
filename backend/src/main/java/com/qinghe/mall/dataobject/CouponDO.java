package com.qinghe.mall.dataobject;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 优惠券模板（运营在后台创建）。
 * type: FULL_REDUCTION 满减 / DISCOUNT 折扣。
 * merchantId: 归属商家ID（NULL=平台券；M6 平台化支持商家自建券）。
 */
public class CouponDO {

    private String id;
    private String name;
    private String type;
    private BigDecimal threshold;
    private BigDecimal amount;
    private BigDecimal discount;
    private BigDecimal maxDiscount;
    private Integer total;
    private Integer issued;
    private Integer perLimit;
    private Date startTime;
    private Date endTime;
    private String status;
    private Long merchantId;
    private Date gmtCreated;
    private Date gmtModified;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public BigDecimal getThreshold() { return threshold; }
    public void setThreshold(BigDecimal threshold) { this.threshold = threshold; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }

    public BigDecimal getMaxDiscount() { return maxDiscount; }
    public void setMaxDiscount(BigDecimal maxDiscount) { this.maxDiscount = maxDiscount; }

    public Integer getTotal() { return total; }
    public void setTotal(Integer total) { this.total = total; }

    public Integer getIssued() { return issued; }
    public void setIssued(Integer issued) { this.issued = issued; }

    public Integer getPerLimit() { return perLimit; }
    public void setPerLimit(Integer perLimit) { this.perLimit = perLimit; }

    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }

    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getMerchantId() { return merchantId; }
    public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }

    public Date getGmtCreated() { return gmtCreated; }
    public void setGmtCreated(Date gmtCreated) { this.gmtCreated = gmtCreated; }

    public Date getGmtModified() { return gmtModified; }
    public void setGmtModified(Date gmtModified) { this.gmtModified = gmtModified; }
}
