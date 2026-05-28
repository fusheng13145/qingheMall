package com.qinghe.mall.user.dto;

import lombok.Data;

/**
 * 用户信息返回
 */
@Data
public class UserInfoDTO {
    private Long id;
    private String username;
    private String phone;
    private String email;
    private String avatar;
    private String nickname;
    private Integer gender;
    private Integer status;
}
