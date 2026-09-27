package com.qinghe.mall.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qinghe.mall.config.SnowflakeIdGenerator;
import com.qinghe.mall.dao.OrderDAO;
import com.qinghe.mall.dao.SeckillActivityDAO;
import com.qinghe.mall.dao.SeckillOrderDAO;
import com.qinghe.mall.dataobject.OrderDO;
import com.qinghe.mall.dataobject.SeckillActivityDO;
import com.qinghe.mall.exception.BusinessException;
import com.qinghe.mall.model.ProductDetail;
import com.qinghe.mall.service.ProductDetailService;
import com.qinghe.mall.service.ProductService;
import com.qinghe.mall.service.StockLogService;
import java.math.BigDecimal;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 秒杀服务守卫分支补测（v1.12，复盘 §11.7 清单）。
 *
 * 覆盖：未登录/数量上限/活动不存在/状态与时间窗/防重锁竞争/
 * Redis 闸门预扣为负的「已抢光 + 补偿」/Redis 初始化降级 MySQL CAS 的完整下单链路。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SeckillServiceBranchTest {

    @Mock
    private SeckillActivityDAO activityDAO;

    @Mock
    private SeckillOrderDAO seckillOrderDAO;

    @Mock
    private OrderDAO orderDAO;

    @Mock
    private ProductDetailService productDetailService;

    @Mock
    private ProductService productService;

    @Mock
    private StockLogService stockLogService;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private SnowflakeIdGenerator snowflakeIdGenerator;

    private SeckillServiceImpl service;

    private SeckillActivityDO ongoing;

    @BeforeEach
    void setUp() {
        service = new SeckillServiceImpl();
        ReflectionTestUtils.setField(service, "activityDAO", activityDAO);
        ReflectionTestUtils.setField(service, "seckillOrderDAO", seckillOrderDAO);
        ReflectionTestUtils.setField(service, "orderDAO", orderDAO);
        ReflectionTestUtils.setField(service, "productDetailService", productDetailService);
        ReflectionTestUtils.setField(service, "productService", productService);
        ReflectionTestUtils.setField(service, "stockLogService", stockLogService);
        ReflectionTestUtils.setField(service, "redissonClient", redissonClient);
        ReflectionTestUtils.setField(service, "orderTimeoutQueue",
                org.mockito.Mockito.mock(com.qinghe.mall.config.OrderTimeoutQueue.class));
        ReflectionTestUtils.setField(service, "snowflakeIdGenerator", snowflakeIdGenerator);
        ReflectionTestUtils.setField(service, "transactionTemplate", new TransactionTemplate() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> T execute(TransactionCallback<T> action) {
                return action.doInTransaction(null);
            }
        });

        RAtomicLong orderSeq = mock(RAtomicLong.class);
        when(orderSeq.incrementAndGet()).thenReturn(1L);
        when(redissonClient.getAtomicLong(anyString())).thenReturn(orderSeq);
        when(snowflakeIdGenerator.nextId()).thenReturn(1L);
        when(orderDAO.insert(any(OrderDO.class))).thenReturn(1);

        ongoing = new SeckillActivityDO();
        ongoing.setId("act1");
        ongoing.setProductDetailId("d1");
        ongoing.setSeckillPrice(new BigDecimal("1.00"));
        ongoing.setTotalStock(10);
        ongoing.setRemainStock(10);
        ongoing.setStatus("ONGOING");
        ongoing.setMerchantId(null); // 平台秒杀
        ongoing.setStartTime(new Date(System.currentTimeMillis() - 60_000));
        ongoing.setEndTime(new Date(System.currentTimeMillis() + 3_600_000));
        when(activityDAO.findById("act1")).thenReturn(ongoing);
    }

    private RLock mockLock(boolean acquired) {
        RLock userLock = mock(RLock.class);
        try {
            when(userLock.tryLock(2, 5, java.util.concurrent.TimeUnit.SECONDS)).thenReturn(acquired);
        } catch (Exception ignored) {
            // Mockito stub 受检异常不会真正抛出
        }
        when(redissonClient.getLock(anyString())).thenReturn(userLock);
        return userLock;
    }

    private RAtomicLong mockStockCounter(long remain) {
        RAtomicLong counter = mock(RAtomicLong.class);
        when(counter.isExists()).thenReturn(true);
        when(counter.getAndDecrement()).thenReturn(remain);
        when(redissonClient.getAtomicLong(anyString())).thenReturn(counter);
        return counter;
    }

    @Test
    @DisplayName("未登录拒绝")
    void createOrder_anonymous_rejected() {
        assertThrows(BusinessException.class,
                () -> service.createOrder("act1", null, 1, "n", "p", "a"));
    }

    @Test
    @DisplayName("单次抢购数量超上限拒绝")
    void createOrder_qtyOverLimit_rejected() {
        assertThrows(BusinessException.class,
                () -> service.createOrder("act1", 9L, 11, "n", "p", "a"));
    }

    @Test
    @DisplayName("活动不存在拒绝")
    void createOrder_activityMissing_rejected() {
        when(activityDAO.findById("nope")).thenReturn(null);
        assertThrows(BusinessException.class,
                () -> service.createOrder("nope", 9L, 1, "n", "p", "a"));
    }

    @Test
    @DisplayName("状态非 ONGOING 拒绝")
    void createOrder_notOngoing_rejected() {
        ongoing.setStatus("NOT_START");
        assertThrows(BusinessException.class,
                () -> service.createOrder("act1", 9L, 1, "n", "p", "a"));
    }

    @Test
    @DisplayName("时间窗外拒绝")
    void createOrder_windowExpired_rejected() {
        ongoing.setEndTime(new Date(System.currentTimeMillis() - 1000));
        assertThrows(BusinessException.class,
                () -> service.createOrder("act1", 9L, 1, "n", "p", "a"));
    }

    @Test
    @DisplayName("防重锁竞争失败拒绝（请勿重复抢购）")
    void createOrder_lockBusy_rejected() {
        mockLock(false);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createOrder("act1", 9L, 1, "n", "p", "a"));
        assertEquals("请勿重复抢购", ex.getMessage());
    }

    @Test
    @DisplayName("Redis 闸门预扣为负：已抢光 + 补偿回增")
    void createOrder_redisGateExhausted_rejectedAndCompensated() {
        mockLock(true);
        RAtomicLong counter = mockStockCounter(-1);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createOrder("act1", 9L, 1, "n", "p", "a"));
        assertEquals("已抢光", ex.getMessage());
        verify(counter).incrementAndGet();
        verify(activityDAO, org.mockito.Mockito.never()).decreaseRemainStock(anyString(), org.mockito.Mockito.anyInt());
    }

    @Test
    @DisplayName("Redis 初始化降级 MySQL CAS：完整下单链路走通")
    void createOrder_redisDegrades_mysqlCasPath() {
        // getAtomicLong 抛异常 → redisGate=false → 走 MySQL CAS 事务
        when(redissonClient.getAtomicLong(org.mockito.ArgumentMatchers.startsWith("seckill:stock:")))
                .thenThrow(new RuntimeException("redis down"));
        RLock userLock = mockLock(true);
        ProductDetail detail = new ProductDetail();
        detail.setId("d1");
        detail.setProductId("p1");
        detail.setPrice(new BigDecimal("799.00"));
        detail.setStock(5);
        when(productDetailService.findById("d1")).thenReturn(detail);
        when(productDetailService.decreaseStock("d1", 1)).thenReturn(true);
        when(productService.findById("p1")).thenReturn(null); // 平台自营分支
        when(activityDAO.decreaseRemainStock("act1", 1)).thenReturn(1);

        String orderNumber = service.createOrder("act1", 9L, 1, "n", "p", "a");

        org.junit.jupiter.api.Assertions.assertNotNull(orderNumber);
        verify(activityDAO).decreaseRemainStock("act1", 1);
        verify(orderDAO).insert(any(OrderDO.class));
        verify(seckillOrderDAO).insert(any(com.qinghe.mall.dataobject.SeckillOrderDO.class));
    }
}
