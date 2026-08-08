package com.qinghe.mall.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.qinghe.mall.dao.OrderDAO;
import com.qinghe.mall.dao.ProductDetailDAO;
import com.qinghe.mall.dao.UserDAO;
import com.qinghe.mall.model.Order;
import com.qinghe.mall.model.ProductDetail;
import com.qinghe.mall.service.OrderService;
import com.qinghe.mall.service.ProductDetailService;
import com.qinghe.mall.service.UserService;
import java.util.ArrayList;
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
 * 下单并发不超卖 集成测试（M3-5）
 *
 * 使用真实 MySQL + Redis：将 pd001 库存临时置为 5，
 * 20 个线程并发抢购同一规格，断言：
 * 1) 成功创建订单数 == 5（库存多少卖多少）
 * 2) 剩余库存 == 0（不为负，不超卖）
 * 测试结束自动恢复库存并清理测试数据。
 */
@SpringBootTest
class OrderConcurrencyIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductDetailService productDetailService;

    @Autowired
    private ProductDetailDAO productDetailDAO;

    @Autowired
    private UserService userService;

    @Autowired
    private OrderDAO orderDAO;

    @Autowired
    private UserDAO userDAO;

    private static final String DETAIL_ID = "pd001";
    private static final int STOCK = 5;
    private static final int THREADS = 20;

    private Long testUserId;
    private Integer originalStock;

    @BeforeEach
    void setUp() {
        // 临时用户（用户名 ≤20 字符，避免超长）
        String userName = "ct" + (System.currentTimeMillis() % 100000000L);
        testUserId = userService.register(userName, "test123456").getId();
        // 记录并压低库存
        ProductDetail detail = productDetailService.findById(DETAIL_ID);
        originalStock = detail.getStock();
        productDetailDAO.updateStock(DETAIL_ID, STOCK);
    }

    @AfterEach
    void tearDown() {
        // 清理：删除测试用户订单 + 恢复库存 + 删除测试用户
        orderDAO.findByUserIdAndStatus(testUserId, null)
                .forEach(o -> orderDAO.deleteByOrderNumberForTest(o.getOrderNumber()));
        productDetailDAO.updateStock(DETAIL_ID, originalStock);
        userDAO.deleteByIdForTest(testUserId);
    }

    @Test
    void concurrentOrders_shouldNotOversell() throws InterruptedException {
        int threads = THREADS;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger success = new AtomicInteger(0);
        List<Throwable> errors = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    Order order = new Order();
                    order.setUserId(testUserId);
                    order.setProductDetailId(DETAIL_ID);
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

        int finalStock = productDetailService.findById(DETAIL_ID).getStock();

        assertEquals(STOCK, success.get(), "成功下单数应等于库存数");
        assertEquals(0, finalStock, "库存不应超卖为负");
        assertTrue(finalStock >= 0, "库存必须 >= 0");
    }
}
