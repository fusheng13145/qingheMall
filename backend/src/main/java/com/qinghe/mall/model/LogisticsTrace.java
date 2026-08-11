package com.qinghe.mall.model;

import java.util.Date;

/**
 * 物流轨迹节点（P2-18）：供前端时间线渲染，按 traceTime 正序。
 */
public class LogisticsTrace {

    private String description;
    private Date traceTime;

    public LogisticsTrace() {
    }

    public LogisticsTrace(String description, Date traceTime) {
        this.description = description;
        this.traceTime = traceTime;
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
}
