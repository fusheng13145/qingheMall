package com.qinghe.mall.dataobject;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * PaymentRecordDO.convertToModel() 分支覆盖补充（T3 续补）。
 * 覆盖 payType / payStatus 为 null 与非 null 两个 if 分支。
 */
class PaymentRecordDOTest {

    @Test
    void convert_nullPayTypeAndStatus_keepsNull() {
        PaymentRecordDO d = new PaymentRecordDO();
        d.setId("r1");
        d.setUserId(1L);
        d.setOrderNumber("N1");
        d.setAmount(new BigDecimal("10.00"));
        var m = d.convertToModel();
        assertNull(m.getPayType());
        assertNull(m.getPayStatus());
        assertEquals("r1", m.getId());
    }

    @Test
    void convert_withPayTypeAndStatus_mapsEnum() {
        PaymentRecordDO d = new PaymentRecordDO();
        d.setPayType("ALIPAY");
        d.setPayStatus("SUCCESS");
        var m = d.convertToModel();
        assertEquals(com.qinghe.mall.model.PayType.ALIPAY, m.getPayType());
        assertEquals(com.qinghe.mall.model.PaymentStatus.SUCCESS, m.getPayStatus());
    }
}
