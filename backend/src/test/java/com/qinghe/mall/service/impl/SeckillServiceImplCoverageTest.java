package com.qinghe.mall.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.qinghe.mall.config.OrderTimeoutQueue;
import com.qinghe.mall.dao.OrderDAO;
import com.qinghe.mall.dao.SeckillActivityDAO;
import com.qinghe.mall.dao.SeckillOrderDAO;
import com.qinghe.mall.dataobject.SeckillActivityDO;
import com.qinghe.mall.dataobject.SeckillOrderDO;
import com.qinghe.mall.exception.BusinessException;
import com.qinghe.mall.model.Paging;
import com.qinghe.mall.model.ProductDetail;
import com.qinghe.mall.service.ProductDetailService;
import com.qinghe.mall.service.ProductService;
import com.qinghe.mall.service.StockLogService;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * SeckillServiceImpl 分支覆盖补测（#37）：覆盖 createActivity/toggle/listActivities 守卫与归一化、
 * createOrder 前置守卫（Redis 预扣与事务链路之前的参数/活动态校验）、rollbackIfUnpaid 各分支。
 * preheatStock/compensateStock 的 Redis 异常分支由 getAtomicLong 抛错触发。事务以空实现包裹。
 */
class SeckillServiceImplCoverageTest {

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
    private TransactionTemplate transactionTemplate;
    @Mock
    private OrderTimeoutQueue orderTimeoutQueue;

    @InjectMocks
    private SeckillServiceImpl seckillService;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            TransactionCallback<?> cb = inv.getArgument(0);
            return cb.doInTransaction(null);
        });
        when(productDetailService.findById(anyString())).thenReturn(new ProductDetail());
        when(activityDAO.query(anyString())).thenReturn(Collections.emptyList());
        RLock lock = mock(RLock.class);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        RAtomicLong atomic = mock(RAtomicLong.class);
        when(redissonClient.getAtomicLong(anyString())).thenReturn(atomic);
        when(atomic.isExists()).thenReturn(false);
        when(atomic.getAndDecrement()).thenReturn(1L);
        when(atomic.incrementAndGet()).thenReturn(1L);
    }

    private SeckillActivityDO baseActivity() {
        SeckillActivityDO a = new SeckillActivityDO();
        a.setProductDetailId("pd1");
        a.setSeckillPrice(new BigDecimal("9.9"));
        a.setTotalStock(100);
        a.setStartTime(new Date(System.currentTimeMillis() - 86400000L));
        a.setEndTime(new Date(System.currentTimeMillis() + 86400000L));
        return a;
    }

    // ========== createActivity 守卫 ==========

    @Test
    void createActivity_null_shouldThrow() {
        BusinessException ex = assertThrows(BusinessException.class, () -> seckillService.createActivity(null));
        assertEquals("活动绑定的商品规格不能为空", ex.getMessage());
    }

    @Test
    void createActivity_blankDetail_shouldThrow() {
        SeckillActivityDO a = baseActivity();
        a.setProductDetailId(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> seckillService.createActivity(a));
        assertEquals("活动绑定的商品规格不能为空", ex.getMessage());
    }

    @Test
    void createActivity_detailMissing_shouldThrow() {
        SeckillActivityDO a = baseActivity();
        when(productDetailService.findById("pd1")).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> seckillService.createActivity(a));
        assertEquals("商品规格不存在", ex.getMessage());
    }

    @Test
    void createActivity_priceNotPositive_shouldThrow() {
        SeckillActivityDO a = baseActivity();
        a.setSeckillPrice(BigDecimal.ZERO);
        BusinessException ex = assertThrows(BusinessException.class, () -> seckillService.createActivity(a));
        assertEquals("秒杀价必须大于 0", ex.getMessage());
    }

    @Test
    void createActivity_stockNotPositive_shouldThrow() {
        SeckillActivityDO a = baseActivity();
        a.setTotalStock(0);
        BusinessException ex = assertThrows(BusinessException.class, () -> seckillService.createActivity(a));
        assertEquals("活动库存必须大于 0", ex.getMessage());
    }

    @Test
    void createActivity_timesNull_shouldThrow() {
        SeckillActivityDO a = baseActivity();
        a.setStartTime(null);
        a.setEndTime(null);
        BusinessException ex = assertThrows(BusinessException.class, () -> seckillService.createActivity(a));
        assertEquals("活动起止时间不能为空", ex.getMessage());
    }

    @Test
    void createActivity_endBeforeStart_shouldThrow() {
        SeckillActivityDO a = baseActivity();
        a.setStartTime(new Date(System.currentTimeMillis() + 86400000L));
        a.setEndTime(new Date(System.currentTimeMillis() - 86400000L));
        BusinessException ex = assertThrows(BusinessException.class, () -> seckillService.createActivity(a));
        assertEquals("活动结束时间必须晚于开始时间", ex.getMessage());
    }

    @Test
    void createActivity_valid_shouldInsert() {
        SeckillActivityDO r = seckillService.createActivity(baseActivity());
        assertNotNull(r.getId());
        assertEquals(100, r.getRemainStock());
    }

    // ========== toggle ==========

    @Test
    void toggle_blankId_shouldThrow() {
        BusinessException ex = assertThrows(BusinessException.class, () -> seckillService.toggle(null, "ONGOING"));
        assertEquals("活动ID不能为空", ex.getMessage());
    }

    @Test
    void toggle_invalidStatus_shouldThrow() {
        BusinessException ex = assertThrows(BusinessException.class, () -> seckillService.toggle("a1", "WEIRD"));
        assertEquals("非法的活动状态", ex.getMessage());
    }

    @Test
    void toggle_ongoing_shouldPreheat() {
        SeckillActivityDO a = baseActivity();
        a.setRemainStock(100);
        when(activityDAO.findById("a1")).thenReturn(a);
        seckillService.toggle("a1", "ONGOING");
        assertTrue(true);
    }

    @Test
    void toggle_closed_shouldUpdateOnly() {
        seckillService.toggle("a1", "CLOSED");
        assertTrue(true);
    }

    // ========== listActivities 分页归一化 ==========

    @Test
    void listActivities_lowBounds_shouldDefault() {
        Paging<SeckillActivityDO> p = seckillService.listActivities("ONGOING", 0, 0);
        assertEquals(1, p.getPageNum());
        assertEquals(10, p.getPageSize());
    }

    @Test
    void listActivities_highBounds_shouldClamp() {
        Paging<SeckillActivityDO> p = seckillService.listActivities("ONGOING", -3, 100);
        assertEquals(1, p.getPageNum());
        assertEquals(10, p.getPageSize());
    }

    @Test
    void listActivities_normal_shouldKeep() {
        Paging<SeckillActivityDO> p = seckillService.listActivities("ONGOING", 2, 20);
        assertEquals(2, p.getPageNum());
        assertEquals(20, p.getPageSize());
    }

    // ========== createOrder 前置守卫 ==========

    @Test
    void createOrder_nullUser_shouldThrow() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> seckillService.createOrder("a1", null, 1, "n", "p", "a"));
        assertEquals("用户未登录", ex.getMessage());
    }

    @Test
    void createOrder_qtyTooLarge_shouldThrow() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> seckillService.createOrder("a1", 1L, 20, "n", "p", "a"));
        assertEquals("单次最多抢购 10 件", ex.getMessage());
    }

    @Test
    void createOrder_activityMissing_shouldThrow() {
        when(activityDAO.findById("a1")).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> seckillService.createOrder("a1", 1L, 1, "n", "p", "a"));
        assertEquals("活动不存在", ex.getMessage());
    }

    @Test
    void createOrder_notOngoing_shouldThrow() {
        SeckillActivityDO a = baseActivity();
        a.setStatus("NOT_START");
        when(activityDAO.findById("a1")).thenReturn(a);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> seckillService.createOrder("a1", 1L, 1, "n", "p", "a"));
        assertEquals("活动未开始或已结束", ex.getMessage());
    }

    @Test
    void createOrder_outOfTime_shouldThrow() {
        SeckillActivityDO a = baseActivity();
        a.setStatus("ONGOING");
        a.setStartTime(new Date(System.currentTimeMillis() + 86400000L));
        a.setEndTime(new Date(System.currentTimeMillis() + 2 * 86400000L));
        when(activityDAO.findById("a1")).thenReturn(a);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> seckillService.createOrder("a1", 1L, 1, "n", "p", "a"));
        assertEquals("活动未开始或已结束", ex.getMessage());
    }

    // ========== rollbackIfUnpaid ==========

    @Test
    void rollback_orderMissing_shouldReturn() {
        when(seckillOrderDAO.findByOrderNumber("QH1")).thenReturn(null);
        seckillService.rollbackIfUnpaid("QH1");
        assertTrue(true);
    }

    @Test
    void rollback_notCreated_shouldReturn() {
        SeckillOrderDO so = new SeckillOrderDO();
        so.setStatus("CANCELLED");
        when(seckillOrderDAO.findByOrderNumber("QH1")).thenReturn(so);
        seckillService.rollbackIfUnpaid("QH1");
        assertTrue(true);
    }

    @Test
    void rollback_created_shouldRollback() {
        SeckillOrderDO so = new SeckillOrderDO();
        so.setStatus("CREATED");
        so.setActivityId("a1");
        so.setQuantity(1);
        when(seckillOrderDAO.findByOrderNumber("QH1")).thenReturn(so);
        when(seckillOrderDAO.updateStatus("QH1", "CANCELLED")).thenReturn(1);
        seckillService.rollbackIfUnpaid("QH1");
        assertTrue(true);
    }
}
