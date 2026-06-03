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

    int updateRole(@Param("id") Long id, @Param("role") String role);
}
