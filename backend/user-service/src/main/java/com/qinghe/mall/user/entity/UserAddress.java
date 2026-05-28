package com.qinghe.mall.user.entity;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 收货地址实体
 */
@Data
public class UserAddress implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String receiver;
    private String phone;
    private String province;
    private String city;
    private String district;
    private String detail;
    private Integer isDefault; // 0:非默认 1:默认
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
