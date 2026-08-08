package com.qinghe.mall.dataobject;

import com.qinghe.mall.model.ProductDetail;
import java.math.BigDecimal;
import java.util.Date;

public class ProductDetailDO {

    private String id;
    private String productId;
    private BigDecimal price;
    private Double size;
    private Integer stock;
    private Date gmtCreated;
    private Date gmtModified;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Double getSize() {
        return size;
    }

    public void setSize(Double size) {
        this.size = size;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
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

    public ProductDetail convertToModel() {
        ProductDetail productDetail = new ProductDetail();
        productDetail.setId(this.id);
        productDetail.setProductId(this.productId);
        productDetail.setPrice(this.price);
        productDetail.setSize(this.size);
        productDetail.setStock(this.stock);
        productDetail.setGmtCreated(this.gmtCreated);
        productDetail.setGmtModified(this.gmtModified);
        return productDetail;
    }
}
