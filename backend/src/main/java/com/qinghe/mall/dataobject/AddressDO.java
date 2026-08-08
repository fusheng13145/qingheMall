package com.qinghe.mall.dataobject;

import com.qinghe.mall.model.Address;
import java.util.Date;

public class AddressDO {

    private Long id;
    private Long userId;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private Integer isDefault;
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

    public Integer getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Integer isDefault) {
        this.isDefault = isDefault;
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

    public Address convertToModel() {
        Address address = new Address();
        address.setId(this.id);
        address.setUserId(this.userId);
        address.setReceiverName(this.receiverName);
        address.setReceiverPhone(this.receiverPhone);
        address.setReceiverAddress(this.receiverAddress);
        address.setIsDefault(this.isDefault != null && this.isDefault == 1);
        address.setGmtCreated(this.gmtCreated);
        address.setGmtModified(this.gmtModified);
        return address;
    }
}
