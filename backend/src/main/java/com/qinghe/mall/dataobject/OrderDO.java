package com.qinghe.mall.dataobject;

import com.qinghe.mall.model.Order;
import com.qinghe.mall.model.OrderStatus;
import java.util.Date;

public class OrderDO {

    private String id;
    private String orderNumber;
    private Long userId;
    private String productDetailId;
    private Double totalPrice;
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getProductDetailId() {
        return productDetailId;
    }

    public void setProductDetailId(String productDetailId) {
        this.productDetailId = productDetailId;
    }

    public Double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(Double totalPrice) {
        this.totalPrice = totalPrice;
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

    public Order convertToModel() {
        Order order = new Order();
        order.setId(this.id);
        order.setOrderNumber(this.orderNumber);
        order.setUserId(this.userId);
        order.setProductDetailId(this.productDetailId);
        order.setTotalPrice(this.totalPrice);
        if (this.status != null) {
            order.setStatus(OrderStatus.valueOf(this.status));
        }
        order.setGmtCreated(this.gmtCreated);
        order.setGmtModified(this.gmtModified);
        return order;
    }
}
