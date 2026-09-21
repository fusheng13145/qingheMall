package com.qinghe.mall.dataobject;

import com.qinghe.mall.model.Order;
import com.qinghe.mall.model.OrderStatus;
import java.math.BigDecimal;
import java.util.Date;

public class OrderDO {

    private Long id;
    private String orderNumber;
    private Long userId;
    /** 归属商家ID（NULL=平台自营，M6 平台化；下单时由商品归属推导落库） */
    private Long merchantId;
    private String productDetailId;
    private Integer quantity;
    private BigDecimal totalPrice;
    private String status;
    private String couponId;
    private BigDecimal discountAmount;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private Date gmtCreated;
    private Date gmtModified;

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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
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

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCouponId() {
        return couponId;
    }

    public void setCouponId(String couponId) {
        this.couponId = couponId;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getReceiverPhone() {
        return receiverPhone;
    }

    public void setReceiverPhone(String receiverPhone) {
        this.receiverPhone = receiverPhone;
    }

    public String getReceiverAddress() {
        return receiverAddress;
    }

    public void setReceiverAddress(String receiverAddress) {
        this.receiverAddress = receiverAddress;
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
        order.setId(this.id == null ? null : String.valueOf(this.id));
        order.setOrderNumber(this.orderNumber);
        order.setUserId(this.userId);
        order.setMerchantId(this.merchantId);
        order.setProductDetailId(this.productDetailId);
        order.setQuantity(this.quantity);
        order.setTotalPrice(this.totalPrice);
        if (this.status != null) {
            order.setStatus(OrderStatus.valueOf(this.status));
        }
        order.setReceiverName(this.receiverName);
        order.setReceiverPhone(this.receiverPhone);
        order.setReceiverAddress(this.receiverAddress);
        order.setCouponId(this.couponId);
        order.setDiscountAmount(this.discountAmount);
        order.setGmtCreated(this.gmtCreated);
        order.setGmtModified(this.gmtModified);
        return order;
    }
}
