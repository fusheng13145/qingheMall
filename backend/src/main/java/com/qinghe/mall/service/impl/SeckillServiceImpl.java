package com.qinghe.mall.service.impl;

import com.qinghe.mall.dao.OrderDAO;
import com.qinghe.mall.dao.SeckillActivityDAO;
import com.qinghe.mall.dao.SeckillOrderDAO;
import com.qinghe.mall.dataobject.OrderDO;
import com.qinghe.mall.dataobject.SeckillActivityDO;
import com.qinghe.mall.dataobject.SeckillOrderDO;
import com.qinghe.mall.model.OrderStatus;
import com.qinghe.mall.model.Paging;
import com.qinghe.mall.model.ProductDetail;
import com.qinghe.mall.service.ProductDetailService;
import com.qinghe.mall.service.ProductService;
import com.qinghe.mall.service.SeckillService;
import com.qinghe.mall.service.StockLogService;
import com.qinghe.mall.config.OrderTimeoutQueue;
import com.qinghe.mall.util.UUIDUtils;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;

/**
 * 秒杀服务实现（M5-A3）。
 *
 * 下单流程（事务 + 锁）：
 * 1. 校验活动 ONGOING 且当前时间在 [start, end]；
 * 2. 用户级 Redisson 锁 {@code seckill:user:{activityId}:{userId}} 防同一用户并发重复抢；
 * 3. 事务内：活动库存 CAS 预扣 → 商品 SKU 库存 CAS 扣减 → 普通订单插入（秒杀价）→ stock_log → seckill_order 插入（唯一兜底）；
 * 4. 下单入延迟队列（复用 OrderTimeoutQueue），超时未付自动回滚。
 */
@Service
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    private SeckillActivityDAO activityDAO;

    @Autowired
    private SeckillOrderDAO seckillOrderDAO;

    @Autowired
    private OrderDAO orderDAO;

    @Autowired
    private ProductDetailService productDetailService;

    @Autowired
    private ProductService productService;

    @Autowired
    private StockLogService stockLogService;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    @Lazy
    private OrderTimeoutQueue orderTimeoutQueue;

    @Override
    public SeckillActivityDO createActivity(SeckillActivityDO activity) {
        if (activity == null || activity.getProductDetailId() == null) {
            throw new RuntimeException("活动绑定的商品规格不能为空");
        }
        ProductDetail detail = productDetailService.findById(activity.getProductDetailId());
        if (detail == null) {
            throw new RuntimeException("商品规格不存在");
        }
        if (activity.getSeckillPrice() == null || activity.getSeckillPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("秒杀价必须大于 0");
        }
        if (activity.getTotalStock() == null || activity.getTotalStock() <= 0) {
            throw new RuntimeException("活动库存必须大于 0");
        }
        if (activity.getStartTime() == null || activity.getEndTime() == null) {
            throw new RuntimeException("活动起止时间不能为空");
        }
        if (activity.getEndTime().before(activity.getStartTime())) {
            throw new RuntimeException("活动结束时间必须晚于开始时间");
        }
        activity.setId(UUIDUtils.uuid());
        // 初始剩余库存 = 总库存
        activity.setRemainStock(activity.getTotalStock());
        if (activity.getStatus() == null || activity.getStatus().isEmpty()) {
            activity.setStatus("NOT_START");
        }
        Date now = new Date();
        activity.setGmtCreated(now);
        activity.setGmtModified(now);
        activityDAO.insert(activity);
        return activity;
    }

    @Override
    public Paging<SeckillActivityDO> listActivities(String status, int pageNum, int pageSize) {
        if (pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize < 1 || pageSize > 50) {
            pageSize = 10;
        }
        Page<SeckillActivityDO> page = PageHelper.startPage(pageNum, pageSize)
                .doSelectPage(() -> activityDAO.query(status));
        Paging<SeckillActivityDO> paging = new Paging<>();
        paging.setPageNum(pageNum);
        paging.setPageSize(pageSize);
        paging.setTotalPage(page.getPages());
        paging.setTotalCount(page.getTotal());
        paging.setData(page.getResult());
        return paging;
    }

    @Override
    public List<SeckillActivityDO> listOngoing() {
        return activityDAO.findActive();
    }

    @Override
    public SeckillActivityDO getActivity(String id) {
        SeckillActivityDO activity = activityDAO.findById(id);
        if (activity == null) {
            throw new RuntimeException("活动不存在");
        }
        return activity;
    }

    @Override
    public void toggle(String activityId, String status) {
        if (activityId == null || activityId.isEmpty()) {
            throw new RuntimeException("活动ID不能为空");
        }
        if (!"ONGOING".equals(status) && !"CLOSED".equals(status) && !"ENDED".equals(status)) {
            throw new RuntimeException("非法的活动状态");
        }
        activityDAO.updateStatus(activityId, status);
    }

    @Override
    public String createOrder(String activityId, Long userId, int quantity,
                              String receiverName, String receiverPhone, String receiverAddress) {
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }
        final int qty = quantity <= 0 ? 1 : quantity;
        // 单次抢购数量上限（P2-7）
        if (qty > 10) {
            throw new RuntimeException("单次最多抢购 10 件");
        }
        SeckillActivityDO activity = activityDAO.findById(activityId);
        if (activity == null) {
            throw new RuntimeException("活动不存在");
        }
        if (!"ONGOING".equals(activity.getStatus())) {
            throw new RuntimeException("活动未开始或已结束");
        }
        Date now = new Date();
        if (now.before(activity.getStartTime()) || now.after(activity.getEndTime())) {
            throw new RuntimeException("活动未开始或已结束");
        }

        // 用户级防重锁：同一用户同一活动串行化（最终兜底仍是 uk_user_activity 唯一约束）
        RLock userLock = redissonClient.getLock("seckill:user:" + activityId + ":" + userId);
        boolean locked = false;
        try {
            locked = userLock.tryLock(2, 5, java.util.concurrent.TimeUnit.SECONDS);
            if (!locked) {
                throw new RuntimeException("请勿重复抢购");
            }
            // 事务内：活动库存 CAS 预扣 → 商品库存 CAS 扣减 → 普通订单插入 → 流水 → 秒杀订单插入
            String orderNumber = transactionTemplate.execute(status -> {
                // 1) 活动库存 CAS 预扣（主防线）
                int aff = activityDAO.decreaseRemainStock(activityId, qty);
                if (aff <= 0) {
                    throw new RuntimeException("已抢光");
                }
                // 2) 商品 SKU 库存 CAS 扣减（二次校验）
                ProductDetail before = productDetailService.findById(activity.getProductDetailId());
                if (before == null) {
                    throw new RuntimeException("商品规格不存在");
                }
                boolean decreased = productDetailService.decreaseStock(activity.getProductDetailId(), qty);
                if (!decreased) {
                    throw new RuntimeException("库存不足");
                }
                ProductDetail after = productDetailService.findById(activity.getProductDetailId());

                // 3) 生成普通订单（秒杀价 × 数量，支付链路零侵入）
                String no = generateOrderNumber();
                OrderDO orderDO = new OrderDO();
                orderDO.setId(UUIDUtils.uuid());
                orderDO.setOrderNumber(no);
                orderDO.setUserId(userId);
                // 商家归属：由商品归属推导落库（NULL=平台自营，M6）
                Long merchantId = null;
                if (before != null && before.getProductId() != null) {
                    com.qinghe.mall.model.Product product = productService.findById(before.getProductId());
                    merchantId = product != null ? product.getMerchantId() : null;
                }
                orderDO.setMerchantId(merchantId);
                orderDO.setProductDetailId(activity.getProductDetailId());
                orderDO.setQuantity(qty);
                orderDO.setTotalPrice(activity.getSeckillPrice().multiply(BigDecimal.valueOf(qty)));
                orderDO.setStatus(OrderStatus.WAIT_BUYER_PAY.name());
                // 无优惠券：discount_amount 为 NOT NULL，显式置 0（coupon_id 可空保持 null）
                orderDO.setDiscountAmount(BigDecimal.ZERO);
                orderDO.setReceiverName(receiverName);
                orderDO.setReceiverPhone(receiverPhone);
                orderDO.setReceiverAddress(receiverAddress);
                orderDO.setGmtCreated(new Date());
                orderDO.setGmtModified(new Date());
                orderDAO.insert(orderDO);

                // 4) 库存流水留痕
                stockLogService.record(activity.getProductDetailId(), before.getProductId(), no,
                        StockLogService.TYPE_ORDER_DEDUCT, -quantity, before.getStock(), after.getStock());

                // 5) 秒杀订单（uk_user_activity 唯一兜底；冲突即该用户已参与）
                SeckillOrderDO so = new SeckillOrderDO();
                so.setId(UUIDUtils.uuid());
                so.setUserId(userId);
                so.setActivityId(activityId);
                so.setProductDetailId(activity.getProductDetailId());
                so.setQuantity(qty);
                so.setOrderNumber(no);
                so.setStatus("CREATED");
                so.setGmtCreated(new Date());
                try {
                    seckillOrderDAO.insert(so);
                } catch (DuplicateKeyException e) {
                    throw new RuntimeException("您已参与过该活动");
                }
                return no;
            });

            // 事务已提交：延迟关单入队（超时未付自动回滚活动库存 + 商品库存 + 置 CANCELLED）
            orderTimeoutQueue.offer(orderNumber);
            return orderNumber;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("抢购被中断", e);
        } finally {
            if (locked && userLock.isHeldByCurrentThread()) {
                userLock.unlock();
            }
        }
    }

    @Override
    public void rollbackIfUnpaid(String orderNumber) {
        SeckillOrderDO so = seckillOrderDAO.findByOrderNumber(orderNumber);
        if (so == null) {
            return;
        }
        if (!"CREATED".equals(so.getStatus())) {
            return;
        }
        // 仅当 seckill_order 仍为 CREATED 时回滚活动库存并置 CANCELLED（CAS 守卫）
        int aff = seckillOrderDAO.updateStatus(orderNumber, "CANCELLED");
        if (aff > 0) {
            activityDAO.increaseRemainStock(so.getActivityId(), so.getQuantity());
        }
    }

    private String generateOrderNumber() {
        long sequence = redissonClient.getAtomicLong("seckill:order:seq").incrementAndGet();
        return "QH" + System.currentTimeMillis() + String.format("%04d", sequence % 10000);
    }
}
