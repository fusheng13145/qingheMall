package com.qinghe.mall.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.qinghe.mall.dao.OrderDAO;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 用户偏好画像服务单元测试（v1.13）：品牌偏好/已购集合的透传与空值防御。
 */
@ExtendWith(MockitoExtension.class)
class UserAffinityServiceImplTest {

    @Mock
    private OrderDAO orderDAO;

    @InjectMocks
    private UserAffinityServiceImpl service;

    @Test
    @DisplayName("品牌偏好：聚合行转品牌串，未登录返回空")
    void preferredBrands_passthroughAndNull() {
        Map<String, Object> row = new HashMap<>();
        row.put("brand", "B");
        row.put("orderCount", 4);
        when(orderDAO.brandAffinity(9L, 5)).thenReturn(List.of(row));

        assertEquals(List.of("B"), service.preferredBrands(9L, 5));
        assertTrue(service.preferredBrands(null, 5).isEmpty());
        verifyOrderDaoNeverForNull();
    }

    @Test
    @DisplayName("已购集合：DAO 结果转不可变 Set，未登录返回空")
    void purchasedProductIds_passthroughAndNull() {
        when(orderDAO.findPurchasedProductIds(9L)).thenReturn(List.of("p1", "p2"));

        assertEquals(java.util.Set.of("p1", "p2"), service.purchasedProductIds(9L));
        assertTrue(service.purchasedProductIds(null).isEmpty());
    }

    @Test
    @DisplayName("DAO 返回 null 的防御")
    void daoNull_returnsEmpty() {
        when(orderDAO.brandAffinity(9L, 5)).thenReturn(null);
        when(orderDAO.findPurchasedProductIds(9L)).thenReturn(null);

        assertTrue(service.preferredBrands(9L, 5).isEmpty());
        assertTrue(service.purchasedProductIds(9L).isEmpty());
    }

    private void verifyOrderDaoNeverForNull() {
        org.mockito.Mockito.verify(orderDAO, org.mockito.Mockito.never())
                .brandAffinity(org.mockito.ArgumentMatchers.isNull(),
                        org.mockito.ArgumentMatchers.anyInt());
    }
}
