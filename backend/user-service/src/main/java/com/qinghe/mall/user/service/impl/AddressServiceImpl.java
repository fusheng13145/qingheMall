package com.qinghe.mall.user.service.impl;

import com.qinghe.mall.user.dto.AddressDTO;
import com.qinghe.mall.user.dto.AddressVO;
import com.qinghe.mall.user.entity.UserAddress;
import com.qinghe.mall.user.mapper.UserAddressMapper;
import com.qinghe.mall.user.service.AddressService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 地址服务实现类
 */
@Service
public class AddressServiceImpl implements AddressService {

    @Autowired
    private UserAddressMapper addressMapper;

    @Override
    public List<AddressVO> getAddressList(Long userId) {
        List<UserAddress> addresses = addressMapper.findByUserId(userId);
        return addresses.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public void addAddress(Long userId, AddressDTO addressDTO) {
        UserAddress address = new UserAddress();
        BeanUtils.copyProperties(addressDTO, address);
        address.setUserId(userId);
        address.setCreateTime(LocalDateTime.now());
        address.setUpdateTime(LocalDateTime.now());

        // 如果设置为默认地址，先取消其他默认
        if (addressDTO.getIsDefault() != null && addressDTO.getIsDefault() == 1) {
            addressMapper.clearDefault(userId);
            address.setIsDefault(1);
        } else {
            address.setIsDefault(0);
        }

        addressMapper.insert(address);
    }

    @Override
    public void updateAddress(Long userId, AddressDTO addressDTO) {
        UserAddress existAddress = addressMapper.findById(addressDTO.getId());
        if (existAddress == null || !existAddress.getUserId().equals(userId)) {
            throw new RuntimeException("地址不存在");
        }

        UserAddress address = new UserAddress();
        BeanUtils.copyProperties(addressDTO, address);
        address.setUserId(userId);
        address.setUpdateTime(LocalDateTime.now());

        // 如果设置为默认地址，先取消其他默认
        if (addressDTO.getIsDefault() != null && addressDTO.getIsDefault() == 1) {
            addressMapper.clearDefault(userId);
            address.setIsDefault(1);
        }

        addressMapper.update(address);
    }

    @Override
    public void deleteAddress(Long userId, Long addressId) {
        UserAddress existAddress = addressMapper.findById(addressId);
        if (existAddress == null || !existAddress.getUserId().equals(userId)) {
            throw new RuntimeException("地址不存在");
        }
        addressMapper.deleteById(addressId);
    }

    @Override
    public void setDefaultAddress(Long userId, Long addressId) {
        UserAddress existAddress = addressMapper.findById(addressId);
        if (existAddress == null || !existAddress.getUserId().equals(userId)) {
            throw new RuntimeException("地址不存在");
        }

        addressMapper.clearDefault(userId);
        addressMapper.setDefault(addressId, userId);
    }

    @Override
    public UserAddress getAddressById(Long addressId) {
        return addressMapper.findById(addressId);
    }

    private AddressVO convertToVO(UserAddress address) {
        AddressVO vo = new AddressVO();
        BeanUtils.copyProperties(address, vo);
        return vo;
    }
}
