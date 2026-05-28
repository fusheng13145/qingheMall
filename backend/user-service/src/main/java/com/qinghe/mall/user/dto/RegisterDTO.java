package com.qinghe.mall.user.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

/**
 * 注册参数
 */
@Data
public class RegisterDTO {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "手机号不能为空")
    private String phone;

    @NotBlank(message = "验证码不能为空")
    private String code;
}
