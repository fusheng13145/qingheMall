package com.qinghe.mall.user.mapper;

import com.qinghe.mall.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户Mapper
 */
@Mapper
public interface UserMapper {

    /**
     * 根据用户名查询
     */
    User findByUsername(@Param("username") String username);

    /**
     * 根据ID查询
     */
    User findById(@Param("id") Long id);

    /**
     * 根据手机号查询
     */
    User findByPhone(@Param("phone") String phone);

    /**
     * 插入用户
     */
    int insert(User user);

    /**
     * 更新用户
     */
    int update(User user);

    /**
     * 更新密码
     */
    int updatePassword(@Param("id") Long id, @Param("password") String password);
}
