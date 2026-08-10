package com.qinghe.mall.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qinghe.mall.dao.PaymentRecordDAO;
import com.qinghe.mall.dataobject.PaymentRecordDO;
import com.qinghe.mall.model.PaymentRecord;
import com.qinghe.mall.model.PayType;
import com.qinghe.mall.model.PaymentStatus;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 支付流水服务单元测试（原 4% 覆盖，补齐）。
 *
 * 覆盖：insert 默认值填充（id/时间自动生成）、findByOrderNumber 存在/缺失、
 * findByPayStatus 转换、updatePayStatus 成败透传。
 */
@ExtendWith(MockitoExtension.class)
class PaymentRecordServiceImplTest {

    @Mock
    private PaymentRecordDAO paymentRecordDAO;

    @InjectMocks
    private PaymentRecordServiceImpl service;

    private PaymentRecord record;

    @BeforeEach
    void setUp() {
        record = new PaymentRecord();
        record.setUserId(1L);
        record.setOrderNumber("QH20260810001");
        record.setChannelPaymentId("wx-4200001234");
        record.setChannelType("WECHAT");
        record.setAmount(new BigDecimal("199.00"));
        record.setPayType(PayType.WEIXIN);
        record.setPayStatus(PaymentStatus.SUCCESS);
    }

    @Test
    @DisplayName("insert 填充默认 id 与时间并落库")
    void insert_fillsDefaults() {
        when(paymentRecordDAO.insert(any(PaymentRecordDO.class))).thenReturn(1);

        int result = service.insert(record);

        assertEquals(1, result);
        verify(paymentRecordDAO).insert(org.mockito.ArgumentMatchers.argThat(do_ ->
                do_.getId() != null
                        && do_.getPayType().equals("WEIXIN")
                        && do_.getPayStatus().equals("SUCCESS")
                        && do_.getGmtCreated() != null
                        && do_.getGmtModified() != null));
    }

    @Test
    @DisplayName("insert 显式 id 保留")
    void insert_keepsExplicitId() {
        when(paymentRecordDAO.insert(any(PaymentRecordDO.class))).thenReturn(1);
        record.setId("PR-EXPLICIT");

        service.insert(record);

        verify(paymentRecordDAO).insert(org.mockito.ArgumentMatchers.argThat(do_ ->
                "PR-EXPLICIT".equals(do_.getId())));
    }

    @Test
    @DisplayName("findByOrderNumber 存在时转换模型")
    void findByOrderNumber_found() {
        PaymentRecordDO do_ = new PaymentRecordDO();
        do_.setId("PR1");
        do_.setOrderNumber("QH1");
        do_.setPayStatus("SUCCESS");
        when(paymentRecordDAO.findByOrderNumber("QH1")).thenReturn(do_);

        PaymentRecord result = service.findByOrderNumber("QH1");

        assertNotNull(result);
        assertEquals("PR1", result.getId());
        assertEquals("QH1", result.getOrderNumber());
    }

    @Test
    @DisplayName("findByOrderNumber 不存在返回 null")
    void findByOrderNumber_missing() {
        when(paymentRecordDAO.findByOrderNumber("QH-X")).thenReturn(null);
        assertNull(service.findByOrderNumber("QH-X"));
    }

    @Test
    @DisplayName("findByPayStatus 转换列表")
    void findByPayStatus_converts() {
        PaymentRecordDO do_ = new PaymentRecordDO();
        do_.setId("PR1");
        do_.setPayStatus("SUCCESS");
        when(paymentRecordDAO.findByPayStatus("SUCCESS")).thenReturn(List.of(do_));

        List<PaymentRecord> records = service.findByPayStatus("SUCCESS");

        assertEquals(1, records.size());
        assertEquals("PR1", records.get(0).getId());
    }

    @Test
    @DisplayName("updatePayStatus 成功返回 true")
    void updatePayStatus_success() {
        when(paymentRecordDAO.updatePayStatus("QH1", "SUCCESS", "wx-1")).thenReturn(1);
        assertTrue(service.updatePayStatus("QH1", "SUCCESS", "wx-1"));
    }

    @Test
    @DisplayName("updatePayStatus 未命中返回 false")
    void updatePayStatus_noMatch() {
        when(paymentRecordDAO.updatePayStatus("QH1", "SUCCESS", "wx-1")).thenReturn(0);
        assertFalse(service.updatePayStatus("QH1", "SUCCESS", "wx-1"));
    }
}
