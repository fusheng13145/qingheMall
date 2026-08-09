package com.qinghe.mall.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.qinghe.mall.dao.OrderDAO;
import com.qinghe.mall.dao.ProductDAO;
import com.qinghe.mall.dao.ProductDetailDAO;
import com.qinghe.mall.dao.SeckillActivityDAO;
import com.qinghe.mall.dao.SeckillOrderDAO;
import com.qinghe.mall.dao.UserDAO;
import com.qinghe.mall.dataobject.SeckillActivityDO;
import com.qinghe.mall.model.Product;
import com.qinghe.mall.model.ProductDetail;
import com.qinghe.mall.service.SeckillService;
import com.qinghe.mall.service.ProductDetailService;
import com.qinghe.mall.service.ProductService;
import com.qinghe.mall.service.UserService;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 秒杀并发不超卖 集成测试（M5-A3，遗留A 隔离改造）
 *
 * 使用真实 MySQL + Redis：**动态创建独立测试商品/规格**，创建活动库存 M=10，
 * 30 个线程各自用独立测试用户抢购 qty=1，断言：
 * 1) 成功下单数 == 10（活动库存多少卖多少）
 * 2) 活动剩余库存 == 0（不为负，不超卖）
 * 测试结束清理测试活动 / 秒杀订单 / 测试用户 / 测试商品。
 */
@SpringBootTest
class SeckillConcurrencyIntegrationTest {

    @Autowired
    private SeckillService seckillService;

    @Autowired
    private SeckillActivityDAO activityDAO;

    @Autowired
    private SeckillOrderDAO seckillOrderDAO;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductDetailService productDetailService;

    @Autowired
    private ProductDAO productDAO;

    @Autowired
    private ProductDetailDAO productDetailDAO;

    @Autowired
    private UserService userService;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private OrderDAO orderDAO;

    private static final int STOCK = 10;
    private static final int THREADS = 30;

    private String activityId;
    private String testDetailId;
    private String testProductId;
    private final List<Long> testUserIds = new CopyOnWriteArrayList<>();

    @BeforeEach
    void setUp() {
        // 动态创建独立测试商品与规格（隔离改造：不占用种子数据）
        long ts = System.nanoTime();
        testDetailId = "pd_it_seckill_" + ts;
        Product product = new Product();
        product.setName("集成测试商品-秒杀并发");
        product.setBrand("TEST");
        product.setPrice(new BigDecimal("9.90"));
        product.setProductIntro("integration test seckill product");
        product.setProductImgs("");
        Product created = productService.add(product);
        testProductId = created.getId();
        ProductDetail detail = new ProductDetail();
        detail.setProductId(testProductId);
        detail.setPrice(new BigDecimal("9.90"));
        detail.setSize(1.0);
        detail.setStock(1000);
        // replaceByProductId 内部会重新生成规格 id，须回读真实 id 供活动绑定
        productDetailService.replaceByProductId(testProductId, Collections.singletonList(detail));
        testDetailId = productDetailService.findByProductId(testProductId).get(0).getId();

        // 创建活动（库存 M，进行中）
        SeckillActivityDO activity = new SeckillActivityDO();
        activity.setProductDetailId(testDetailId);
        activity.setSeckillPrice(new BigDecimal("1.00"));
        activity.setTotalStock(STOCK);
        activity.setRemainStock(STOCK);
        Date now = new Date();
        activity.setStartTime(new Date(now.getTime() - 60_000L));
        activity.setEndTime(new Date(now.getTime() + 3_600_000L));
        activity.setStatus("ONGOING");
        SeckillActivityDO createdActivity = seckillService.createActivity(activity);
        activityId = createdActivity.getId();
    }

    @AfterEach
    void tearDown() {
        // 清理：删除测试用户的普通订单 + 测试用户；删除活动相关秒杀订单；删除活动；删除测试商品
        for (Long uid : testUserIds) {
            orderDAO.findByUserIdAndStatus(uid, null)
                    .forEach(o -> orderDAO.deleteByOrderNumberForTest(o.getOrderNumber()));
            userDAO.deleteByIdForTest(uid);
        }
        seckillOrderDAO.deleteByActivityIdForTest(activityId);
        activityDAO.deleteByIdForTest(activityId);
        productDetailDAO.deleteByProductId(testProductId);
        productDAO.deleteById(testProductId);
    }

    @Test
    void concurrentSeckill_shouldNotOversell() throws InterruptedException {
        int threads = THREADS;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger success = new AtomicInteger(0);
        List<String> errors = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                // 每线程独立测试用户（绕过 uk_user_activity 唯一约束，专注验证活动库存 CAS）
                Long uid = userService.register("sk" + System.nanoTime(), "test123456").getId();
                testUserIds.add(uid);
                ready.countDown();
                try {
                    start.await();
                    seckillService.createOrder(activityId, uid, 1, null, null, null);
                    success.incrementAndGet();
                } catch (Throwable t) {
                    if (errors.size() < 5) {
                        errors.add(t.getClass().getSimpleName() + ": " + t.getMessage());
                    }
                } finally {
                    done.countDown();
                }
            });
        }
        ready.await(10, TimeUnit.SECONDS);
        start.countDown();
        assertTrue(done.await(60, TimeUnit.SECONDS), "并发抢购应在超时前完成");
        pool.shutdown();

        SeckillActivityDO after = activityDAO.findById(activityId);
        int finalRemain = after.getRemainStock();

        assertEquals(STOCK, success.get(), "成功抢购数应等于活动库存数; 错误样本=" + errors);
        assertEquals(0, finalRemain, "活动剩余库存不应超卖为负");
        assertTrue(finalRemain >= 0, "活动剩余库存必须 >= 0");
    }
}
