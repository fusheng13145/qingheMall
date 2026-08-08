package com.qinghe.mall.service;

import com.qinghe.mall.model.Address;
import java.util.List;

public interface AddressService {

    Address add(Long userId, Address address);

    Address update(Long userId, Address address);

    boolean delete(Long userId, Long id);

    boolean setDefault(Long userId, Long id);

    List<Address> list(Long userId);

    /** 获取默认地址（无则返回第一条，无地址返回 null） */
    Address findDefault(Long userId);
}
