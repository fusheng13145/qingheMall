package com.qinghe.mall.dataobject;

import java.util.Date;

/**
 * 秒杀订单 DO（M5-A3）。
 *
 * 用户抢购成功后生成，关联活动与用户，并对应用户生成的普通订单（order_number）。
 * 状态仅使用 CREATED（待支付）/ CANCELLED（超时或取消回滚）两态；
 * 已支付由关联的普通 order 状态推导，无需冗余存储。
 * {@code uk_user_activity(user_id, activity_id)} 唯一约束作为「同一用户不可重复抢」的最终兜底。
 */
public class SeckillOrderDO {

    private String id;
    private Long userId;
    private String activityId;
    private String productDetailId;
    private Integer quantity;
    private String orderNumber;
    private String status;
    private Date gmtCreated;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getActivityId() {
        return activityId;
    }

    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }

    public String getProductDetailId() {
        return productDetailId;
    }

    public void setProductDetailId(String productDetailId) {
        this.productDetailId = productDetailId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
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
}
