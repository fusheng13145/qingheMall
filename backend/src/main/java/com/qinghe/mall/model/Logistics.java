package com.qinghe.mall.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 物流信息视图（P2-18）：承运商/运单号/当前状态 + 轨迹时间线。
 * 状态机：SHIPPED → IN_TRANSIT → DELIVERING → SIGNED（终态）。
 */
public class Logistics {

    private String orderNumber;
    private String company;
    private String trackingNumber;
    private String status;
    private Date gmtCreated;
    private List<LogisticsTrace> traces = new ArrayList<>();

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

    public List<LogisticsTrace> getTraces() {
        return traces;
    }

    public void setTraces(List<LogisticsTrace> traces) {
        this.traces = traces;
    }
}
