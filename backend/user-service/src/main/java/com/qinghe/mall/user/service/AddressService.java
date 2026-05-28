package com.qinghe.mall.user.service;

import com.qinghe.mall.user.dto.AddressDTO;
import com.qinghe.mall.user.dto.AddressVO;
import com.qinghe.mall.user.entity.UserAddress;

import java.util.List;

/**
 * 地址服务接口
 */
public interface AddressService {

    /**
     * 获取用户地址列表
     */
    List<AddressVO> getAddressList(Long userId);

    /**
     * 添加收货地址
     */
    void addAddress(Long userId, AddressDTO addressDTO);

    /**
     * 更新收货地址
     */
    void updateAddress(Long userId, AddressDTO addressDTO);

    /**
     * 删除收货地址
     */
    void deleteAddress(Long userId, Long addressId);

    /**
     * 设置默认地址
     */
    void setDefaultAddress(Long userId, Long addressId);

    /**
     * 根据ID查询地址
     */
    UserAddress getAddressById(Long addressId);
}
