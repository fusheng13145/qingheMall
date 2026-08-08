package com.qinghe.mall.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qinghe.mall.dao.AddressDAO;
import com.qinghe.mall.dataobject.AddressDO;
import com.qinghe.mall.model.Address;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * 收货地址服务 单元测试（RR-4 测试广度补充）
 * 覆盖：入参校验、首条自动默认、显式默认切换、改/删/查与归属校验。
 */
class AddressServiceImplTest {

    @Mock
    private AddressDAO addressDAO;

    @InjectMocks
    private AddressServiceImpl addressService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private Address address(Long id, String name, String phone, String addr, Boolean isDefault) {
        Address a = new Address();
        a.setId(id);
        a.setReceiverName(name);
        a.setReceiverPhone(phone);
        a.setReceiverAddress(addr);
        a.setIsDefault(isDefault);
        return a;
    }

    private AddressDO addressDO(Long id, Long userId, String name, String phone, String addr, int isDefault) {
        AddressDO a = new AddressDO();
        a.setId(id);
        a.setUserId(userId);
        a.setReceiverName(name);
        a.setReceiverPhone(phone);
        a.setReceiverAddress(addr);
        a.setIsDefault(isDefault);
        a.setGmtCreated(new Date());
        a.setGmtModified(new Date());
        return a;
    }

    // ---------- 入参校验 ----------
    @Test
    void add_blankName_shouldThrow() {
        assertThrows(RuntimeException.class, () -> addressService.add(1L, address(null, "", "13800000000", "北京", null)));
    }

    @Test
    void add_blankPhone_shouldThrow() {
        assertThrows(RuntimeException.class, () -> addressService.add(1L, address(null, "张三", "", "北京", null)));
    }

    @Test
    void add_invalidPhone_shouldThrow() {
        assertThrows(RuntimeException.class, () -> addressService.add(1L, address(null, "张三", "123", "北京", null)));
    }

    @Test
    void add_blankAddress_shouldThrow() {
        assertThrows(RuntimeException.class, () -> addressService.add(1L, address(null, "张三", "13800000000", "", null)));
    }

    // ---------- 首条自动默认 ----------
    @Test
    void add_firstAddress_shouldBeDefault() {
        when(addressDAO.findByUserId(1L)).thenReturn(Collections.emptyList());
        when(addressDAO.insert(any(AddressDO.class))).thenReturn(1);

        Address result = addressService.add(1L, address(null, "张三", "13800000000", "北京", null));

        assertTrue(result.getIsDefault(), "首条地址应自动设为默认");
        // 因 asDefault=true，应先清除该用户所有默认标记
        verify(addressDAO).clearDefaultByUserId(1L);
    }

    // ---------- 非首条且不显式默认 ----------
    @Test
    void add_secondWithoutDefaultFlag_shouldNotBeDefault() {
        when(addressDAO.findByUserId(1L)).thenReturn(Collections.singletonList(addressDO(1L, 1L, "旧", "13800000000", "上海", 1)));
        when(addressDAO.insert(any(AddressDO.class))).thenReturn(1);

        Address result = addressService.add(1L, address(null, "张三", "13800000000", "北京", null));

        assertFalse(result.getIsDefault(), "未显式指定默认时不应成为默认");
        // 非默认，不应清除已有默认
        verify(addressDAO, never()).clearDefaultByUserId(anyLong());
    }

    // ---------- 显式指定默认 ----------
    @Test
    void add_explicitDefault_shouldClearOthers() {
        when(addressDAO.findByUserId(1L)).thenReturn(Collections.singletonList(addressDO(1L, 1L, "旧", "13800000000", "上海", 1)));
        when(addressDAO.insert(any(AddressDO.class))).thenReturn(1);

        Address result = addressService.add(1L, address(null, "张三", "13800000000", "北京", Boolean.TRUE));

        assertTrue(result.getIsDefault());
        verify(addressDAO).clearDefaultByUserId(1L);
    }

    // ---------- 更新 ----------
    @Test
    void update_nullId_shouldThrow() {
        assertThrows(RuntimeException.class, () -> addressService.update(1L, address(null, "张三", "13800000000", "北京", null)));
    }

    @Test
    void update_notExist_shouldThrow() {
        when(addressDAO.findById(9L)).thenReturn(null);
        assertThrows(RuntimeException.class, () -> addressService.update(1L, address(9L, "张三", "13800000000", "北京", null)));
    }

    @Test
    void update_notOwner_shouldThrow() {
        when(addressDAO.findById(9L)).thenReturn(addressDO(9L, 2L, "别人", "13800000000", "上海", 1));
        assertThrows(RuntimeException.class, () -> addressService.update(1L, address(9L, "张三", "13800000000", "北京", null)));
    }

    @Test
    void update_success() {
        when(addressDAO.findById(9L)).thenReturn(addressDO(9L, 1L, "旧", "13800000000", "上海", 1));
        when(addressDAO.update(any(AddressDO.class))).thenReturn(1);
        when(addressDAO.findById(9L)).thenReturn(addressDO(9L, 1L, "张三", "13800000000", "北京", 0));

        Address result = addressService.update(1L, address(9L, "张三", "13800000000", "北京", null));
        assertEquals("张三", result.getReceiverName());
        verify(addressDAO).update(any(AddressDO.class));
    }

    // ---------- 删除 ----------
    @Test
    void delete_notExistOrNotOwner_shouldReturnFalse() {
        when(addressDAO.findById(9L)).thenReturn(null);
        assertFalse(addressService.delete(1L, 9L));
        when(addressDAO.findById(9L)).thenReturn(addressDO(9L, 2L, "别人", "13800000000", "上海", 1));
        assertFalse(addressService.delete(1L, 9L));
    }

    @Test
    void delete_success() {
        when(addressDAO.findById(9L)).thenReturn(addressDO(9L, 1L, "张三", "13800000000", "北京", 1));
        when(addressDAO.deleteById(9L)).thenReturn(1);
        assertTrue(addressService.delete(1L, 9L));
    }

    // ---------- 设为默认 ----------
    @Test
    void setDefault_notExist_shouldThrow() {
        when(addressDAO.findById(9L)).thenReturn(null);
        assertThrows(RuntimeException.class, () -> addressService.setDefault(1L, 9L));
    }

    @Test
    void setDefault_success() {
        when(addressDAO.findById(9L)).thenReturn(addressDO(9L, 1L, "张三", "13800000000", "北京", 0));
        when(addressDAO.setDefault(9L, 1L)).thenReturn(1);
        assertTrue(addressService.setDefault(1L, 9L));
        verify(addressDAO).clearDefaultByUserId(1L);
    }

    // ---------- 列表与默认查询 ----------
    @Test
    void list_shouldReturnModelList() {
        when(addressDAO.findByUserId(1L)).thenReturn(Arrays.asList(
                addressDO(1L, 1L, "张三", "13800000000", "北京", 1),
                addressDO(2L, 1L, "李四", "13900000000", "上海", 0)));
        List<Address> list = addressService.list(1L);
        assertEquals(2, list.size());
        assertTrue(list.get(0).getIsDefault());
    }

    @Test
    void findDefault_hasDefault_shouldReturnIt() {
        when(addressDAO.findDefaultByUserId(1L)).thenReturn(addressDO(1L, 1L, "张三", "13800000000", "北京", 1));
        Address def = addressService.findDefault(1L);
        assertTrue(def.getIsDefault());
    }

    @Test
    void findDefault_noDefault_shouldReturnFirst() {
        when(addressDAO.findDefaultByUserId(1L)).thenReturn(null);
        when(addressDAO.findByUserId(1L)).thenReturn(Collections.singletonList(addressDO(2L, 1L, "李四", "13900000000", "上海", 0)));
        Address def = addressService.findDefault(1L);
        assertEquals("李四", def.getReceiverName());
    }

    @Test
    void findDefault_empty_shouldReturnNull() {
        when(addressDAO.findDefaultByUserId(1L)).thenReturn(null);
        when(addressDAO.findByUserId(1L)).thenReturn(Collections.emptyList());
        assertNull(addressService.findDefault(1L));
    }
}
