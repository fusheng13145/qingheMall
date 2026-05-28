package com.qinghe.mall.user.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

/**
 * 收货地址DTO
 */
@Data
public class AddressDTO {
    private Long id;

    @NotBlank(message = "收货人不能为空")
    private String receiver;

    @NotBlank(message = "手机号不能为空")
    private String phone;

    @NotBlank(message = "省份不能为空")
    private String province;

    @NotBlank(message = "城市不能为空")
    private String city;

    @NotBlank(message = "区县不能为空")
    private String district;

    @NotBlank(message = "详细地址不能为空")
    private String detail;

    private Integer isDefault;
}
