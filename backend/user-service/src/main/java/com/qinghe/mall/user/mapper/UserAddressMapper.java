package com.qinghe.mall.user.mapper;

import com.qinghe.mall.user.entity.UserAddress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户地址Mapper
 */
@Mapper
public interface UserAddressMapper {

    /**
     * 根据用户ID查询地址列表
     */
    List<UserAddress> findByUserId(@Param("userId") Long userId);

    /**
     * 根据ID查询
     */
    UserAddress findById(@Param("id") Long id);

    /**
     * 插入地址
     */
    int insert(UserAddress address);

    /**
     * 更新地址
     */
    int update(UserAddress address);

    /**
     * 删除地址
     */
    int deleteById(@Param("id") Long id);

    /**
     * 取消所有默认地址
     */
    int clearDefault(@Param("userId") Long userId);

    /**
     * 设置默认地址
     */
    int setDefault(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 查询用户默认地址
     */
    UserAddress findDefaultByUserId(@Param("userId") Long userId);
}
