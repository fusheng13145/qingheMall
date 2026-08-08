package com.qinghe.mall.dataobject;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 秒杀活动 DO（M5-A3）。
 *
 * 后台创建，绑定一个 SKU，设定秒杀价、活动库存、起止时间与状态。
 * {@code remainStock} 为防超卖主防线：扣减走 {@code WHERE remain_stock >= ?} 的 CAS 更新。
 */
public class SeckillActivityDO {

    private String id;
    private String productDetailId;
    private BigDecimal seckillPrice;
    private Integer totalStock;
    private Integer remainStock;
    private Date startTime;
    private Date endTime;
    private String status;
    private Date gmtCreated;
    private Date gmtModified;

    // 展示冗余字段（JOIN product_detail / product 填充，不落库）
    private String productName;
    private String productImg;
    private java.math.BigDecimal originalPrice;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProductDetailId() {
        return productDetailId;
    }

    public void setProductDetailId(String productDetailId) {
        this.productDetailId = productDetailId;
    }

    public BigDecimal getSeckillPrice() {
        return seckillPrice;
    }

    public void setSeckillPrice(BigDecimal seckillPrice) {
        this.seckillPrice = seckillPrice;
    }

    public Integer getTotalStock() {
        return totalStock;
    }

    public void setTotalStock(Integer totalStock) {
        this.totalStock = totalStock;
    }

    public Integer getRemainStock() {
        return remainStock;
    }

    public void setRemainStock(Integer remainStock) {
        this.remainStock = remainStock;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getGmtCreated() {
        return gmtCreated;
    }

    public void setGmtCreated(Date gmtCreated) {
        this.gmtCreated = gmtCreated;
    }

    public Date getGmtModified() {
        return gmtModified;
    }

    public void setGmtModified(Date gmtModified) {
        this.gmtModified = gmtModified;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductImg() {
        return productImg;
    }

    public void setProductImg(String productImg) {
        this.productImg = productImg;
    }

    public java.math.BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(java.math.BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }
}
