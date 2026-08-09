package com.qinghe.mall.config;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qinghe.mall.service.OrderService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * 订单超时延迟队列 单元测试（P0-3 补齐：关单死信与重试）。
 *
 * 覆盖：关单成功路径、业务性状态变化跳过（不重试不告警）、
 * 系统级异常重试后成功、重试耗尽进入死信计数。
 */
class OrderTimeoutQueueTest {

    @Mock
    private OrderService orderService;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter deadLetterCounter;

    private OrderTimeoutQueue queue;

    private static final String ORDER_NUMBER = "O202608090001";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(meterRegistry.counter(anyString())).thenReturn(deadLetterCounter);
        queue = new OrderTimeoutQueue();
        java.lang.reflect.Field field;
        try {
            field = OrderTimeoutQueue.class.getDeclaredField("meterRegistry");
            field.setAccessible(true);
            field.set(queue, meterRegistry);
            field = OrderTimeoutQueue.class.getDeclaredField("orderService");
            field.setAccessible(true);
            field.set(queue, orderService);
            // init() 会启动真实消费线程，单测不调用；直接注入死信计数器
            field = OrderTimeoutQueue.class.getDeclaredField("deadLetterCounter");
            field.setAccessible(true);
            field.set(queue, deadLetterCounter);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void processOne_success_noRetry() {
        when(orderService.closeExpiredOrder(ORDER_NUMBER)).thenReturn(true);

        queue.processOne(ORDER_NUMBER);

        verify(orderService, times(1)).closeExpiredOrder(ORDER_NUMBER);
        verify(deadLetterCounter, times(0)).increment();
    }

    @Test
    void processOne_businessSkip_noRetryNoDeadLetter() {
        // 业务性异常（订单状态已变化）→ 幂等跳过，不重试、不计死信
        when(orderService.closeExpiredOrder(ORDER_NUMBER))
                .thenThrow(new RuntimeException("订单状态已变化，请刷新后重试"));

        queue.processOne(ORDER_NUMBER);

        verify(orderService, times(1)).closeExpiredOrder(ORDER_NUMBER);
        verify(deadLetterCounter, times(0)).increment();
    }

    @Test
    void processOne_systemError_retryThenSuccess() {
        // 系统级异常第 1 次失败、第 2 次成功 → 重试后成功，无死信
        when(orderService.closeExpiredOrder(ORDER_NUMBER))
                .thenThrow(new IllegalStateException("Communications link failure"))
                .thenReturn(true);

        queue.processOne(ORDER_NUMBER);

        verify(orderService, times(2)).closeExpiredOrder(ORDER_NUMBER);
        verify(deadLetterCounter, times(0)).increment();
    }

    @Test
    void processOne_systemError_retryExhausted_deadLetter() {
        // 系统级异常持续失败 → 重试 MAX_RETRY(3) 次后进入死信计数
        when(orderService.closeExpiredOrder(ORDER_NUMBER))
                .thenThrow(new IllegalStateException("Communications link failure"));

        queue.processOne(ORDER_NUMBER);

        verify(orderService, times(3)).closeExpiredOrder(ORDER_NUMBER);
        verify(deadLetterCounter, times(1)).increment();
    }

    @Test
    void processOne_nullBusinessMessage_treatedAsSystemError() {
        // 异常消息为 null（无法判定业务语义）→ 按系统级处理，重试至耗尽进入死信
        when(orderService.closeExpiredOrder(ORDER_NUMBER))
                .thenThrow(new RuntimeException());

        queue.processOne(ORDER_NUMBER);

        verify(orderService, times(3)).closeExpiredOrder(ORDER_NUMBER);
        verify(deadLetterCounter, times(1)).increment();
    }
}
