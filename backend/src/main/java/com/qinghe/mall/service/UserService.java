package com.qinghe.mall.service;

import com.qinghe.mall.model.User;
import java.util.List;

public interface UserService {

    User register(String userName, String pwd);

    /** 注册并指定角色（M6：USER/MERCHANT），默认 USER 的便捷注册见 {@link #register(String, String)} */
    User register(String userName, String pwd, String role);

    User login(String userName, String pwd);

    User findByUserName(String userName);

    List<User> findAll();

    /** P1-11：管理端用户分页查询（消除全表捞取） */
    com.qinghe.mall.model.Paging<User> findAdminPage(Integer pagination, Integer pageSize);

    User findById(Long id);

    /** 批量查询用户（订单列表消除 N+1） */
    List<User> findByIds(List<Long> ids);

    /** 用户总数（看板聚合） */
    long countAll();

    boolean updateRole(Long id, String role);

    /** 更新个人资料（昵称/头像），返回更新后的用户（pwd 已置空） */
    User updateProfile(Long id, String nickName, String avatar);
}
