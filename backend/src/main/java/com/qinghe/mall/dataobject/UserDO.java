package com.qinghe.mall.dataobject;

import com.qinghe.mall.model.User;
import java.time.LocalDateTime;

public class UserDO {

    /** 角色常量（M6 平台化：顾客/商家/管理员） */
    public static final String ROLE_USER = "USER";
    public static final String ROLE_MERCHANT = "MERCHANT";
    public static final String ROLE_ADMIN = "ADMIN";

    private Long id;
    private String userName;
    private String pwd;
    private String nickName;
    private String avatar;
    private String role;
    private LocalDateTime gmtCreated;
    private LocalDateTime gmtModified;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getGmtCreated() {
        return gmtCreated;
    }

    public void setGmtCreated(LocalDateTime gmtCreated) {
        this.gmtCreated = gmtCreated;
    }

    public LocalDateTime getGmtModified() {
        return gmtModified;
    }

    public void setGmtModified(LocalDateTime gmtModified) {
        this.gmtModified = gmtModified;
    }

    public User convertToModel() {
        User user = new User();
        user.setId(this.id);
        user.setUserName(this.userName);
        user.setPwd(this.pwd);
        user.setNickName(this.nickName);
        user.setAvatar(this.avatar);
        user.setRole(this.role);
        user.setGmtCreated(this.gmtCreated);
        user.setGmtModified(this.gmtModified);
        return user;
    }
}
