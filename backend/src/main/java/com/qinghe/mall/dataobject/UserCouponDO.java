package com.qinghe.mall.dataobject;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 用户持有券（领取后生成）。
 * status: UNUSED 未使用 / USED 已使用 / EXPIRED 已过期 / RELEASED 已释放
 *
 * 下列 coupon* 字段为 JOIN coupon 表的展示冗余列（不落库），便于前端直接展示券信息。
 */
public class UserCouponDO {

    private String id;
    private Long userId;
    private String couponId;
    private String status;
    private String orderNumber;
    private Date usedTime;
    private Date gmtCreated;
    private Date gmtModified;

    // ===== 关联券模板展示字段（来自 coupon 表，查询时填充） =====
    private String couponName;
    private String couponType;
    private BigDecimal threshold;
    private BigDecimal couponAmount;
    private BigDecimal couponDiscount;
    private BigDecimal couponMaxDiscount;
    private Date couponEndTime;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getCouponId() { return couponId; }
    public void setCouponId(String couponId) { this.couponId = couponId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public Date getUsedTime() { return usedTime; }
    public void setUsedTime(Date usedTime) { this.usedTime = usedTime; }

    public Date getGmtCreated() { return gmtCreated; }
    public void setGmtCreated(Date gmtCreated) { this.gmtCreated = gmtCreated; }

    public Date getGmtModified() { return gmtModified; }
    public void setGmtModified(Date gmtModified) { this.gmtModified = gmtModified; }

    public String getCouponName() { return couponName; }
    public void setCouponName(String couponName) { this.couponName = couponName; }

    public String getCouponType() { return couponType; }
    public void setCouponType(String couponType) { this.couponType = couponType; }

    public BigDecimal getThreshold() { return threshold; }
    public void setThreshold(BigDecimal threshold) { this.threshold = threshold; }

    public BigDecimal getCouponAmount() { return couponAmount; }
    public void setCouponAmount(BigDecimal couponAmount) { this.couponAmount = couponAmount; }

    public BigDecimal getCouponDiscount() { return couponDiscount; }
    public void setCouponDiscount(BigDecimal couponDiscount) { this.couponDiscount = couponDiscount; }

    public BigDecimal getCouponMaxDiscount() { return couponMaxDiscount; }
    public void setCouponMaxDiscount(BigDecimal couponMaxDiscount) { this.couponMaxDiscount = couponMaxDiscount; }

    public Date getCouponEndTime() { return couponEndTime; }
    public void setCouponEndTime(Date couponEndTime) { this.couponEndTime = couponEndTime; }
}
