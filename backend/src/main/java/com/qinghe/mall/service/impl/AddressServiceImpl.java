package com.qinghe.mall.service.impl;

import com.qinghe.mall.exception.BusinessException;
import com.qinghe.mall.dao.AddressDAO;
import com.qinghe.mall.dataobject.AddressDO;
import com.qinghe.mall.model.Address;
import com.qinghe.mall.service.AddressService;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AddressServiceImpl implements AddressService {

    @Autowired
    private AddressDAO addressDAO;

    private void validate(Address address) {
        if (StringUtils.isBlank(address.getReceiverName())) {
            throw new BusinessException("请填写收货人");
        }
        if (StringUtils.isBlank(address.getReceiverPhone())) {
            throw new BusinessException("请填写联系电话");
        }
        if (!address.getReceiverPhone().matches("^1\\d{10}$")) {
            throw new BusinessException("联系电话格式不正确");
        }
        if (StringUtils.isBlank(address.getReceiverAddress())) {
            throw new BusinessException("请填写收货地址");
        }
    }

    @Override
    @Transactional
    public Address add(Long userId, Address address) {
        validate(address);
        boolean first = addressDAO.findByUserId(userId).isEmpty();
        boolean asDefault = (address.getIsDefault() != null && address.getIsDefault()) || first;
        if (asDefault) {
            addressDAO.clearDefaultByUserId(userId);
        }
        AddressDO addressDO = new AddressDO();
        addressDO.setUserId(userId);
        addressDO.setReceiverName(address.getReceiverName());
        addressDO.setReceiverPhone(address.getReceiverPhone());
        addressDO.setReceiverAddress(address.getReceiverAddress());
        addressDO.setIsDefault(asDefault ? 1 : 0);
        addressDO.setGmtCreated(new Date());
        addressDO.setGmtModified(new Date());
        addressDAO.insert(addressDO);
        return addressDO.convertToModel();
    }

    @Override
    @Transactional
    public Address update(Long userId, Address address) {
        if (address.getId() == null) {
            throw new BusinessException("地址ID不能为空");
        }
        AddressDO existing = addressDAO.findById(address.getId());
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw new BusinessException("地址不存在");
        }
        validate(address);
        boolean asDefault = address.getIsDefault() != null && address.getIsDefault();
        if (asDefault) {
            addressDAO.clearDefaultByUserId(userId);
        }
        AddressDO addressDO = new AddressDO();
        addressDO.setId(address.getId());
        addressDO.setUserId(userId);
        addressDO.setReceiverName(address.getReceiverName());
        addressDO.setReceiverPhone(address.getReceiverPhone());
        addressDO.setReceiverAddress(address.getReceiverAddress());
        addressDO.setIsDefault(asDefault ? 1 : null);
        addressDAO.update(addressDO);
        return addressDAO.findById(address.getId()).convertToModel();
    }

    @Override
    public boolean delete(Long userId, Long id) {
        AddressDO existing = addressDAO.findById(id);
        if (existing == null || !existing.getUserId().equals(userId)) {
            return false;
        }
        return addressDAO.deleteById(id) > 0;
    }

    @Override
    public boolean setDefault(Long userId, Long id) {
        AddressDO existing = addressDAO.findById(id);
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw new BusinessException("地址不存在");
        }
        addressDAO.clearDefaultByUserId(userId);
        return addressDAO.setDefault(id, userId) > 0;
    }

    @Override
    public List<Address> list(Long userId) {
        List<AddressDO> addressDOs = addressDAO.findByUserId(userId);
        List<Address> addresses = new ArrayList<>();
        for (AddressDO addressDO : addressDOs) {
            addresses.add(addressDO.convertToModel());
        }
        return addresses;
    }

    @Override
    public Address findDefault(Long userId) {
        AddressDO addressDO = addressDAO.findDefaultByUserId(userId);
        if (addressDO != null) {
            return addressDO.convertToModel();
        }
        List<AddressDO> all = addressDAO.findByUserId(userId);
        return all.isEmpty() ? null : all.get(0).convertToModel();
    }
}
