package com.qinghe.mall.dataobject;

import com.qinghe.mall.model.PayType;
import com.qinghe.mall.model.PaymentRecord;
import com.qinghe.mall.model.PaymentStatus;
import java.util.Date;

public class PaymentRecordDO {

    private String id;
    private Long userId;
    private String orderNumber;
    private String channelPaymentId;
    private String channelType;
    private Double amount;
    private String payType;
    private String payStatus;
    private String extendStr;
    private Date payEndTime;
    private Date gmtCreated;
    private Date gmtModified;

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

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getChannelPaymentId() {
        return channelPaymentId;
    }

    public void setChannelPaymentId(String channelPaymentId) {
        this.channelPaymentId = channelPaymentId;
    }

    public String getChannelType() {
        return channelType;
    }

    public void setChannelType(String channelType) {
        this.channelType = channelType;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getPayType() {
        return payType;
    }

    public void setPayType(String payType) {
        this.payType = payType;
    }

    public String getPayStatus() {
        return payStatus;
    }

    public void setPayStatus(String payStatus) {
        this.payStatus = payStatus;
    }

    public String getExtendStr() {
        return extendStr;
    }

    public void setExtendStr(String extendStr) {
        this.extendStr = extendStr;
    }

    public Date getPayEndTime() {
        return payEndTime;
    }

    public void setPayEndTime(Date payEndTime) {
        this.payEndTime = payEndTime;
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

    public PaymentRecord convertToModel() {
        PaymentRecord record = new PaymentRecord();
        record.setId(this.id);
        record.setUserId(this.userId);
        record.setOrderNumber(this.orderNumber);
        record.setChannelPaymentId(this.channelPaymentId);
        record.setChannelType(this.channelType);
        record.setAmount(this.amount);
        if (this.payType != null) {
            record.setPayType(PayType.valueOf(this.payType));
        }
        if (this.payStatus != null) {
            record.setPayStatus(PaymentStatus.valueOf(this.payStatus));
        }
        record.setExtendStr(this.extendStr);
        record.setPayEndTime(this.payEndTime);
        record.setGmtCreated(this.gmtCreated);
        record.setGmtModified(this.gmtModified);
        return record;
    }
}
