package com.qinghe.mall.config;

import com.qinghe.mall.service.OrderService;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * 订单超时关单 - 延迟队列实现（P2-1 消息队列解耦）。
 *
 * 基于 Redisson RDelayedQueue（Redis ZSET + 客户端定时转移，无需 Redis 额外配置）：
 * - 下单成功后 {@link #offer(String)} 将订单号入队，延迟 expireMinutes 分钟
 * - 到期后订单号自动转入阻塞队列，由后台消费线程取出并执行关单（回滚库存）
 * - 订单号存于 Redis，应用重启不丢失；同队列支持多实例消费（分布式锁保证幂等）
 *
 * 说明：与 {@link OrderTimeoutTask} 低频轮询互为兜底（轮询间隔放宽到 5 分钟），
 * 主路径由延迟队列驱动，消除「最长 60 秒延迟」。
 */
@Component
public class OrderTimeoutQueue {

    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutQueue.class);

    /** 超时分钟数（与 OrderTimeoutTask 一致，可通过系统属性覆盖以便测试） */
    private static final int EXPIRE_MINUTES =
            Integer.parseInt(System.getProperty("qinghe.order.expire-minutes", "30"));

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    @Lazy
    private OrderService orderService;

    private RBlockingQueue<String> queue;
    private RDelayedQueue<String> delayedQueue;

    @PostConstruct
    public void init() {
        queue = redissonClient.getBlockingQueue("order:timeout:queue");
        // 注册延迟队列（与阻塞队列同名关联）；客户端启动时会自动恢复 Redis 中未到期的调度
        delayedQueue = redissonClient.getDelayedQueue(queue);
        // ⚠️ 不可调用 delayedQueue.destroy()：会把未到期元素立即转移导致提前关单

        // 后台消费线程：阻塞等待到期订单号
        Thread consumer = new Thread(this::consume, "order-timeout-consumer");
        consumer.setDaemon(true);
        consumer.start();
        log.info("订单超时延迟队列已启动，expireMinutes={}", EXPIRE_MINUTES);
    }

    /** 下单成功后调用：订单号延迟 expireMinutes 后自动进入关单队列 */
    public void offer(String orderNumber) {
        if (orderNumber == null) {
            return;
        }
        delayedQueue.offer(orderNumber, EXPIRE_MINUTES, TimeUnit.MINUTES);
    }

    private void consume() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                String orderNumber = queue.take();
                try {
                    if (orderService.closeExpiredOrder(orderNumber)) {
                        log.info("延迟队列关单成功 orderNumber={}", orderNumber);
                    }
                } catch (RuntimeException e) {
                    // 多为订单状态已变化（已支付/已取消），忽略
                    log.warn("延迟队列关单跳过 orderNumber={}, reason={}", orderNumber, e.getMessage());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("延迟关单消费异常，1s 后重试", e);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
}
