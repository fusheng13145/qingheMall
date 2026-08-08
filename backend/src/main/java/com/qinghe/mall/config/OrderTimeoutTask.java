package com.qinghe.mall.config;

import com.qinghe.mall.model.Order;
import com.qinghe.mall.service.OrderService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 订单超时自动关单 - 兜底轮询（P2-1）。
 *
 * 主路径由 {@link OrderTimeoutQueue} 延迟队列驱动（下单即入队、到期即关单），
 * 本任务降为 5 分钟低频兜底扫描，覆盖延迟队列消息丢失/Redis 异常等极端情况。
 *
 * 说明：演示环境默认 30 分钟；生产可用系统属性/配置覆盖：
 *   -Dqinghe.order.expire-minutes=30
 */
@Component
public class OrderTimeoutTask {

    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutTask.class);

    /** 超时分钟数（演示环境 30 分钟，可通过系统属性覆盖以便测试） */
    private static final int EXPIRE_MINUTES =
            Integer.parseInt(System.getProperty("qinghe.order.expire-minutes", "30"));

    @Autowired
    private OrderService orderService;

    /** 每 5 分钟扫描一次（兜底；主路径为延迟队列） */
    @Scheduled(fixedDelay = 300_000)
    public void closeTimeoutOrders() {
        try {
            List<Order> expired = orderService.findExpiredWaitPay(EXPIRE_MINUTES);
            if (expired.isEmpty()) {
                return;
            }
            int closed = 0;
            for (Order order : expired) {
                try {
                    if (orderService.closeExpiredOrder(order.getOrderNumber())) {
                        closed++;
                    }
                } catch (RuntimeException e) {
                    // 单笔失败不中断整体扫描（多为状态已变化）
                    log.warn("关闭超时订单失败 orderNumber={}, reason={}", order.getOrderNumber(), e.getMessage());
                }
            }
            log.info("超时关单任务完成：扫描 {} 笔，关闭 {} 笔", expired.size(), closed);
        } catch (Exception e) {
            log.error("超时关单任务异常", e);
        }
    }
}
