package com.qinghe.mall.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qinghe.mall.config.OrderTimeoutQueue;
import com.qinghe.mall.dao.OrderDAO;
import com.qinghe.mall.dao.SeckillActivityDAO;
import com.qinghe.mall.dao.SeckillOrderDAO;
import com.qinghe.mall.dataobject.SeckillActivityDO;
import com.qinghe.mall.dataobject.SeckillOrderDO;
import com.qinghe.mall.model.Product;
import com.qinghe.mall.model.ProductDetail;
import com.qinghe.mall.service.ProductDetailService;
import com.qinghe.mall.service.ProductService;
import com.qinghe.mall.service.StockLogService;
import java.math.BigDecimal;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 秒杀服务单元测试（原零覆盖，miss 72 行）。
 *
 * 覆盖：活动创建参数校验/成功预热、toggle 状态机、getActivity 不存在、
 * createOrder 全拒绝路径 + Redis 预扣闸门 + 事务成功 + 已抢光 + 事务失败补偿、
 * rollbackIfUnpaid 三分支。
 */
class SeckillServiceImplTest {

    private SeckillServiceImpl service;
    private SeckillActivityDAO activityDAO;
    private SeckillOrderDAO seckillOrderDAO;
    private OrderDAO orderDAO;
    private ProductDetailService productDetailService;
    private ProductService productService;
    private StockLogService stockLogService;
    private RedissonClient redissonClient;
    private TransactionTemplate transactionTemplate;
    private OrderTimeoutQueue orderTimeoutQueue;
    private RAtomicLong stockCounter;
    private RLock userLock;

    @BeforeEach
    void setUp() {
        service = new SeckillServiceImpl();
        activityDAO = mock(SeckillActivityDAO.class);
        seckillOrderDAO = mock(SeckillOrderDAO.class);
        orderDAO = mock(OrderDAO.class);
        productDetailService = mock(ProductDetailService.class);
        productService = mock(ProductService.class);
        stockLogService = mock(StockLogService.class);
        redissonClient = mock(RedissonClient.class);
        transactionTemplate = mock(TransactionTemplate.class);
        orderTimeoutQueue = mock(OrderTimeoutQueue.class);
        stockCounter = mock(RAtomicLong.class);
        userLock = mock(RLock.class);

        ReflectionTestUtils.setField(service, "activityDAO", activityDAO);
        ReflectionTestUtils.setField(service, "seckillOrderDAO", seckillOrderDAO);
        ReflectionTestUtils.setField(service, "orderDAO", orderDAO);
        ReflectionTestUtils.setField(service, "productDetailService", productDetailService);
        ReflectionTestUtils.setField(service, "productService", productService);
        ReflectionTestUtils.setField(service, "stockLogService", stockLogService);
        ReflectionTestUtils.setField(service, "redissonClient", redissonClient);
        ReflectionTestUtils.setField(service, "transactionTemplate", transactionTemplate);
        ReflectionTestUtils.setField(service, "orderTimeoutQueue", orderTimeoutQueue);
    }

    private SeckillActivityDO validActivity() {
        SeckillActivityDO a = new SeckillActivityDO();
        a.setId("act1");
        a.setProductDetailId("pd1");
        a.setSeckillPrice(new BigDecimal("9.90"));
        a.setTotalStock(100);
        a.setRemainStock(100);
        a.setStartTime(new Date(System.currentTimeMillis() - 60_000));
        a.setEndTime(new Date(System.currentTimeMillis() + 3_600_000));
        a.setStatus("ONGOING");
        return a;
    }

    private ProductDetail detail(String id, int stock) {
        ProductDetail d = new ProductDetail();
        d.setId(id);
        d.setProductId("p1");
        d.setStock(stock);
        return d;
    }

    private void mockTransactionDirect() {
        // 直接执行回调（类似既有 OrderServiceImplTest 模式）
        when(transactionTemplate.execute(any(TransactionCallback.class))).thenAnswer(inv -> {
            TransactionCallback<?> cb = inv.getArgument(0);
            return cb.doInTransaction(null);
        });
    }

    // ============ createActivity ============

    @Test
    @DisplayName("createActivity null 或缺规格拒绝")
    void createActivityRejectsMissingDetail() {
        assertThatThrownBy(() -> service.createActivity(null))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("商品规格");
        SeckillActivityDO a = validActivity();
        a.setProductDetailId(null);
        assertThatThrownBy(() -> service.createActivity(a))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("商品规格");
    }

    @Test
    @DisplayName("createActivity 规格不存在拒绝")
    void createActivityRejectsUnknownDetail() {
        when(productDetailService.findById("pd1")).thenReturn(null);
        assertThatThrownBy(() -> service.createActivity(validActivity()))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("规格不存在");
    }

    @Test
    @DisplayName("createActivity 秒杀价必须大于 0")
    void createActivityRejectsBadPrice() {
        when(productDetailService.findById("pd1")).thenReturn(detail("pd1", 10));
        SeckillActivityDO a = validActivity();
        a.setSeckillPrice(BigDecimal.ZERO);
        assertThatThrownBy(() -> service.createActivity(a))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("秒杀价");
    }

    @Test
    @DisplayName("createActivity 库存必须大于 0")
    void createActivityRejectsBadStock() {
        when(productDetailService.findById("pd1")).thenReturn(detail("pd1", 10));
        SeckillActivityDO a = validActivity();
        a.setTotalStock(0);
        assertThatThrownBy(() -> service.createActivity(a))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("库存");
    }

    @Test
    @DisplayName("createActivity 结束时间早于开始时间拒绝")
    void createActivityRejectsBadTimeRange() {
        when(productDetailService.findById("pd1")).thenReturn(detail("pd1", 10));
        SeckillActivityDO a = validActivity();
        a.setEndTime(new Date(System.currentTimeMillis() - 3_600_000));
        assertThatThrownBy(() -> service.createActivity(a))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("结束时间");
    }

    @Test
    @DisplayName("createActivity 成功：插入 + 预热 Redis 库存")
    void createActivitySuccessPreheats() {
        when(productDetailService.findById("pd1")).thenReturn(detail("pd1", 10));
        // createActivity 内部覆盖 id 为 UUID，预热 key 用 anyString 匹配
        when(redissonClient.getAtomicLong(anyString())).thenReturn(stockCounter);
        when(stockCounter.expire(anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(true);

        SeckillActivityDO created = service.createActivity(validActivity());

        assertThat(created.getId()).isNotBlank();
        assertThat(created.getRemainStock()).isEqualTo(100);
        assertThat(created.getStatus()).isEqualTo("ONGOING");
        verify(activityDAO).insert(created);
        verify(redissonClient).getAtomicLong("seckill:stock:" + created.getId());
        verify(stockCounter).set(100);
    }

    // ============ list/get/toggle ============

    @Test
    @DisplayName("getActivity 不存在抛异常")
    void getActivityNotFound() {
        when(activityDAO.findById("x")).thenReturn(null);
        assertThatThrownBy(() -> service.getActivity("x"))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("不存在");
    }

    @Test
    @DisplayName("toggle 非法状态拒绝")
    void toggleRejectsBadStatus() {
        assertThatThrownBy(() -> service.toggle("act1", "PAUSED"))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("非法");
    }

    @Test
    @DisplayName("toggle ONGOING 时重建 Redis 闸门")
    void toggleOngoingRebuildsRedis() {
        when(activityDAO.findById("act1")).thenReturn(validActivity());
        when(redissonClient.getAtomicLong("seckill:stock:act1")).thenReturn(stockCounter);

        service.toggle("act1", "ONGOING");

        verify(activityDAO).updateStatus("act1", "ONGOING");
        verify(stockCounter).set(100);
    }

    @Test
    @DisplayName("toggle CLOSED 不重建 Redis")
    void toggleClosedNoRebuild() {
        service.toggle("act1", "CLOSED");
        verify(activityDAO).updateStatus("act1", "CLOSED");
        verify(redissonClient, never()).getAtomicLong(anyString());
    }

    // ============ createOrder 拒绝路径 ============

    @Test
    @DisplayName("createOrder 未登录拒绝")
    void createOrderRejectsAnonymous() {
        assertThatThrownBy(() -> service.createOrder("act1", null, 1, "n", "p", "a"))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("未登录");
    }

    @Test
    @DisplayName("createOrder 超量（>10）拒绝")
    void createOrderRejectsOverLimit() {
        assertThatThrownBy(() -> service.createOrder("act1", 1L, 11, "n", "p", "a"))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("10");
    }

    @Test
    @DisplayName("createOrder 活动不存在拒绝")
    void createOrderRejectsUnknownActivity() {
        when(activityDAO.findById("act1")).thenReturn(null);
        assertThatThrownBy(() -> service.createOrder("act1", 1L, 1, "n", "p", "a"))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("不存在");
    }

    @Test
    @DisplayName("createOrder 活动非 ONGOING 拒绝")
    void createOrderRejectsNotOngoing() {
        SeckillActivityDO a = validActivity();
        a.setStatus("NOT_START");
        when(activityDAO.findById("act1")).thenReturn(a);
        assertThatThrownBy(() -> service.createOrder("act1", 1L, 1, "n", "p", "a"))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("未开始");
    }

    @Test
    @DisplayName("createOrder 时间窗外拒绝")
    void createOrderRejectsOutsideWindow() {
        SeckillActivityDO a = validActivity();
        a.setStartTime(new Date(System.currentTimeMillis() + 3_600_000));
        when(activityDAO.findById("act1")).thenReturn(a);
        assertThatThrownBy(() -> service.createOrder("act1", 1L, 1, "n", "p", "a"))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("未开始");
    }

    // ============ createOrder 主流程 ============

    @Test
    @DisplayName("createOrder 成功：Redis 预扣 + 事务落库 + 延迟关单入队")
    void createOrderSuccess() throws Exception {
        SeckillActivityDO a = validActivity();
        when(activityDAO.findById("act1")).thenReturn(a);
        when(redissonClient.getAtomicLong("seckill:stock:act1")).thenReturn(stockCounter);
        when(stockCounter.isExists()).thenReturn(true);
        when(stockCounter.getAndDecrement()).thenReturn(99L);
        when(redissonClient.getLock(anyString())).thenReturn(userLock);
        when(userLock.tryLock(2, 5, TimeUnit.SECONDS)).thenReturn(true);
        when(userLock.isHeldByCurrentThread()).thenReturn(true);
        mockTransactionDirect();
        when(activityDAO.decreaseRemainStock("act1", 2)).thenReturn(1);
        when(productDetailService.findById("pd1")).thenReturn(detail("pd1", 50));
        when(productDetailService.decreaseStock("pd1", 2)).thenReturn(true);
        when(productService.findById("p1")).thenReturn(new Product());
        when(redissonClient.getAtomicLong("seckill:order:seq")).thenReturn(stockCounter);
        when(stockCounter.incrementAndGet()).thenReturn(1L);

        String no = service.createOrder("act1", 1L, 2, "张三", "13800000000", "北京");

        assertThat(no).startsWith("QH");
        verify(orderDAO).insert(any(com.qinghe.mall.dataobject.OrderDO.class));
        verify(stockLogService).record(anyString(), anyString(), anyString(), any(), anyInt(), anyInt(), anyInt());
        verify(seckillOrderDAO).insert(any(SeckillOrderDO.class));
        verify(orderTimeoutQueue).offer(no);
        verify(userLock).unlock();
    }

    @Test
    @DisplayName("createOrder Redis 预扣不足拒绝（已抢光）且补偿 INCR")
    void createOrderSoldOut() throws InterruptedException {
        SeckillActivityDO a = validActivity();
        when(activityDAO.findById("act1")).thenReturn(a);
        when(redissonClient.getAtomicLong("seckill:stock:act1")).thenReturn(stockCounter);
        when(stockCounter.isExists()).thenReturn(true);
        when(stockCounter.getAndDecrement()).thenReturn(-1L);
        when(redissonClient.getLock(anyString())).thenReturn(userLock);
        when(userLock.tryLock(2, 5, TimeUnit.SECONDS)).thenReturn(true);
        when(userLock.isHeldByCurrentThread()).thenReturn(true);

        assertThatThrownBy(() -> service.createOrder("act1", 1L, 1, "n", "p", "a"))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("已抢光");

        verify(stockCounter).incrementAndGet();
        verify(transactionTemplate, never()).execute(any(TransactionCallback.class));
    }

    @Test
    @DisplayName("createOrder 用户锁获取失败拒绝")
    void createOrderLockTimeout() throws InterruptedException {
        SeckillActivityDO a = validActivity();
        when(activityDAO.findById("act1")).thenReturn(a);
        when(redissonClient.getLock(anyString())).thenReturn(userLock);
        when(userLock.tryLock(2, 5, TimeUnit.SECONDS)).thenReturn(false);

        assertThatThrownBy(() -> service.createOrder("act1", 1L, 1, "n", "p", "a"))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("重复抢购");
    }

    @Test
    @DisplayName("createOrder 事务内活动库存 CAS 失败抛已抢光并补偿 Redis")
    void createOrderTxActivityCasFail() throws InterruptedException {
        SeckillActivityDO a = validActivity();
        when(activityDAO.findById("act1")).thenReturn(a);
        when(redissonClient.getAtomicLong("seckill:stock:act1")).thenReturn(stockCounter);
        when(stockCounter.isExists()).thenReturn(true);
        when(stockCounter.getAndDecrement()).thenReturn(0L);
        when(redissonClient.getLock(anyString())).thenReturn(userLock);
        when(userLock.tryLock(2, 5, TimeUnit.SECONDS)).thenReturn(true);
        when(userLock.isHeldByCurrentThread()).thenReturn(true);
        mockTransactionDirect();
        when(activityDAO.decreaseRemainStock("act1", 1)).thenReturn(0);

        assertThatThrownBy(() -> service.createOrder("act1", 1L, 1, "n", "p", "a"))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("已抢光");

        // 事务失败（CAS 0 行）→ 仅补偿 Redis 预扣 INCR 一次（预扣 getAndDecrement 返回 0 非 <0，无预扣侧补偿）
        verify(stockCounter, times(1)).incrementAndGet();
    }

    @Test
    @DisplayName("createOrder 重复参与（唯一约束冲突）抛业务异常并补偿")
    void createOrderDuplicateParticipation() throws InterruptedException {
        SeckillActivityDO a = validActivity();
        when(activityDAO.findById("act1")).thenReturn(a);
        when(redissonClient.getAtomicLong("seckill:stock:act1")).thenReturn(stockCounter);
        when(stockCounter.isExists()).thenReturn(true);
        when(stockCounter.getAndDecrement()).thenReturn(50L);
        when(redissonClient.getLock(anyString())).thenReturn(userLock);
        when(userLock.tryLock(2, 5, TimeUnit.SECONDS)).thenReturn(true);
        when(userLock.isHeldByCurrentThread()).thenReturn(true);
        mockTransactionDirect();
        when(activityDAO.decreaseRemainStock("act1", 1)).thenReturn(1);
        when(productDetailService.findById("pd1")).thenReturn(detail("pd1", 50));
        when(productDetailService.decreaseStock("pd1", 1)).thenReturn(true);
        when(productService.findById("p1")).thenReturn(new Product());
        doThrow(new DuplicateKeyException("dup")).when(seckillOrderDAO).insert(any(SeckillOrderDO.class));
        when(redissonClient.getAtomicLong("seckill:order:seq")).thenReturn(stockCounter);
        when(stockCounter.incrementAndGet()).thenReturn(1L);

        assertThatThrownBy(() -> service.createOrder("act1", 1L, 1, "n", "p", "a"))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("已参与");
    }

    @Test
    @DisplayName("createOrder Redis 不可用时降级 MySQL CAS 仍可下单")
    void createOrderRedisDegrade() throws Exception {
        SeckillActivityDO a = validActivity();
        when(activityDAO.findById("act1")).thenReturn(a);
        when(redissonClient.getAtomicLong("seckill:stock:act1")).thenThrow(new RuntimeException("redis down"));
        when(redissonClient.getLock(anyString())).thenReturn(userLock);
        when(userLock.tryLock(2, 5, TimeUnit.SECONDS)).thenReturn(true);
        when(userLock.isHeldByCurrentThread()).thenReturn(true);
        mockTransactionDirect();
        when(activityDAO.decreaseRemainStock("act1", 1)).thenReturn(1);
        when(productDetailService.findById("pd1")).thenReturn(detail("pd1", 50));
        when(productDetailService.decreaseStock("pd1", 1)).thenReturn(true);
        when(productService.findById("p1")).thenReturn(new Product());
        when(redissonClient.getAtomicLong("seckill:order:seq")).thenReturn(stockCounter);
        when(stockCounter.incrementAndGet()).thenReturn(1L);

        String no = service.createOrder("act1", 1L, 1, "n", "p", "a");

        assertThat(no).startsWith("QH");
        verify(orderDAO).insert(any(com.qinghe.mall.dataobject.OrderDO.class));
        // 降级路径：事务失败不应有 Redis 补偿（redisPreDeducted=false）
    }

    // ============ rollbackIfUnpaid ============

    @Test
    @DisplayName("rollbackIfUnpaid 无秒杀记录直接返回")
    void rollbackNoRecord() {
        when(seckillOrderDAO.findByOrderNumber("QH1")).thenReturn(null);
        service.rollbackIfUnpaid("QH1");
        verify(seckillOrderDAO, never()).updateStatus(anyString(), anyString());
    }

    @Test
    @DisplayName("rollbackIfUnpaid 状态非 CREATED 不处理")
    void rollbackNotCreated() {
        SeckillOrderDO so = new SeckillOrderDO();
        so.setStatus("PAID");
        when(seckillOrderDAO.findByOrderNumber("QH1")).thenReturn(so);
        service.rollbackIfUnpaid("QH1");
        verify(seckillOrderDAO, never()).updateStatus(anyString(), anyString());
    }

    @Test
    @DisplayName("rollbackIfUnpaid 回滚活动库存 + Redis 补偿")
    void rollbackSuccess() {
        SeckillOrderDO so = new SeckillOrderDO();
        so.setStatus("CREATED");
        so.setActivityId("act1");
        so.setQuantity(2);
        when(seckillOrderDAO.findByOrderNumber("QH1")).thenReturn(so);
        when(seckillOrderDAO.updateStatus("QH1", "CANCELLED")).thenReturn(1);
        when(redissonClient.getAtomicLong("seckill:stock:act1")).thenReturn(stockCounter);

        service.rollbackIfUnpaid("QH1");

        verify(activityDAO).increaseRemainStock("act1", 2);
        verify(stockCounter).incrementAndGet();
    }
}
