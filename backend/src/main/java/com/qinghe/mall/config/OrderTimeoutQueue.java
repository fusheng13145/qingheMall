package com.qinghe.mall.config;

import com.qinghe.mall.service.OrderService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.PostConstruct;
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

    /** 系统级异常本地重试上限（P0-3：防瞬时故障导致漏关单） */
    private static final int MAX_RETRY = 3;

    /** 重试基础退避间隔（毫秒）：1s / 2s / 4s 指数退避 */
    private static final long RETRY_BASE_MS = 1000L;

    /** 死信计数指标（Prometheus：qinghe_order_timeout_deadletter_total，便于告警） */
    private static final String METRIC_DEAD_LETTER = "qinghe.order.timeout.deadletter";

    /**
     * 业务性异常特征消息（OrderServiceImpl.closeAndRestoreStock 抛出）：
     * 订单不存在 / 状态异常 / 状态已变化，属幂等跳过路径，重试无意义。
     * 之所以按消息特征而非异常类型区分：系统级故障（如 Spring DataAccessException、NPE）
     * 同为 RuntimeException 子类，无法仅凭类型判断。
     */
    private static final String[] BUSINESS_SKIP_MESSAGES = {
            "订单不存在", "订单状态异常", "订单状态已变化"
    };

    /** P2：消费线程数（多线程并行关单；关单 CAS 幂等保证并发安全） */
    private static final int CONSUMER_THREADS = 2;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    @Lazy
    private OrderService orderService;

    @Autowired
    private MeterRegistry meterRegistry;

    private RBlockingQueue<String> queue;
    private RDelayedQueue<String> delayedQueue;
    private Counter deadLetterCounter;

    @PostConstruct
    public void init() {
        queue = redissonClient.getBlockingQueue("order:timeout:queue");
        // 注册延迟队列（与阻塞队列同名关联）；客户端启动时会自动恢复 Redis 中未到期的调度
        delayedQueue = redissonClient.getDelayedQueue(queue);
        // ⚠️ 不可调用 delayedQueue.destroy()：会把未到期元素立即转移导致提前关单

        deadLetterCounter = meterRegistry.counter(METRIC_DEAD_LETTER);

        // P2：多线程消费（阻塞等待到期订单号；RBlockingQueue 多消费者安全）
        for (int i = 0; i < CONSUMER_THREADS; i++) {
            Thread consumer = new Thread(this::consume, "order-timeout-consumer-" + i);
            consumer.setDaemon(true);
            consumer.start();
        }
        log.info("订单超时延迟队列已启动，expireMinutes={}，consumerThreads={}", EXPIRE_MINUTES, CONSUMER_THREADS);
    }

    /** 下单成功后调用：订单号延迟 expireMinutes 后自动进入关单队列 */
    public void offer(String orderNumber) {
        if (orderNumber == null) {
            return;
        }
        delayedQueue.offer(orderNumber, EXPIRE_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * 消费线程主循环：仅负责从队列取订单号，单笔处理交给 {@link #processOne}。
     * P0-3 修复：取出的订单号在 processOne 内完成全部处理（重试/死信），
     * 杜绝原先「处理异常后订单号静默丢失」的问题。
     */
    private void consume() {
        while (!Thread.currentThread().isInterrupted()) {
            String orderNumber;
            try {
                orderNumber = queue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("延迟关单取队列异常，1s 后重试", e);
                sleepQuietly(1000);
                continue;
            }
            processOne(orderNumber);
        }
    }

    /**
     * 处理单笔到期订单。
     * - 业务性异常（订单不存在/状态异常/状态已变化）→ 幂等跳过，不重试不告警
     * - 系统级异常（DB 故障、序列化失败等）→ 本地指数退避重试 {@link #MAX_RETRY} 次，
     *   仍失败记死信指标并告警日志，由 {@link OrderTimeoutTask} 5 分钟兜底扫描补偿
     */
    void processOne(String orderNumber) {
        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                if (orderService.closeExpiredOrder(orderNumber)) {
                    log.info("延迟队列关单成功 orderNumber={}", orderNumber);
                }
                return;
            } catch (Exception e) {
                if (isBusinessSkip(e)) {
                    log.warn("延迟队列关单跳过 orderNumber={}, reason={}", orderNumber, e.getMessage());
                    return;
                }
                log.warn("延迟队列关单系统异常(第{}/{}次) orderNumber={}, reason={}",
                        attempt, MAX_RETRY, orderNumber, e.getMessage());
                if (attempt < MAX_RETRY) {
                    sleepQuietly(RETRY_BASE_MS * attempt);
                }
            }
        }
        log.error("延迟队列关单重试耗尽，进入死信 orderNumber={}（将由 5 分钟兜底任务补偿）", orderNumber);
        if (deadLetterCounter != null) {
            deadLetterCounter.increment();
        }
    }

    /** 业务性跳过判定：异常消息命中订单状态类语义则跳过（幂等路径） */
    private boolean isBusinessSkip(Throwable e) {
        String message = e.getMessage();
        if (message == null) {
            return false;
        }
        for (String businessMessage : BUSINESS_SKIP_MESSAGES) {
            if (message.contains(businessMessage)) {
                return true;
            }
        }
        return false;
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
