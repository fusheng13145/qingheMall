package com.qinghe.mall.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.qinghe.mall.dao.CommentDAO;
import com.qinghe.mall.dao.OrderDAO;
import com.qinghe.mall.exception.BusinessException;
import com.qinghe.mall.model.Order;
import com.qinghe.mall.model.OrderStatus;
import com.qinghe.mall.model.Product;
import com.qinghe.mall.model.ProductDetail;
import com.qinghe.mall.model.User;

/**
 * OrderServiceImpl 分支缺口靶向补测（v1.11，复盘 §11.7-P1）。
 *
 * 覆盖：allocateDiscount 防御边界（空集/零合计/触顶回填/防御 break/单笔）、
 * fillExtraBatch 空值填充链（缺规格/缺商品/缺图/用户脱敏/已评价标记）、
 * firstImg 分隔符分支、doCreateOrder「优惠金额缺失」、batchCreateOrders「券模板不存在」、
 * findPageByUserIdAndStatus 分页收敛、applyRefund 入参边界。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceImplBranchGapsTest {

    @Mock
    private OrderDAO orderDAO;

    @Mock
    private com.qinghe.mall.service.ProductDetailService productDetailService;

    @Mock
    private com.qinghe.mall.service.ProductService productService;

    @Mock
    private com.qinghe.mall.service.CouponService couponService;

    @Mock
    private com.qinghe.mall.service.UserService userService;

    @Mock
    private com.qinghe.mall.service.StockLogService stockLogService;

    @Mock
    private CommentDAO commentDAO;

    @Mock
    private com.qinghe.mall.config.OrderTimeoutQueue orderTimeoutQueue;

    @Mock
    private com.qinghe.mall.service.SeckillService seckillService;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock lock;

    @Mock
    private com.qinghe.mall.service.SettlementService settlementService;

    @Mock
    private com.qinghe.mall.config.SnowflakeIdGenerator snowflakeIdGenerator;

    @InjectMocks
    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(orderService, "transactionTemplate",
                new TransactionTemplate() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public <T> T execute(TransactionCallback<T> action) {
                        return action.doInTransaction(null);
                    }
                });
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        try {
            when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
        } catch (Exception ignored) {
            // Mockito stub 受检异常不会真正抛出
        }
        RAtomicLong seq = org.mockito.Mockito.mock(RAtomicLong.class);
        when(seq.incrementAndGet()).thenReturn(1L, 2L, 3L, 4L, 5L);
        when(redissonClient.getAtomicLong(anyString())).thenReturn(seq);
        when(snowflakeIdGenerator.nextId()).thenReturn(1L, 2L, 3L, 4L, 5L);
        when(orderDAO.insert(any(com.qinghe.mall.dataobject.OrderDO.class))).thenReturn(1);
    }

    // ========== allocateDiscount 防御边界（私有方法，反射调用） ==========

    @SuppressWarnings("unchecked")
    private List<BigDecimal> allocate(List<BigDecimal> originals, String discount) {
        List<Order> eligible = new ArrayList<>();
        Map<Order, BigDecimal> totals = new HashMap<>();
        for (BigDecimal o : originals) {
            Order o1 = new Order();
            o1.setOrderNumber("QH" + o);
            eligible.add(o1);
            totals.put(o1, o);
        }
        return (List<BigDecimal>) ReflectionTestUtils.invokeMethod(orderService, "allocateDiscount",
                new BigDecimal(discount), eligible, totals);
    }

    @Test
    @DisplayName("allocateDiscount：空可核销集返回空")
    void allocate_emptyEligible_returnsEmpty() {
        assertTrue(allocate(new ArrayList<>(), "1.00").isEmpty());
    }

    @Test
    @DisplayName("allocateDiscount：原价合计为 0 的防御分支（全零分摊）")
    void allocate_zeroOriginalSum_allZero() {
        List<BigDecimal> result = allocate(List.of(new BigDecimal("0.00"), new BigDecimal("0.00")), "1.00");
        assertEquals(2, result.size());
        assertEquals(0, result.get(0).compareTo(BigDecimal.ZERO));
        assertEquals(0, result.get(1).compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("allocateDiscount：触顶回填（无门槛小额券超单笔原价）")
    void allocate_clampAndRefill() {
        // 券 0.02 分摊到两笔 0.01：按比例各 0.01 恰好触顶 → 钳制后守恒
        List<BigDecimal> result = allocate(List.of(new BigDecimal("0.01"), new BigDecimal("0.01")), "0.02");
        assertEquals(0, result.get(0).compareTo(new BigDecimal("0.01")));
        assertEquals(0, result.get(1).compareTo(new BigDecimal("0.01")));
    }

    @Test
    @DisplayName("allocateDiscount：总额超可核销合计的防御 break（钳制到底不抛错）")
    void allocate_defensiveBreak_noThrow() {
        List<BigDecimal> result = allocate(List.of(new BigDecimal("0.01"), new BigDecimal("0.01")), "1.00");
        assertEquals(0, result.get(0).compareTo(new BigDecimal("0.01")));
        assertEquals(0, result.get(1).compareTo(new BigDecimal("0.01")));
    }

    @Test
    @DisplayName("allocateDiscount：单笔订单整额分摊")
    void allocate_singleOrder_exact() {
        List<BigDecimal> result = allocate(List.of(new BigDecimal("5.00")), "1.25");
        assertEquals(0, result.get(0).compareTo(new BigDecimal("1.25")));
    }

    // ========== fillExtraBatch 空值填充链（私有方法，反射调用） ==========

    @SuppressWarnings("unchecked")
    private List<Order> fill(List<Order> orders) {
        return (List<Order>) ReflectionTestUtils.invokeMethod(orderService, "fillExtraBatch", orders);
    }

    @Test
    @DisplayName("fillExtraBatch：null/空列表原样返回")
    void fill_nullAndEmpty_passthrough() {
        assertNull(fill(null));
        assertTrue(fill(new ArrayList<>()).isEmpty());
    }

    @Test
    @DisplayName("fillExtraBatch：全空字段订单（无规格/无用户/无单号）零查询直通")
    void fill_blankFields_noLookups() {
        Order o = new Order();
        List<Order> result = fill(List.of(o));
        assertEquals(1, result.size());
        assertNull(result.get(0).getProductDetail());
        assertNull(result.get(0).getUser());
    }

    @Test
    @DisplayName("fillExtraBatch：规格缺失/商品缺失/商品图片多分支/用户脱敏/已评价标记")
    void fill_missingAndPresent_branches() {
        // 订单 1：规格存在 → 商品存在（多图 firstImg 取首张）+ 用户存在（脱敏）+ 已评价
        Order full = new Order();
        full.setOrderNumber("QH1");
        full.setUserId(9L);
        full.setProductDetailId("d1");
        // 订单 2：规格查不到 → pd null 分支；用户查不到 → null
        Order ghost = new Order();
        ghost.setOrderNumber("QH2");
        ghost.setUserId(8L);
        ghost.setProductDetailId("ghost");
        // 订单 3：规格存在但商品查不到 → product null 分支
        Order noProduct = new Order();
        noProduct.setOrderNumber("QH3");
        noProduct.setUserId(9L);
        noProduct.setProductDetailId("d2");

        ProductDetail d1 = new ProductDetail();
        d1.setId("d1");
        d1.setProductId("p1");
        ProductDetail d2 = new ProductDetail();
        d2.setId("d2");
        d2.setProductId("pX");
        when(productDetailService.findByIds(anyList())).thenReturn(List.of(d1, d2));

        Product p1 = new Product();
        p1.setId("p1");
        p1.setName("冒烟商品");
        p1.setProductImgs("  ; a.jpg b.jpg ;  ");
        when(productService.findByIds(anyList())).thenReturn(List.of(p1));

        User u9 = new User();
        u9.setId(9L);
        u9.setPwd("secret");
        when(userService.findByIds(anyList())).thenReturn(new ArrayList<>(List.of(u9)));

        when(commentDAO.findCommentedOrderNumbers(anyList())).thenReturn(List.of("QH1"));

        List<Order> result = fill(new ArrayList<>(List.of(full, ghost, noProduct)));

        // 订单 1：名称 + 首图（firstImg 跳过空段）+ 用户脱敏 + 已评价
        assertEquals("冒烟商品", result.get(0).getProductName());
        assertEquals("a.jpg", result.get(0).getProductImg());
        assertNull(result.get(0).getUser().getPwd());
        assertEquals(Boolean.TRUE, result.get(0).getCommented());
        // 订单 2：规格缺失分支
        assertNull(result.get(1).getProductDetail());
        assertNull(result.get(1).getUser());
        // 订单 3：商品缺失分支（有规格无商品）
        assertEquals("pX", result.get(2).getProductDetail().getProductId());
        assertNull(result.get(2).getProductName());
        // 未评价订单
        assertEquals(Boolean.FALSE, result.get(1).getCommented());
    }

    @Test
    @DisplayName("fillExtraBatch：商品图片全空段 → 首图 null")
    void fill_blankImgs_productImgNull() {
        Order o = new Order();
        o.setOrderNumber("QH1");
        o.setProductDetailId("d1");
        ProductDetail d1 = new ProductDetail();
        d1.setId("d1");
        d1.setProductId("p1");
        when(productDetailService.findByIds(anyList())).thenReturn(List.of(d1));
        Product p1 = new Product();
        p1.setId("p1");
        p1.setProductImgs("  ;  ");
        when(productService.findByIds(anyList())).thenReturn(List.of(p1));

        List<Order> result = fill(List.of(o));
        assertNull(result.get(0).getProductImg());
    }

    // ========== firstImg 分隔符分支（私有方法，反射调用） ==========

    private String firstImg(String imgs) {
        return (String) ReflectionTestUtils.invokeMethod(orderService, "firstImg", imgs);
    }

    @Test
    @DisplayName("firstImg：null/空串/多分隔/全空段四分支")
    void firstImg_branches() {
        assertNull(firstImg(null));
        assertNull(firstImg("  "));
        assertEquals("a.jpg", firstImg("a.jpg b.jpg"));
        assertEquals("a.jpg", firstImg(";a.jpg; ;b.jpg;"));
        assertNull(firstImg(" ; ; "));
    }

    // ========== doCreateOrder 券分支边界 ==========

    @Test
    @DisplayName("单笔带券但优惠额缺失：事务内拒绝「优惠金额缺失」")
    void createOrder_couponWithoutAmount_rejected() {
        ProductDetail pd = new ProductDetail();
        pd.setId("d1");
        pd.setProductId("p1");
        pd.setPrice(new BigDecimal("10.00"));
        pd.setStock(5);
        when(productDetailService.findById("d1")).thenReturn(pd);
        when(productDetailService.decreaseStock("d1", 1)).thenReturn(true);

        Order o = new Order();
        o.setUserId(9L);
        o.setProductDetailId("d1");
        o.setQuantity(1);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> orderService.createOrder(o, "uc1", null));
        assertEquals("优惠金额缺失", ex.getMessage());
    }

    @Test
    @DisplayName("batchCreateOrders：券模板不存在拒绝")
    void batch_couponTemplateMissing_rejected() {
        ProductDetail pd = new ProductDetail();
        pd.setId("d1");
        pd.setProductId("p1");
        pd.setPrice(new BigDecimal("10.00"));
        pd.setStock(5);
        when(productDetailService.findById("d1")).thenReturn(pd);
        when(productService.findById("p1")).thenReturn(new Product());
        when(couponService.findCouponByUserCouponId("ucX")).thenReturn(null);
        Order o = new Order();
        o.setUserId(9L);
        o.setProductDetailId("d1");
        o.setQuantity(1);
        o.setCouponId("ucX");
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> orderService.batchCreateOrders(List.of(o)));
        assertEquals("优惠券不存在", ex.getMessage());
    }

    // ========== findPageByUserIdAndStatus 分页收敛 ==========

    @Test
    @DisplayName("用户订单分页：pageNum/pageSize 缺省与越界收敛")
    void findPageByUserIdAndStatus_defaultsAndClamp() {
        when(orderDAO.findByUserIdAndStatus(eq(9L), isNull())).thenReturn(new ArrayList<>());
        when(orderDAO.findByUserIdAndStatus(eq(9L), eq("WAIT_BUYER_PAY"))).thenReturn(new ArrayList<>());

        // 全缺省
        var p1 = orderService.findPageByUserIdAndStatus(9L, null, null, null);
        assertEquals(1, p1.getPageNum());
        assertEquals(10, p1.getPageSize());
        // 越界收敛：pageNum=0 → 1，pageSize=99 → 10
        var p2 = orderService.findPageByUserIdAndStatus(9L, "WAIT_BUYER_PAY", 0, 99);
        assertEquals(1, p2.getPageNum());
        assertEquals(10, p2.getPageSize());
    }

    // ========== applyRefund 入参边界 ==========

    @Test
    @DisplayName("applyRefund：订单号空白 / 订单不存在 / 非本人三边界")
    void applyRefund_guards() {
        assertThrows(IllegalArgumentException.class, () -> orderService.applyRefund(" ", 9L));

        when(orderDAO.findByOrderNumber("QH404")).thenReturn(null);
        assertThrows(BusinessException.class, () -> orderService.applyRefund("QH404", 9L));

        com.qinghe.mall.dataobject.OrderDO od = new com.qinghe.mall.dataobject.OrderDO();
        od.setOrderNumber("QH1");
        od.setUserId(1L);
        od.setStatus(OrderStatus.TRADE_PAID_SUCCESS.name());
        when(orderDAO.findByOrderNumber("QH1")).thenReturn(od);
        assertThrows(BusinessException.class, () -> orderService.applyRefund("QH1", 9L));
    }
}
