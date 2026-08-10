package com.qinghe.mall.config;

import com.qinghe.mall.model.Order;
import com.qinghe.mall.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 超时关单兜底任务测试（原零覆盖）：
 * 空结果短路 / 逐笔关闭统计 / 单笔失败不中断 / 查询异常被吞不抛。
 */
class OrderTimeoutTaskTest {

    private OrderService orderService;
    private OrderTimeoutTask task;

    @BeforeEach
    void setUp() {
        orderService = mock(OrderService.class);
        task = new OrderTimeoutTask();
        // 直接注入 mock（OrderTimeoutTask 字段为包内可见的 @Autowired 私有字段，经反射注入）
        org.springframework.test.util.ReflectionTestUtils.setField(task, "orderService", orderService);
    }

    private Order order(String orderNumber) {
        Order o = new Order();
        o.setOrderNumber(orderNumber);
        return o;
    }

    @Test
    @DisplayName("无过期订单时直接返回，不调用关单")
    void emptyResultShortCircuits() {
        when(orderService.findExpiredWaitPay(org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(Collections.emptyList());

        task.closeTimeoutOrders();

        verify(orderService, never()).closeExpiredOrder(anyString());
    }

    @Test
    @DisplayName("有过期订单时逐笔关闭并计数")
    void closesEachExpiredOrder() {
        List<Order> expired = new ArrayList<>();
        expired.add(order("O1001"));
        expired.add(order("O1002"));
        expired.add(order("O1003"));
        when(orderService.findExpiredWaitPay(org.mockito.ArgumentMatchers.anyInt())).thenReturn(expired);
        when(orderService.closeExpiredOrder("O1001")).thenReturn(true);
        when(orderService.closeExpiredOrder("O1002")).thenReturn(false);
        when(orderService.closeExpiredOrder("O1003")).thenReturn(true);

        task.closeTimeoutOrders();

        verify(orderService, times(3)).closeExpiredOrder(anyString());
    }

    @Test
    @DisplayName("单笔关单抛异常不中断其余订单")
    void singleFailureDoesNotAbort() {
        List<Order> expired = new ArrayList<>();
        expired.add(order("O1001"));
        expired.add(order("O1002"));
        when(orderService.findExpiredWaitPay(org.mockito.ArgumentMatchers.anyInt())).thenReturn(expired);
        when(orderService.closeExpiredOrder("O1001")).thenThrow(new RuntimeException("状态已变化"));
        when(orderService.closeExpiredOrder("O1002")).thenReturn(true);

        assertThatCode(() -> task.closeTimeoutOrders()).doesNotThrowAnyException();

        verify(orderService, times(1)).closeExpiredOrder("O1002");
    }

    @Test
    @DisplayName("查询异常被吞掉不向上抛（兜底任务自愈）")
    void queryExceptionSwallowed() {
        when(orderService.findExpiredWaitPay(org.mockito.ArgumentMatchers.anyInt()))
                .thenThrow(new RuntimeException("DB down"));

        assertThatCode(() -> task.closeTimeoutOrders()).doesNotThrowAnyException();
    }
}
