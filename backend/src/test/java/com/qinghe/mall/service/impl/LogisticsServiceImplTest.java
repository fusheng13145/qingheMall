package com.qinghe.mall.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qinghe.mall.dao.LogisticsDAO;
import com.qinghe.mall.dao.LogisticsTraceDAO;
import com.qinghe.mall.dao.OrderDAO;
import com.qinghe.mall.dataobject.LogisticsDO;
import com.qinghe.mall.dataobject.LogisticsTraceDO;
import com.qinghe.mall.dataobject.OrderDO;
import com.qinghe.mall.exception.BusinessException;
import com.qinghe.mall.model.Logistics;
import com.qinghe.mall.service.OrderService;
import java.util.Collections;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 物流服务 单元测试（P2-18）。
 * 覆盖：发货建档原子性、状态管道推进、轨迹留痕、终态与并发守卫。
 */
class LogisticsServiceImplTest {

    @Mock
    private LogisticsDAO logisticsDAO;

    @Mock
    private LogisticsTraceDAO logisticsTraceDAO;

    @Mock
    private OrderDAO orderDAO;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private LogisticsServiceImpl logisticsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // 事务模板：直接执行回调（模拟事务提交成功）
        org.springframework.test.util.ReflectionTestUtils.setField(logisticsService, "transactionTemplate",
                new TransactionTemplate() {
                    @Override
                    public <T> T execute(TransactionCallback<T> action) {
                        return action.doInTransaction(null);
                    }
                });
    }

    private LogisticsDO logistics(String orderNumber, String status) {
        LogisticsDO logisticsDO = new LogisticsDO();
        logisticsDO.setId("lg1");
        logisticsDO.setOrderNumber(orderNumber);
        logisticsDO.setCompany("顺丰速运");
        logisticsDO.setTrackingNumber("SF123456");
        logisticsDO.setStatus(status);
        logisticsDO.setGmtCreated(new Date());
        return logisticsDO;
    }

    // ========== 发货 ==========

    @Test
    void ship_success_insertsLogisticsAndFirstTrace() {
        OrderDO od = new OrderDO();
        od.setOrderNumber("O1");
        when(orderDAO.findByOrderNumber("O1")).thenReturn(od);
        when(logisticsDAO.findByOrderNumber("O1")).thenReturn(null)
                .thenReturn(logistics("O1", LogisticsDO.STATUS_SHIPPED));
        when(logisticsTraceDAO.findByOrderNumber("O1")).thenReturn(Collections.emptyList());

        Logistics result = logisticsService.ship("O1", " 顺丰速运 ", " SF123456 ");

        assertNotNull(result);
        verify(orderService).shipOrder("O1");
        // 物流档案：字段去空白、初始状态 SHIPPED
        ArgumentCaptor<LogisticsDO> captor = ArgumentCaptor.forClass(LogisticsDO.class);
        verify(logisticsDAO).insert(captor.capture());
        assertEquals("顺丰速运", captor.getValue().getCompany());
        assertEquals("SF123456", captor.getValue().getTrackingNumber());
        assertEquals(LogisticsDO.STATUS_SHIPPED, captor.getValue().getStatus());
        // 首条轨迹留痕（含承运商与运单号）
        ArgumentCaptor<LogisticsTraceDO> traceCaptor = ArgumentCaptor.forClass(LogisticsTraceDO.class);
        verify(logisticsTraceDAO).insert(traceCaptor.capture());
        assertTrue(traceCaptor.getValue().getDescription().contains("顺丰速运"));
        assertTrue(traceCaptor.getValue().getDescription().contains("SF123456"));
        assertEquals(LogisticsDO.STATUS_SHIPPED, result.getStatus());
    }

    @Test
    void ship_orderStatusInvalid_propagatesAndNoInsert() {
        OrderDO od = new OrderDO();
        od.setOrderNumber("O1");
        when(orderDAO.findByOrderNumber("O1")).thenReturn(od);
        when(orderService.shipOrder("O1"))
                .thenThrow(new BusinessException("订单状态异常，无法发货（仅已付款订单可发货）"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> logisticsService.ship("O1", "顺丰", "SF1"));
        assertTrue(ex.getMessage().contains("订单状态异常"));
        verify(logisticsDAO, never()).insert(any());
        verify(logisticsTraceDAO, never()).insert(any());
    }

    @Test
    void ship_blankCompany_throws() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> logisticsService.ship("O1", "  ", "SF1"));
        assertEquals("请填写承运商与运单号", ex.getMessage());
        verify(orderService, never()).shipOrder(anyString());
    }

    @Test
    void ship_blankTrackingNumber_throws() {
        assertThrows(BusinessException.class, () -> logisticsService.ship("O1", "顺丰", ""));
    }

    @Test
    void ship_tooLongFields_throws() {
        String tooLong = "A".repeat(65);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> logisticsService.ship("O1", tooLong, "SF1"));
        assertTrue(ex.getMessage().contains("过长"));
    }

    @Test
    void ship_orderNotFound_throws() {
        when(orderDAO.findByOrderNumber("O1")).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> logisticsService.ship("O1", "顺丰", "SF1"));
        assertEquals("订单不存在", ex.getMessage());
    }

    @Test
    void ship_duplicateLogistics_throws() {
        OrderDO od = new OrderDO();
        od.setOrderNumber("O1");
        when(orderDAO.findByOrderNumber("O1")).thenReturn(od);
        when(logisticsDAO.findByOrderNumber("O1")).thenReturn(logistics("O1", LogisticsDO.STATUS_SHIPPED));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> logisticsService.ship("O1", "顺丰", "SF1"));
        assertEquals("该订单已录入物流信息，请勿重复发货", ex.getMessage());
        verify(orderService, never()).shipOrder(anyString());
    }

    @Test
    void ship_blankOrderNumber_throws() {
        assertThrows(IllegalArgumentException.class, () -> logisticsService.ship("", "顺丰", "SF1"));
    }

    // ========== 推进 ==========

    @Test
    void advance_shippedToInTransit_appendsTrace() {
        when(logisticsDAO.findByOrderNumber("O1"))
                .thenReturn(logistics("O1", LogisticsDO.STATUS_SHIPPED))
                .thenReturn(logistics("O1", LogisticsDO.STATUS_IN_TRANSIT));
        when(logisticsDAO.updateStatusWithGuard("O1",
                LogisticsDO.STATUS_SHIPPED, LogisticsDO.STATUS_IN_TRANSIT)).thenReturn(1);
        when(logisticsTraceDAO.findByOrderNumber("O1")).thenReturn(Collections.emptyList());

        Logistics result = logisticsService.advance("O1");

        assertEquals(LogisticsDO.STATUS_IN_TRANSIT, result.getStatus());
        ArgumentCaptor<LogisticsTraceDO> captor = ArgumentCaptor.forClass(LogisticsTraceDO.class);
        verify(logisticsTraceDAO).insert(captor.capture());
        assertEquals("包裹运输中，正发往收货地址", captor.getValue().getDescription());
    }

    @Test
    void advance_inTransitToDelivering() {
        when(logisticsDAO.findByOrderNumber("O1"))
                .thenReturn(logistics("O1", LogisticsDO.STATUS_IN_TRANSIT))
                .thenReturn(logistics("O1", LogisticsDO.STATUS_DELIVERING));
        when(logisticsDAO.updateStatusWithGuard("O1",
                LogisticsDO.STATUS_IN_TRANSIT, LogisticsDO.STATUS_DELIVERING)).thenReturn(1);
        when(logisticsTraceDAO.findByOrderNumber("O1")).thenReturn(Collections.emptyList());

        Logistics result = logisticsService.advance("O1");

        assertEquals(LogisticsDO.STATUS_DELIVERING, result.getStatus());
    }

    @Test
    void advance_deliveringToSigned() {
        when(logisticsDAO.findByOrderNumber("O1"))
                .thenReturn(logistics("O1", LogisticsDO.STATUS_DELIVERING))
                .thenReturn(logistics("O1", LogisticsDO.STATUS_SIGNED));
        when(logisticsDAO.updateStatusWithGuard("O1",
                LogisticsDO.STATUS_DELIVERING, LogisticsDO.STATUS_SIGNED)).thenReturn(1);
        when(logisticsTraceDAO.findByOrderNumber("O1")).thenReturn(Collections.emptyList());

        Logistics result = logisticsService.advance("O1");

        assertEquals(LogisticsDO.STATUS_SIGNED, result.getStatus());
    }

    @Test
    void advance_signedTerminal_throws() {
        when(logisticsDAO.findByOrderNumber("O1")).thenReturn(logistics("O1", LogisticsDO.STATUS_SIGNED));

        BusinessException ex = assertThrows(BusinessException.class, () -> logisticsService.advance("O1"));
        assertEquals("物流已签收，无需继续推进", ex.getMessage());
        verify(logisticsDAO, never()).updateStatusWithGuard(anyString(), anyString(), anyString());
    }

    @Test
    void advance_noLogistics_throws() {
        when(logisticsDAO.findByOrderNumber("O1")).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> logisticsService.advance("O1"));
        assertEquals("该订单暂无物流信息", ex.getMessage());
    }

    @Test
    void advance_casRace_throwsAndNoTrace() {
        when(logisticsDAO.findByOrderNumber("O1")).thenReturn(logistics("O1", LogisticsDO.STATUS_SHIPPED));
        when(logisticsDAO.updateStatusWithGuard(anyString(), anyString(), anyString())).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class, () -> logisticsService.advance("O1"));
        assertEquals("物流状态已变化，请刷新后重试", ex.getMessage());
        verify(logisticsTraceDAO, never()).insert(any());
    }

    @Test
    void advance_blankOrderNumber_throws() {
        assertThrows(IllegalArgumentException.class, () -> logisticsService.advance(" "));
    }

    // ========== 查询 ==========

    @Test
    void track_buildsTimelineInOrder() {
        LogisticsDO logisticsDO = logistics("O1", LogisticsDO.STATUS_IN_TRANSIT);
        when(logisticsDAO.findByOrderNumber("O1")).thenReturn(logisticsDO);
        LogisticsTraceDO t1 = new LogisticsTraceDO();
        t1.setDescription("商家已发货");
        t1.setTraceTime(new Date(1000L));
        LogisticsTraceDO t2 = new LogisticsTraceDO();
        t2.setDescription("包裹运输中");
        t2.setTraceTime(new Date(2000L));
        when(logisticsTraceDAO.findByOrderNumber("O1")).thenReturn(java.util.List.of(t1, t2));

        Logistics result = logisticsService.track("O1");

        assertNotNull(result);
        assertEquals("顺丰速运", result.getCompany());
        assertEquals("SF123456", result.getTrackingNumber());
        assertEquals(2, result.getTraces().size());
        assertEquals("商家已发货", result.getTraces().get(0).getDescription());
    }

    @Test
    void track_noLogistics_returnsNull() {
        when(logisticsDAO.findByOrderNumber("O1")).thenReturn(null);
        assertNull(logisticsService.track("O1"));
    }

    @Test
    void track_blankOrderNumber_throws() {
        assertThrows(IllegalArgumentException.class, () -> logisticsService.track(null));
    }
}
