package com.qinghe.mall.dataobject;

import com.qinghe.mall.model.Comment;
import java.util.Date;

public class CommentDO {

    private String id;
    private Long userId;
    private String productId;
    private String orderNumber;
    private Integer rating;
    private String content;
    private Date gmtCreated;
    private Date gmtModified;

    /** 查询冗余：商品名（findByMerchant JOIN product，不落库） */
    private String productName;

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

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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

    public Comment convertToModel() {
        Comment comment = new Comment();
        comment.setId(this.id);
        comment.setUserId(this.userId);
        comment.setProductId(this.productId);
        comment.setOrderNumber(this.orderNumber);
        comment.setRating(this.rating);
        comment.setContent(this.content);
        comment.setGmtCreated(this.gmtCreated);
        comment.setGmtModified(this.gmtModified);
        comment.setProductName(this.productName);
        return comment;
    }
}
