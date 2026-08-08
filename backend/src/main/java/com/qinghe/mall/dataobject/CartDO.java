package com.qinghe.mall.dataobject;

import com.qinghe.mall.model.Cart;
import java.util.Date;

public class CartDO {

    private Long id;
    private Long userId;
    private String productDetailId;
    private Integer quantity;
    private Integer selected;
    private Date gmtCreated;
    private Date gmtModified;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getSelected() {
        return selected;
    }

    public void setSelected(Integer selected) {
        this.selected = selected;
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

    public Cart convertToModel() {
        Cart cart = new Cart();
        cart.setId(this.id);
        cart.setUserId(this.userId);
        cart.setProductDetailId(this.productDetailId);
        cart.setQuantity(this.quantity);
        cart.setSelected(this.selected != null && this.selected == 1);
        cart.setGmtCreated(this.gmtCreated);
        cart.setGmtModified(this.gmtModified);
        return cart;
    }
}
