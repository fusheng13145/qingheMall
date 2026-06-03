package com.qinghe.mall.service;

import com.qinghe.mall.model.User;
import java.util.List;

public interface UserService {

    User register(String userName, String pwd);

    User login(String userName, String pwd);

    User findByUserName(String userName);

    List<User> findAll();

    User findById(Long id);

    boolean updateRole(Long id, String role);
}
