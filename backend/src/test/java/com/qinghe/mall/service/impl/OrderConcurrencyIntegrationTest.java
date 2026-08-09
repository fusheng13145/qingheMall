package com.qinghe.mall.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.qinghe.mall.dao.OrderDAO;
import com.qinghe.mall.dao.ProductDAO;
import com.qinghe.mall.dao.ProductDetailDAO;
import com.qinghe.mall.dao.UserDAO;
import com.qinghe.mall.model.Order;
import com.qinghe.mall.model.Product;
import com.qinghe.mall.model.ProductDetail;
import com.qinghe.mall.service.OrderService;
import com.qinghe.mall.service.ProductDetailService;
import com.qinghe.mall.service.ProductService;
import com.qinghe.mall.service.UserService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
 * 下单并发不超卖 集成测试（M3-5，遗留A 隔离改造）
 *
 * 使用真实 MySQL + Redis：**动态创建独立测试商品/规格**（不再占用种子 pd001），
 * 20 个线程并发抢购同一规格，断言：
 * 1) 成功创建订单数 == 库存（库存多少卖多少）
 * 2) 剩余库存 == 0（不为负，不超卖）
 * 测试结束删除测试商品/规格/用户/订单，与其他用例零数据交叉。
 */
@SpringBootTest
class OrderConcurrencyIntegrationTest {

    @Autowired
    private OrderService orderService;

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
    private OrderDAO orderDAO;

    @Autowired
    private UserDAO userDAO;

    private static final int STOCK = 5;
    private static final int THREADS = 20;

    private String testDetailId;
    private String testProductId;
    private Long testUserId;

    @BeforeEach
    void setUp() {
        // 独立测试用户（用户名 ≤20 字符，避免超长）
        String userName = "ct" + (System.currentTimeMillis() % 100000000L);
        testUserId = userService.register(userName, "test123456").getId();
        // 动态创建独立测试商品与规格（隔离改造：不占用种子数据）
        long ts = System.nanoTime();
        testDetailId = "pd_it_order_" + ts;
        Product product = new Product();
        product.setName("集成测试商品-订单并发");
        product.setBrand("TEST");
        product.setPrice(new BigDecimal("9.90"));
        product.setProductIntro("integration test product");
        product.setProductImgs("");
        Product created = productService.add(product);
        testProductId = created.getId();
        ProductDetail detail = new ProductDetail();
        detail.setProductId(testProductId);
        detail.setPrice(new BigDecimal("9.90"));
        detail.setSize(1.0);
        detail.setStock(STOCK);
        // replaceByProductId 内部会重新生成规格 id，须回读真实 id 供并发下单使用
        productDetailService.replaceByProductId(testProductId, Collections.singletonList(detail));
        testDetailId = productDetailService.findByProductId(testProductId).get(0).getId();
    }

    @AfterEach
    void tearDown() {
        // 清理：删除测试用户订单 + 测试用户；删除测试商品规格与商品本体
        orderDAO.findByUserIdAndStatus(testUserId, null)
                .forEach(o -> orderDAO.deleteByOrderNumberForTest(o.getOrderNumber()));
        userDAO.deleteByIdForTest(testUserId);
        productDetailDAO.deleteByProductId(testProductId);
        productDAO.deleteById(testProductId);
    }

    @Test
    void concurrentOrders_shouldNotOversell() throws InterruptedException {
        int threads = THREADS;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger success = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    Order order = new Order();
                    order.setUserId(testUserId);
                    order.setProductDetailId(testDetailId);
                    order.setQuantity(1);
                    orderService.createOrder(order);
                    success.incrementAndGet();
                } catch (Throwable t) {
                    // 库存不足/系统繁忙等均为预期失败
                } finally {
                    done.countDown();
                }
            });
        }
        ready.await(10, TimeUnit.SECONDS);
        start.countDown();
        assertTrue(done.await(60, TimeUnit.SECONDS), "并发下单应在超时前完成");
        pool.shutdown();

        int finalStock = productDetailService.findById(testDetailId).getStock();

        assertEquals(STOCK, success.get(), "成功下单数应等于库存数");
        assertEquals(0, finalStock, "库存不应超卖为负");
        assertTrue(finalStock >= 0, "库存必须 >= 0");
    }
}
