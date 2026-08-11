package com.qinghe.mall.dataobject;

import java.util.Date;

/**
 * 物流轨迹事件（P2-18）：按订单号聚合的时间线，展示顺序由 trace_time 决定。
 */
public class LogisticsTraceDO {

    private Long id;
    private String orderNumber;
    private String description;
    private Date traceTime;
    private Date gmtCreated;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getTraceTime() {
        return traceTime;
    }

    public void setTraceTime(Date traceTime) {
        this.traceTime = traceTime;
    }

    public Date getGmtCreated() {
        return gmtCreated;
    }

    public void setGmtCreated(Date gmtCreated) {
        this.gmtCreated = gmtCreated;
    }
}
