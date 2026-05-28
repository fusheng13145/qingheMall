package com.qinghe.mall.user.entity;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String password;
    private String phone;
    private String email;
    private String avatar;
    private String nickname;
    private Integer gender; // 0:未知 1:男 2:女
    private Integer status; // 0:禁用 1:启用
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
