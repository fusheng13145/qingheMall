package com.qinghe.mall.dao;

import com.qinghe.mall.dataobject.UserDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserDAO {

    UserDO findByUserName(@Param("userName") String userName);

    int insert(UserDO userDO);

    List<UserDO> findAll();

    UserDO findById(@Param("id") Long id);

    /** 批量查询用户（订单列表消除 N+1） */
    List<UserDO> findByIds(@Param("ids") List<Long> ids);

    /** 用户总数（看板聚合） */
    long countAll();

    int updateRole(@Param("id") Long id, @Param("role") String role);

    /** 更新个人资料（昵称/头像） */
    int updateProfile(@Param("id") Long id, @Param("nickName") String nickName, @Param("avatar") String avatar);

    /** 更新密码哈希（存量 MD5 用户登录成功后升级为 BCrypt） */
    int updatePwd(@Param("id") Long id, @Param("pwd") String pwd);

    /** 按 ID 删除（仅测试数据清理使用） */
    int deleteByIdForTest(@Param("id") Long id);
}
