package com.qinghe.mall.dataobject;

import java.util.Date;

/**
 * 物流记录（P2-18）：一单一记录，承载承运商/运单号与流转状态。
 * 状态机：SHIPPED → IN_TRANSIT → DELIVERING → SIGNED（终态）。
 */
public class LogisticsDO {

    /** 已发货（商家录入运单，包裹已揽收） */
    public static final String STATUS_SHIPPED = "SHIPPED";
    /** 运输中 */
    public static final String STATUS_IN_TRANSIT = "IN_TRANSIT";
    /** 派送中 */
    public static final String STATUS_DELIVERING = "DELIVERING";
    /** 已签收（终态） */
    public static final String STATUS_SIGNED = "SIGNED";

    private String id;
    private String orderNumber;
    private String company;
    private String trackingNumber;
    private String status;
    private Date gmtCreated;
    private Date gmtModified;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
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
}
