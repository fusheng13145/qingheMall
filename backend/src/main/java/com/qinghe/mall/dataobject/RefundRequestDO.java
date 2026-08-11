package com.qinghe.mall.dataobject;

import java.util.Date;

/**
 * 退款/退货申请单（P2-18）。
 *
 * 类型：
 * - REFUND_ONLY：仅退款（未发货订单，无需退货）
 * - RETURN_REFUND：退货退款（已发货/已完成订单，货物退回后退款）
 *
 * previous_order_status 记录申请前订单状态：驳回时订单回退到该状态
 * （TRADE_PAID_SUCCESS / TRADE_SHIPPED / TRADE_COMPLETED 均可能）。
 */
public class RefundRequestDO {

    public static final String TYPE_REFUND_ONLY = "REFUND_ONLY";
    public static final String TYPE_RETURN_REFUND = "RETURN_REFUND";

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    private String id;
    private String orderNumber;
    private Long userId;
    private String type;
    private String reason;
    private String status;
    private String previousOrderStatus;
    private String reviewComment;
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPreviousOrderStatus() {
        return previousOrderStatus;
    }

    public void setPreviousOrderStatus(String previousOrderStatus) {
        this.previousOrderStatus = previousOrderStatus;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
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
