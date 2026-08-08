package com.qinghe.mall.dataobject;

import com.qinghe.mall.model.Product;
import java.math.BigDecimal;
import java.util.Date;

public class ProductDO {

    private String id;
    private String name;
    private String brand;
    /** 归属商家ID（NULL=平台自营，M6 平台化） */
    private Long merchantId;
    /** 上架状态：ON 在售 / OFF 下架（M6） */
    private String status;
    private BigDecimal price;
    private Integer purchaseNum;
    private String productIntro;
    private String productImgs;
    private Date gmtCreated;
    private Date gmtModified;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getPurchaseNum() {
        return purchaseNum;
    }

    public void setPurchaseNum(Integer purchaseNum) {
        this.purchaseNum = purchaseNum;
    }

    public String getProductIntro() {
        return productIntro;
    }

    public void setProductIntro(String productIntro) {
        this.productIntro = productIntro;
    }

    public String getProductImgs() {
        return productImgs;
    }

    public void setProductImgs(String productImgs) {
        this.productImgs = productImgs;
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

    public Product convertToModel() {
        Product product = new Product();
        product.setId(this.id);
        product.setName(this.name);
        product.setBrand(this.brand);
        product.setMerchantId(this.merchantId);
        product.setStatus(this.status);
        product.setPrice(this.price);
        product.setPurchaseNum(this.purchaseNum);
        product.setProductIntro(this.productIntro);
        product.setProductImgs(this.productImgs);
        product.setGmtCreated(this.gmtCreated);
        product.setGmtModified(this.gmtModified);
        return product;
    }
}
