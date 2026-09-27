package com.qinghe.mall.service.impl;

import com.qinghe.mall.exception.BusinessException;
import com.qinghe.mall.dao.OrderDAO;
import com.qinghe.mall.dao.CommentDAO;
import com.qinghe.mall.dataobject.CouponDO;
import com.qinghe.mall.dataobject.OrderDO;
import com.qinghe.mall.model.Order;
import com.qinghe.mall.model.OrderStatus;
import com.qinghe.mall.model.Paging;
import com.qinghe.mall.model.Product;
import com.qinghe.mall.model.ProductDetail;
import com.qinghe.mall.model.User;
import com.qinghe.mall.service.OrderService;
import com.qinghe.mall.service.ProductDetailService;
import com.qinghe.mall.service.ProductService;
import com.qinghe.mall.service.SeckillService;
import com.qinghe.mall.service.StockLogService;
import com.qinghe.mall.service.UserService;
import com.qinghe.mall.util.UUIDUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RedissonClient;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class OrderServiceImpl implements OrderService {

    /**
     * P1-6（2026-08-11）：管理员改单允许的状态转移白名单（状态机守卫）。
     * 此前 /admin/order/updateStatus 仅校验目标值是合法枚举，管理员可把订单改成任意状态、
     * 绕过状态机（如已完成→待付款、已关闭→已发货）。现仅放行下表列出的纠正性转移：
     *   待付款   → 已付款 / 已关闭
     *   已付款   → 已发货 / 已关闭 / 退款中 / 已退款
     *   已发货   → 已完成 / 退款中
     *   退款中   → 已退款（批准）/ 已付款（驳回回退）
     *   已完成   → 退款中（售后）
     * 终态（已关闭/已退款）无任何出边，禁止"复活"订单；
     * TRADE_PAID_FAILED 为未使用状态，不作为任何转移的目标。
     */
    private static final Map<OrderStatus, Set<OrderStatus>> ADMIN_TRANSITIONS;

    static {
        Map<OrderStatus, Set<OrderStatus>> m = new EnumMap<>(OrderStatus.class);
        m.put(OrderStatus.WAIT_BUYER_PAY,
                EnumSet.of(OrderStatus.TRADE_PAID_SUCCESS, OrderStatus.TRADE_CLOSED));
        m.put(OrderStatus.TRADE_PAID_SUCCESS,
                EnumSet.of(OrderStatus.TRADE_SHIPPED, OrderStatus.TRADE_CLOSED,
                        OrderStatus.TRADE_REFUNDING, OrderStatus.TRADE_REFUNDED));
        m.put(OrderStatus.TRADE_SHIPPED,
                EnumSet.of(OrderStatus.TRADE_COMPLETED, OrderStatus.TRADE_REFUNDING));
        m.put(OrderStatus.TRADE_REFUNDING,
                EnumSet.of(OrderStatus.TRADE_REFUNDED, OrderStatus.TRADE_PAID_SUCCESS));
        m.put(OrderStatus.TRADE_COMPLETED,
                EnumSet.of(OrderStatus.TRADE_REFUNDING));
        ADMIN_TRANSITIONS = Collections.unmodifiableMap(m);
    }

    @Autowired
    private OrderDAO orderDAO;

    @Autowired
    private ProductDetailService productDetailService;

    @Autowired
    private ProductService productService;

    @Autowired
    private UserService userService;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private StockLogService stockLogService;

    @Autowired
    private CommentDAO commentDAO;

    @Autowired
    private com.qinghe.mall.service.CouponService couponService;

    @Autowired
    private com.qinghe.mall.service.SettlementService settlementService;

    @Autowired
    private com.qinghe.mall.config.SnowflakeIdGenerator snowflakeIdGenerator;

    @Autowired
    @org.springframework.context.annotation.Lazy
    private com.qinghe.mall.service.SeckillService seckillService;

    @Autowired
    private com.qinghe.mall.config.OrderTimeoutQueue orderTimeoutQueue;

    @Override
    public Order createOrder(Order order) {
        return createOrder(order, null, null);
    }

    @Override
    public Order createOrder(Order order, String userCouponId, BigDecimal discountAmount) {
        // 单笔入口保持既有语义：事务内锁券 + 后端权威复算逐单比对（防伪造）
        return doCreateOrder(order, userCouponId, discountAmount, true, true);
    }

    /**
     * 下单核心（v1.8 拆分）：
     * @param lockCoupon     是否在事务内原子锁定用户券（购物车级多单分摊仅首单锁定，
     *                       user_coupon 单行 CAS 只能绑定一个单号，其余单以 coupon_id 关联追溯）
     * @param validateAmount 是否按「该笔原价复算比对」校验传入优惠额；
     *                       批量分摊场景由 batch 层以可核销合计口径统一校验，逐单分摊额不再复算
     */
    private Order doCreateOrder(Order order, String userCouponId, BigDecimal discountAmount,
                                boolean lockCoupon, boolean validateAmount) {
        String productDetailId = order.getProductDetailId();
        if (productDetailId == null) {
            throw new BusinessException("商品规格ID不能为空");
        }
        int quantity = order.getQuantity() != null && order.getQuantity() > 0 ? order.getQuantity() : 1;
        // 单笔购买数量上限（P2-7：防批量扫库存/异常大单）
        if (quantity > 99) {
            throw new BusinessException("单笔订单最多购买 99 件");
        }

        ProductDetail productDetail = productDetailService.findById(productDetailId);
        if (productDetail == null) {
            throw new BusinessException("商品规格不存在");
        }
        if (productDetail.getStock() < quantity) {
            throw new BusinessException("库存不足");
        }

        // 使用分布式锁保证同一规格的并发下单串行化
        String lockKey = "order:lock:" + productDetailId;
        Order created;
        try {
            RLock lock = redissonClient.getLock(lockKey);
            boolean locked = lock.tryLock(3, 5, TimeUnit.SECONDS);
            if (!locked) {
                throw new BusinessException("系统繁忙，请稍后重试");
            }
            try {
                // 扣库存 + 插订单放在同一事务中：任一失败整体回滚。
                // 事务通过 TransactionTemplate 在锁内提交（提交先于锁释放），
                // 保证后续请求能读到最新库存，避免「锁释放但事务未提交」的并发窗口。
                created = transactionTemplate.execute(status -> {
                    // 扣减前库存（用于流水留痕）
                    ProductDetail beforeDetail = productDetailService.findById(productDetailId);
                    int beforeStock = beforeDetail == null ? 0 : beforeDetail.getStock();

                    // 原子扣减库存（stock >= quantity 才生效），双重保障不超卖
                    boolean decreased = productDetailService.decreaseStock(productDetailId, quantity);
                    if (!decreased) {
                        throw new BusinessException("库存不足");
                    }

                    ProductDetail pd = productDetailService.findById(productDetailId);

                    // 生成订单号
                    String orderNumber = generateOrderNumber();

                    // 优惠券核销（仅当传入 userCouponId 时）：事务内原子锁定并改写实付金额。
                    // 商家归属先行解析（A1 券核销范围校验依赖 product.merchant_id）
                    BigDecimal originalTotal = pd.getPrice().multiply(BigDecimal.valueOf(quantity));
                    BigDecimal payable = originalTotal;
                    Long merchantId = null;
                    if (pd != null && StringUtils.isNotBlank(pd.getProductId())) {
                        Product product = productService.findById(pd.getProductId());
                        merchantId = product != null ? product.getMerchantId() : null;
                    }
                    if (StringUtils.isNotBlank(userCouponId)) {
                        if (discountAmount == null) {
                            throw new BusinessException("优惠金额缺失");
                        }
                        if (validateAmount) {
                            // A1：核销前二次权威校验（归属/未用/上架/时间窗/门槛/店铺归属一致性），
                            // 并以后端计算值为准比对传入优惠额，直接调用本方法也无法伪造优惠金额
                            BigDecimal computed = couponService.validateAndComputeDiscount(
                                    userCouponId, order.getUserId(), originalTotal, merchantId);
                            if (discountAmount.compareTo(computed) != 0) {
                                throw new BusinessException("优惠金额校验失败");
                            }
                        }
                        // 锁定用户券（CAS：仅本人未使用的券可锁定），并绑定订单号；
                        // 购物车级多单分摊（v1.8）仅首单执行锁定，其余单以 coupon_id 关联追溯
                        if (lockCoupon) {
                            couponService.lockCoupon(userCouponId, order.getUserId(), orderNumber);
                        }
                        payable = originalTotal.subtract(discountAmount);
                        if (payable.compareTo(BigDecimal.ZERO) < 0) {
                            payable = BigDecimal.ZERO;
                        }
                    }

                    OrderDO orderDO = new OrderDO();
                    // B2/v1.6：主键改雪花 BIGINT（order_number 仍为业务键）
                    orderDO.setId(snowflakeIdGenerator.nextId());
                    orderDO.setOrderNumber(orderNumber);
                    orderDO.setUserId(order.getUserId());
                    // 商家归属：由商品归属推导落库（NULL=平台自营，M6）
                    orderDO.setMerchantId(merchantId);
                    orderDO.setProductDetailId(productDetailId);
                    orderDO.setQuantity(quantity);
                    // 金额精确计算：实付 = 原价 - 优惠（优惠券将 totalPrice 改写为实付；
                    // 原价可由 totalPrice + discountAmount 还原，支付链路无需改动）
                    orderDO.setTotalPrice(payable);
                    orderDO.setCouponId(userCouponId);
                    // discount_amount 列为 NOT NULL，无券时显式置 0（避免绕过列默认值）
                    orderDO.setDiscountAmount(discountAmount != null ? discountAmount : BigDecimal.ZERO);
                    orderDO.setStatus(OrderStatus.WAIT_BUYER_PAY.name());
                    orderDO.setReceiverName(order.getReceiverName());
                    orderDO.setReceiverPhone(order.getReceiverPhone());
                    orderDO.setReceiverAddress(order.getReceiverAddress());
                    orderDO.setGmtCreated(new Date());
                    orderDO.setGmtModified(new Date());
                    orderDAO.insert(orderDO);

                    // 库存流水留痕（与扣库存同事务）
                    stockLogService.record(productDetailId, pd.getProductId(), orderNumber,
                            StockLogService.TYPE_ORDER_DEDUCT, -quantity, beforeStock, beforeStock - quantity);

                    Order result = orderDO.convertToModel();
                    result.setProductDetail(pd);
                    return result;
                });
            } finally {
                // 仅释放当前线程持有的锁；锁租期(5s)过期被他线程抢占后，
                // isHeldByCurrentThread() 为 false，跳过 unlock 避免 IllegalMonitorStateException
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
            // 事务已提交且锁已释放：延迟关单入队（超时未付自动关闭）
            orderTimeoutQueue.offer(created.getOrderNumber());
            return created;
        } catch (InterruptedException e) {
            // 恢复中断状态后包装抛出，并保留原始异常链路，便于排查
            Thread.currentThread().interrupt();
            throw new BusinessException("创建订单被中断", e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            // 保留原始异常 cause，不再吞掉堆栈
            throw new BusinessException("创建订单失败", e);
        }
    }

    @Override
    public List<Order> batchCreateOrders(List<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            throw new BusinessException("下单商品不能为空");
        }
        // 优惠券（v1.8 购物车级）：整批至多一张券；
        // 平台券按整批合计计算门槛并分摊到各单，店铺券仅分摊到本店订单（A1 归属口径不变）
        String userCouponId = null;
        Order couponOrder = null;
        for (Order o : orders) {
            if (StringUtils.isNotBlank(o.getCouponId())) {
                if (userCouponId != null) {
                    throw new BusinessException("一次结算仅可使用一张优惠券");
                }
                userCouponId = o.getCouponId();
                couponOrder = o;
            }
        }
        if (userCouponId == null) {
            List<Order> created = new ArrayList<>();
            for (Order order : orders) {
                created.add(createOrder(order));
            }
            return created;
        }

        // 解析每笔订单的规格原价与商家归属
        Map<Order, BigDecimal> originalTotals = new LinkedHashMap<>();
        Map<Order, Long> merchantIds = new LinkedHashMap<>();
        for (Order o : orders) {
            ProductDetail pd = productDetailService.findById(o.getProductDetailId());
            if (pd == null) {
                throw new BusinessException("商品规格不存在");
            }
            int qty = o.getQuantity() != null && o.getQuantity() > 0 ? o.getQuantity() : 1;
            originalTotals.put(o, pd.getPrice().multiply(BigDecimal.valueOf(qty)));
            Product product = productService.findById(pd.getProductId());
            merchantIds.put(o, product != null ? product.getMerchantId() : null);
        }

        // 可核销集合：平台券（merchant_id=NULL）为整批；店铺券仅本店订单（空集即跨店误用，拒绝）
        CouponDO coupon = couponService.findCouponByUserCouponId(userCouponId);
        if (coupon == null) {
            throw new BusinessException("优惠券不存在");
        }
        List<Order> eligible = new ArrayList<>();
        BigDecimal eligibleTotal = BigDecimal.ZERO;
        for (Order o : orders) {
            if (coupon.getMerchantId() == null || coupon.getMerchantId().equals(merchantIds.get(o))) {
                eligible.add(o);
                eligibleTotal = eligibleTotal.add(originalTotals.get(o));
            }
        }
        if (eligible.isEmpty()) {
            throw new BusinessException("店铺券仅可用于本店商品");
        }

        // 后端权威校验并按可核销合计计算总优惠额（归属/未用/上架/时间窗/门槛/店铺一致性）
        BigDecimal computedTotal = couponService.validateAndComputeDiscount(
                userCouponId, couponOrder.getUserId(), eligibleTotal, coupon.getMerchantId());
        // 防伪造：前端传入的合计优惠额（挂载在携券那笔）须与后端一致
        BigDecimal provided = couponOrder.getDiscountAmount();
        if (provided == null || provided.compareTo(computedTotal) != 0) {
            throw new BusinessException("优惠金额校验失败");
        }

        // 按比例分摊到各可核销订单（分币守恒：Σ分摊 == 总优惠额）
        List<BigDecimal> allocations = allocateDiscount(computedTotal, eligible, originalTotals);

        List<Order> created = new ArrayList<>();
        boolean couponLocked = false;
        for (Order o : orders) {
            int eligibleIdx = eligible.indexOf(o);
            if (eligibleIdx < 0) {
                // 店铺券批次中的非本店订单：不参与核销，按原价下单
                created.add(createOrder(o));
                continue;
            }
            // 首个可核销订单锁定用户券（user_coupon 单行 CAS 只能绑定一个单号）
            boolean lock = !couponLocked;
            couponLocked = true;
            created.add(doCreateOrder(o, userCouponId, allocations.get(eligibleIdx), lock, false));
        }
        return created;
    }

    /**
     * 优惠分摊（v1.8）：按各单原价占比把总优惠额拆到「分」。
     * ① Hamilton 最大余数法保证 Σ分摊 == 总优惠额（金额列 DECIMAL(10,2)，以分为最小单位）；
     * ② 单笔分摊若超过该笔原价（无门槛券 + 极小单的病态边界）则钳制，损失余额按最大余量逐分回填；
     * 总优惠额恒 ≤ 可核销合计（calculateDiscount 已按合计封顶），故回填必有可行解。
     */
    private List<BigDecimal> allocateDiscount(BigDecimal totalDiscount, List<Order> eligibleOrders,
                                              Map<Order, BigDecimal> originalTotals) {
        int n = eligibleOrders.size();
        if (n == 0) {
            return new ArrayList<>();
        }
        long totalCents = totalDiscount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
        long[] originalCents = new long[n];
        long originalSum = 0;
        for (int i = 0; i < n; i++) {
            originalCents[i] = originalTotals.get(eligibleOrders.get(i)).movePointRight(2)
                    .setScale(0, RoundingMode.HALF_UP).longValueExact();
            originalSum += originalCents[i];
        }
        // 防御：无原价合计或零优惠无从分摊，全零返回（业务路径不可达，v1.11 缺口补测发现的防御缺陷）
        if (originalSum == 0 || totalCents == 0) {
            List<BigDecimal> zeros = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                zeros.add(BigDecimal.ZERO);
            }
            return zeros;
        }
        long[] alloc = new long[n];
        long[] remainders = new long[n];
        long distributed = 0;
        for (int i = 0; i < n; i++) {
            long exact = totalCents * originalCents[i];
            alloc[i] = exact / originalSum;
            remainders[i] = exact % originalSum;
            distributed += alloc[i];
        }
        // Hamilton：余数大者优先 +1（并列取小索引，保证确定性）。
        // 数学上 leftover ≤ n-1 且每索引至多 +1，循环内 best 恒有解（v1.11 缺口补测的越界缺陷由此前置守卫根治）
        long leftover = totalCents - distributed;
        while (leftover > 0) {
            int best = -1;
            for (int i = 0; i < n; i++) {
                if (remainders[i] < 0) {
                    continue;
                }
                if (best < 0 || remainders[i] > remainders[best]) {
                    best = i;
                }
            }
            alloc[best]++;
            remainders[best] = -1;
            leftover--;
        }
        // 病态边界钳制：单笔分摊 ≤ 该笔原价，损失余额按最大余量逐分回填
        distributed = 0;
        for (int i = 0; i < n; i++) {
            if (alloc[i] > originalCents[i]) {
                alloc[i] = originalCents[i];
            }
            distributed += alloc[i];
        }
        while (distributed < totalCents) {
            int best = -1;
            long headroom = -1;
            for (int i = 0; i < n; i++) {
                long h = originalCents[i] - alloc[i];
                if (h > headroom) {
                    headroom = h;
                    best = i;
                }
            }
            if (best < 0 || headroom <= 0) {
                break;
            }
            alloc[best]++;
            distributed++;
        }
        List<BigDecimal> result = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            result.add(BigDecimal.valueOf(alloc[i], 2));
        }
        return result;
    }

    @Override
    public boolean cancelOrder(String orderNumber, Long userId) {
        if (StringUtils.isBlank(orderNumber)) {
            throw new BusinessException("订单号不能为空");
        }
        Order order = findByOrderNumber(orderNumber);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此订单");
        }
        return closeAndRestoreStock(orderNumber, StockLogService.TYPE_ORDER_RESTORE);
    }

    @Override
    public boolean closeExpiredOrder(String orderNumber) {
        return closeAndRestoreStock(orderNumber, StockLogService.TYPE_EXPIRE_RESTORE);
    }

    @Override
    public boolean shipOrder(String orderNumber) {
        if (StringUtils.isBlank(orderNumber)) {
            throw new IllegalArgumentException("订单号不能为空");
        }
        return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            int updated = orderDAO.updateStatusWithGuard(
                    orderNumber, OrderStatus.TRADE_PAID_SUCCESS.name(), OrderStatus.TRADE_SHIPPED.name());
            if (updated <= 0) {
                throw new BusinessException("订单状态异常，无法发货（仅已付款订单可发货）");
            }
            return true;
        }));
    }

    @Override
    public boolean confirmReceipt(String orderNumber, Long userId) {
        if (StringUtils.isBlank(orderNumber)) {
            throw new IllegalArgumentException("订单号不能为空");
        }
        Order order = findByOrderNumber(orderNumber);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此订单");
        }
        return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            int updated = orderDAO.updateStatusWithGuard(
                    orderNumber, OrderStatus.TRADE_SHIPPED.name(), OrderStatus.TRADE_COMPLETED.name());
            if (updated <= 0) {
                throw new BusinessException("订单状态异常，无法确认收货（仅已发货订单可确认）");
            }
            // A2：确认收货即商家货款分账（EARN，幂等；平台自营 merchant_id 为空自动跳过）
            OrderDO completed = orderDAO.findByOrderNumber(orderNumber);
            settlementService.recordEarning(completed);
            return true;
        }));
    }

    @Override
    public boolean applyRefund(String orderNumber, Long userId) {
        if (StringUtils.isBlank(orderNumber)) {
            throw new IllegalArgumentException("订单号不能为空");
        }
        Order order = findByOrderNumber(orderNumber);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此订单");
        }
        // 仅未发货的已付款订单可申请退款；已发货需走退货流程（本期未实现）
        return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            int updated = orderDAO.updateStatusWithGuard(
                    orderNumber, OrderStatus.TRADE_PAID_SUCCESS.name(), OrderStatus.TRADE_REFUNDING.name());
            if (updated <= 0) {
                throw new BusinessException("订单状态异常，无法申请退款（仅未发货的已付款订单可申请）");
            }
            return true;
        }));
    }

    @Override
    public boolean processRefund(String orderNumber, boolean approve) {
        if (StringUtils.isBlank(orderNumber)) {
            throw new IllegalArgumentException("订单号不能为空");
        }
        String target = approve ? OrderStatus.TRADE_REFUNDED.name() : OrderStatus.TRADE_PAID_SUCCESS.name();
        return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            int updated = orderDAO.updateStatusWithGuard(
                    orderNumber, OrderStatus.TRADE_REFUNDING.name(), target);
            if (updated <= 0) {
                throw new BusinessException("订单状态异常，无法处理退款（仅退款中订单可处理）");
            }
            // 拒绝退款（回退已付款）：释放被核销的优惠券，使其可再次使用
            if (!approve) {
                OrderDO od = orderDAO.findByOrderNumber(orderNumber);
                if (od != null && StringUtils.isNotBlank(od.getCouponId())) {
                    couponService.releaseCoupon(od.getCouponId());
                }
            }
            return true;
        }));
    }

    @Override
    public Paging<Order> listByMerchant(Long merchantId, String status, int pageNum, int pageSize) {
        if (merchantId == null) {
            throw new BusinessException("商家信息缺失");
        }
        if (pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize < 1 || pageSize > 50) {
            pageSize = 10;
        }
        com.github.pagehelper.Page<OrderDO> page = com.github.pagehelper.PageHelper.startPage(pageNum, pageSize)
                .doSelectPage(() -> orderDAO.findByMerchantId(merchantId, status));
        Paging<Order> paging = new Paging<>();
        paging.setPageNum(pageNum);
        paging.setPageSize(pageSize);
        paging.setTotalPage(page.getPages());
        paging.setTotalCount(page.getTotal());
        List<Order> orders = new ArrayList<>();
        for (OrderDO orderDO : page.getResult()) {
            orders.add(orderDO.convertToModel());
        }
        paging.setData(fillExtraBatch(orders));
        return paging;
    }

    @Override
    public boolean shipMerchantOrder(Long merchantId, String orderNumber) {
        assertMerchantOwnership(merchantId, orderNumber);
        return shipOrder(orderNumber);
    }

    @Override
    public boolean processMerchantRefund(Long merchantId, String orderNumber, boolean approve) {
        assertMerchantOwnership(merchantId, orderNumber);
        return processRefund(orderNumber, approve);
    }

    @Override
    public Map<String, Object> merchantStats(Long merchantId) {
        if (merchantId == null) {
            throw new BusinessException("商家信息缺失");
        }
        // 今日零点（本地时区）
        Date today = Date.from(java.time.LocalDate.now().atStartOfDay()
                .atZone(java.time.ZoneId.systemDefault()).toInstant());
        // 已支付口径：统一使用 OrderStatus.paidRevenueStatuses()（P1-7，与看板/日报一致）
        List<String> paidStatuses = OrderStatus.paidRevenueStatuses();
        // P2：GROUP BY 一次聚合替代循环 6 次 SUM（配合 (merchant_id,status,gmt_created) 联合索引）
        BigDecimal paidRevenue = sumAggregateAmount(
                orderDAO.sumByMerchantAndStatuses(merchantId, paidStatuses, null));
        BigDecimal todayRevenue = sumAggregateAmount(
                orderDAO.sumByMerchantAndStatuses(merchantId, paidStatuses, today));
        Map<String, Object> stats = new HashMap<>();
        stats.put("productCount", productService.queryMerchantPage(merchantId, null, null, 1, 1).getTotalCount());
        stats.put("orderCount", orderDAO.countByMerchantId(merchantId));
        stats.put("todayOrderCount", orderDAO.countByMerchantIdAndCreatedAfter(merchantId, today));
        stats.put("paidRevenue", paidRevenue);
        stats.put("todayRevenue", todayRevenue);
        return stats;
    }

    /** P2：聚合结果行（Map: amount）求和 */
    private BigDecimal sumAggregateAmount(List<Map<String, Object>> rows) {
        BigDecimal sum = BigDecimal.ZERO;
        if (rows == null) {
            return sum;
        }
        for (Map<String, Object> row : rows) {
            Object amount = row.get("amount");
            if (amount instanceof BigDecimal) {
                sum = sum.add((BigDecimal) amount);
            } else if (amount instanceof Number) {
                sum = sum.add(BigDecimal.valueOf(((Number) amount).doubleValue()));
            }
        }
        return sum;
    }

    /** 商家订单归属校验：订单必须属于该商家店铺 */
    private void assertMerchantOwnership(Long merchantId, String orderNumber) {
        if (merchantId == null) {
            throw new BusinessException("商家信息缺失");
        }
        if (StringUtils.isBlank(orderNumber)) {
            throw new IllegalArgumentException("订单号不能为空");
        }
        OrderDO orderDO = orderDAO.findByOrderNumber(orderNumber);
        if (orderDO == null) {
            throw new BusinessException("订单不存在");
        }
        if (!merchantId.equals(orderDO.getMerchantId())) {
            throw new BusinessException("无权操作其他店铺的订单");
        }
    }

    /**
     * 关闭待付款订单并回滚库存（事务内原子操作）。
     * 仅当订单仍为 WAIT_BUYER_PAY 时生效，避免并发下把已支付订单误关闭。
     *
     * @param changeType 库存流水变动类型（取消 ORDER_RESTORE / 超时 EXPIRE_RESTORE）
     */
    private boolean closeAndRestoreStock(String orderNumber, String changeType) {
        return transactionTemplate.execute(status -> {
            OrderDO orderDO = orderDAO.findByOrderNumber(orderNumber);
            if (orderDO == null) {
                throw new BusinessException("订单不存在");
            }
            if (!OrderStatus.WAIT_BUYER_PAY.name().equals(orderDO.getStatus())) {
                throw new BusinessException("订单状态异常，无法关闭");
            }
            // 原子更新：仅待付款 → 已关闭（防并发支付成功）
            int updated = orderDAO.updateStatusIfWaitPay(orderNumber, OrderStatus.TRADE_CLOSED.name());
            if (updated <= 0) {
                throw new BusinessException("订单状态已变化，请刷新后重试");
            }
            // 回滚库存
            int quantity = orderDO.getQuantity() != null && orderDO.getQuantity() > 0 ? orderDO.getQuantity() : 1;
            String detailId = orderDO.getProductDetailId();
            ProductDetail detail = productDetailService.findById(detailId);
            int beforeStock = detail == null ? 0 : detail.getStock();
            productDetailService.increaseStock(detailId, quantity);
            // 库存流水留痕（与回滚同事务）
            stockLogService.record(detailId, detail != null ? detail.getProductId() : null, orderNumber,
                    changeType, quantity, beforeStock, beforeStock + quantity);
            // 取消订单：释放被核销的优惠券（归属/已用由 CAS 守卫保障）。
            // 购物车级用券（v1.8）：仅当本单是最后一张持券在途单时才归还，防止整券误还
            if (StringUtils.isNotBlank(orderDO.getCouponId())
                    && orderDAO.countActiveByCouponExcluding(
                            orderDO.getCouponId(), orderDO.getUserId(), orderDO.getOrderNumber()) == 0) {
                couponService.releaseCoupon(orderDO.getCouponId());
            }
            // 秒杀订单回滚：仅当 seckill_order 为 CREATED 时恢复活动库存并置 CANCELLED
            seckillService.rollbackIfUnpaid(orderNumber);
            return true;
        });
    }

    @Override
    public Order findByOrderNumber(String orderNumber) {
        OrderDO orderDO = orderDAO.findByOrderNumber(orderNumber);
        if (orderDO == null) {
            return null;
        }
        Order order = orderDO.convertToModel();
        fillExtra(order);
        return order;
    }

    @Override
    public List<Order> findByUserIdAndStatus(Long userId, String status) {
        List<OrderDO> orderDOs = orderDAO.findByUserIdAndStatus(userId, status);
        List<Order> orders = new ArrayList<>();
        for (OrderDO orderDO : orderDOs) {
            orders.add(orderDO.convertToModel());
        }
        return fillExtraBatch(orders);
    }

    @Override
    public Paging<Order> findPageByUserIdAndStatus(Long userId, String status, Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1 || pageSize > 50) {
            pageSize = 10;
        }
        com.github.pagehelper.Page<OrderDO> page = com.github.pagehelper.PageHelper.startPage(pageNum, pageSize)
                .doSelectPage(() -> orderDAO.findByUserIdAndStatus(userId, status));

        Paging<Order> paging = new Paging<>();
        paging.setPageNum(pageNum);
        paging.setPageSize(pageSize);
        paging.setTotalPage(page.getPages());
        paging.setTotalCount(page.getTotal());

        List<Order> orders = new ArrayList<>();
        for (OrderDO orderDO : page.getResult()) {
            orders.add(orderDO.convertToModel());
        }
        paging.setData(fillExtraBatch(orders));
        return paging;
    }

    @Override
    public boolean updateOrderStatus(String orderNumber, String status) {
        if (StringUtils.isBlank(orderNumber)) {
            throw new IllegalArgumentException("订单号不能为空");
        }
        if (!OrderStatus.isValid(status)) {
            throw new IllegalArgumentException("订单状态不合法：" + status);
        }
        // P1-6：状态机守卫——先读当前状态，校验目标转移是否在白名单内，CAS 更新
        Order order = findByOrderNumber(orderNumber);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        OrderStatus from = order.getStatus();
        if (from == null) {
            throw new BusinessException("订单当前状态异常，无法变更");
        }
        OrderStatus to = OrderStatus.valueOf(status);
        Set<OrderStatus> allowed = ADMIN_TRANSITIONS.getOrDefault(from, Collections.emptySet());
        if (!allowed.contains(to)) {
            throw new BusinessException("不允许的状态变更：" + from + " → " + to);
        }
        // CAS：仅当前状态仍为 from 时才更新，防止与支付/发货/退款等并发流程互相覆盖
        int updated = orderDAO.updateStatusWithGuard(orderNumber, from.name(), to.name());
        if (updated <= 0) {
            throw new BusinessException("订单状态已变化，请刷新后重试");
        }
        return true;
    }

    @Override
    public boolean updateStatusIfWaitPay(String orderNumber, String targetStatus) {
        return orderDAO.updateStatusIfWaitPay(orderNumber, targetStatus) > 0;
    }

    @Override
    public java.util.List<java.util.Map<String, Object>> dailySalesReport(int days) {
        if (days < 1 || days > 90) {
            days = 7;
        }
        // P1-7：统一已付营收口径（已付款/已发货/已完成），与看板、商家统计一致
        return orderDAO.dailySalesReportByStatuses(days, OrderStatus.paidRevenueStatuses());
    }

    @Override
    public List<Order> findAll() {
        List<OrderDO> orderDOs = orderDAO.findAll();
        List<Order> orders = new ArrayList<>();
        for (OrderDO orderDO : orderDOs) {
            orders.add(orderDO.convertToModel());
        }
        return fillExtraBatch(orders);
    }

    @Override
    public Paging<Order> findAdminPage(Integer pagination, Integer pageSize, String status) {
        if (pagination == null || pagination < 1) {
            pagination = 1;
        }
        if (pageSize == null || pageSize < 1 || pageSize > 50) {
            pageSize = 20;
        }
        // P1-11：管理端列表分页查询，仅组装当页数据，避免全表捞取 OOM/超时
        com.github.pagehelper.Page<OrderDO> page =
                com.github.pagehelper.PageHelper.startPage(pagination, pageSize)
                        .doSelectPage(() -> orderDAO.queryAdminPage(status));

        Paging<Order> paging = new Paging<>();
        paging.setPageNum(pagination);
        paging.setPageSize(pageSize);
        paging.setTotalPage(page.getPages());
        paging.setTotalCount(page.getTotal());

        List<Order> orders = new ArrayList<>();
        for (OrderDO orderDO : page.getResult()) {
            orders.add(orderDO.convertToModel());
        }
        paging.setData(fillExtraBatch(orders));
        return paging;
    }

    @Override
    public long countAll() {
        return orderDAO.countAll();
    }

    @Override
    public BigDecimal sumTotalPriceByStatus(String status) {
        return orderDAO.sumTotalPriceByStatus(status);
    }

    @Override
    public BigDecimal sumTotalPriceByStatuses(List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = orderDAO.sumTotalPriceByStatuses(statuses);
        return sum == null ? BigDecimal.ZERO : sum;
    }

    @Override
    public List<Order> findExpiredWaitPay(int expireMinutes) {
        Date expireTime = new Date(System.currentTimeMillis() - expireMinutes * 60L * 1000L);
        List<OrderDO> orderDOs = orderDAO.findExpiredWaitPay(expireTime);
        List<Order> orders = new ArrayList<>();
        for (OrderDO orderDO : orderDOs) {
            Order order = orderDO.convertToModel();
            // 仅需 productDetailId + quantity 用于回滚，无需填充冗余信息
            orders.add(order);
        }
        return orders;
    }

    /**
     * 批量组装订单展示信息（消除 N+1）：
     * 一次性查询规格（IN）、商品（IN）、用户（IN）后按 ID 映射填充。
     */
    private List<Order> fillExtraBatch(List<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            return orders;
        }
        // 1) 批量查规格
        Set<String> detailIds = new LinkedHashSet<>();
        for (Order order : orders) {
            if (StringUtils.isNotBlank(order.getProductDetailId())) {
                detailIds.add(order.getProductDetailId());
            }
        }
        Map<String, ProductDetail> detailMap = new HashMap<>();
        if (!detailIds.isEmpty()) {
            for (ProductDetail pd : productDetailService.findByIds(new ArrayList<>(detailIds))) {
                detailMap.put(pd.getId(), pd);
            }
        }
        // 2) 批量查商品
        Set<String> productIds = new LinkedHashSet<>();
        for (ProductDetail pd : detailMap.values()) {
            if (pd != null && StringUtils.isNotBlank(pd.getProductId())) {
                productIds.add(pd.getProductId());
            }
        }
        Map<String, Product> productMap = new HashMap<>();
        if (!productIds.isEmpty()) {
            for (Product p : productService.findByIds(new ArrayList<>(productIds))) {
                productMap.put(p.getId(), p);
            }
        }
        // 3) 批量查用户
        Set<Long> userIds = new LinkedHashSet<>();
        for (Order order : orders) {
            if (order.getUserId() != null) {
                userIds.add(order.getUserId());
            }
        }
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (User u : userService.findByIds(new ArrayList<>(userIds))) {
                u.setPwd(null);
                userMap.put(u.getId(), u);
            }
        }
        // 4) 批量查已评价订单号（一次 IN）
        List<String> orderNumbers = new ArrayList<>();
        for (Order order : orders) {
            if (StringUtils.isNotBlank(order.getOrderNumber())) {
                orderNumbers.add(order.getOrderNumber());
            }
        }
        Set<String> commentedSet = new LinkedHashSet<>();
        if (!orderNumbers.isEmpty()) {
            commentedSet.addAll(commentDAO.findCommentedOrderNumbers(orderNumbers));
        }
        // 5) 组装
        for (Order order : orders) {
            ProductDetail pd = detailMap.get(order.getProductDetailId());
            order.setProductDetail(pd);
            if (pd != null) {
                Product product = productMap.get(pd.getProductId());
                if (product != null) {
                    order.setProductName(product.getName());
                    order.setProductImg(firstImg(product.getProductImgs()));
                }
            }
            order.setUser(userMap.get(order.getUserId()));
            order.setCommented(commentedSet.contains(order.getOrderNumber()));
        }
        return orders;
    }

    /**
     * 组装订单展示冗余信息：商品详情、商品名、商品首图、下单用户。
     * 均非落库字段，仅用于前端列表/详情直接展示。
     */
    private void fillExtra(Order order) {
        if (StringUtils.isNotBlank(order.getProductDetailId())) {
            ProductDetail productDetail = productDetailService.findById(order.getProductDetailId());
            order.setProductDetail(productDetail);
            if (productDetail != null && StringUtils.isNotBlank(productDetail.getProductId())) {
                Product product = productService.findById(productDetail.getProductId());
                if (product != null) {
                    order.setProductName(product.getName());
                    order.setProductImg(firstImg(product.getProductImgs()));
                }
            }
        }
        if (order.getUserId() != null) {
            User user = userService.findById(order.getUserId());
            if (user != null) {
                user.setPwd(null);
                order.setUser(user);
            }
        }
        order.setCommented(StringUtils.isNotBlank(order.getOrderNumber())
                && commentDAO.countByOrderNumber(order.getOrderNumber()) > 0);
    }

    /** 取图片串（空格或分号分隔）中的第一张图 */
    private String firstImg(String productImgs) {
        if (StringUtils.isBlank(productImgs)) {
            return null;
        }
        for (String part : productImgs.split("[;\\s]+")) {
            if (StringUtils.isNotBlank(part)) {
                return part.trim();
            }
        }
        return null;
    }

    private String generateOrderNumber() {
        // 使用 Redisson 的原子自增生成订单号
        long sequence = redissonClient.getAtomicLong("order:seq").incrementAndGet();
        return "QH" + System.currentTimeMillis() + String.format("%04d", sequence % 10000);
    }
}
