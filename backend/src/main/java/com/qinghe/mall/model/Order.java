package com.qinghe.mall.model;

import java.math.BigDecimal;
import java.util.Date;

public class Order {

    private String id;
    private String orderNumber;
    private Long userId;
    /** 归属商家ID（NULL=平台自营，M6） */
    private Long merchantId;
    private User user;
    private String productDetailId;
    private ProductDetail productDetail;
    private Integer quantity;
    private BigDecimal totalPrice;
    private OrderStatus status;

    // ===== 优惠券字段（落库于 order 表） =====
    /** 使用的用户券 id（user_coupon.id），无则为 null */
    private String couponId;
    /** 优惠券抵扣金额（实付 = totalPrice，原价可由 totalPrice + discountAmount 还原） */
    private BigDecimal discountAmount;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private Date gmtCreated;
    private Date gmtModified;

    // ===== 展示冗余字段（不落库，Service 组装时填充，供前端列表/详情展示） =====
    private String productName;
    private String productImg;

    /** 该订单是否已评价（订单中心评价按钮状态，Service 批量填充） */
    private Boolean commented;

    public Boolean getCommented() {
        return commented;
    }

    public void setCommented(Boolean commented) {
        this.commented = commented;
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

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getProductDetailId() {
        return productDetailId;
    }

    public void setProductDetailId(String productDetailId) {
        this.productDetailId = productDetailId;
    }

    public ProductDetail getProductDetail() {
        return productDetail;
    }

    public void setProductDetail(ProductDetail productDetail) {
        this.productDetail = productDetail;
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

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
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
}
