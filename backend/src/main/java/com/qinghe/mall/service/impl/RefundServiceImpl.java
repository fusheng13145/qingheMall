package com.qinghe.mall.service.impl;

import com.qinghe.mall.dao.OrderDAO;
import com.qinghe.mall.dao.RefundRequestDAO;
import com.qinghe.mall.dataobject.OrderDO;
import com.qinghe.mall.dataobject.RefundRequestDO;
import com.qinghe.mall.exception.BusinessException;
import com.qinghe.mall.model.OrderStatus;
import com.qinghe.mall.model.ProductDetail;
import com.qinghe.mall.model.RefundRequest;
import com.qinghe.mall.service.CouponService;
import com.qinghe.mall.service.ProductDetailService;
import com.qinghe.mall.service.RefundService;
import com.qinghe.mall.service.StockLogService;
import com.qinghe.mall.util.UUIDUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 退货退款服务实现（P2-18）。
 *
 * 申请：订单 CAS（多来源状态 → 退款中）与申请单落库同事务原子完成，
 * 并以「同一订单至多一条 PENDING」防重复申请。
 * 审核：申请单 CAS（仅 PENDING 可流转）与订单状态流转/库存回补同事务，
 * 任一步失败整体回滚，不会出现「申请已通过但订单未退款」的中间态。
 */
@Service
public class RefundServiceImpl implements RefundService {

    /** 允许发起退货退款的订单状态（已发货/已完成：货物在手，走退货流程） */
    private static final List<String> RETURN_REFUND_SOURCES = Arrays.asList(
            OrderStatus.TRADE_SHIPPED.name(), OrderStatus.TRADE_COMPLETED.name());

    @Autowired
    private OrderDAO orderDAO;

    @Autowired
    private RefundRequestDAO refundRequestDAO;

    @Autowired
    private ProductDetailService productDetailService;

    @Autowired
    private StockLogService stockLogService;

    @Autowired
    private CouponService couponService;

    @Autowired
    private com.qinghe.mall.service.SettlementService settlementService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Override
    public RefundRequest apply(String orderNumber, Long userId, String type, String reason) {
        if (StringUtils.isBlank(orderNumber)) {
            throw new IllegalArgumentException("订单号不能为空");
        }
        if (userId == null) {
            throw new BusinessException("请先登录");
        }
        String trimmedReason = reason == null ? "" : reason.trim();
        if (trimmedReason.isEmpty()) {
            throw new BusinessException("请填写退款原因");
        }
        if (trimmedReason.length() > 200) {
            throw new BusinessException("退款原因不能超过 200 字");
        }

        OrderDO order = orderDAO.findByOrderNumber(orderNumber);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!userId.equals(order.getUserId())) {
            throw new BusinessException("无权操作此订单");
        }

        // 类型推导与一致性校验：未发货只能仅退款，已发货/已完成走退货退款
        String currentStatus = order.getStatus();
        String resolvedType;
        List<String> casSources;
        if (OrderStatus.TRADE_PAID_SUCCESS.name().equals(currentStatus)) {
            resolvedType = RefundRequestDO.TYPE_REFUND_ONLY;
            casSources = java.util.Collections.singletonList(OrderStatus.TRADE_PAID_SUCCESS.name());
        } else if (RETURN_REFUND_SOURCES.contains(currentStatus)) {
            resolvedType = RefundRequestDO.TYPE_RETURN_REFUND;
            casSources = RETURN_REFUND_SOURCES;
        } else if (OrderStatus.TRADE_REFUNDING.name().equals(currentStatus)) {
            throw new BusinessException("该订单已有退款申请正在审核，请耐心等待");
        } else {
            throw new BusinessException("当前订单状态不支持申请退款");
        }
        if (StringUtils.isNotBlank(type) && !resolvedType.equals(type.trim())) {
            throw new BusinessException("该订单状态仅支持"
                    + (RefundRequestDO.TYPE_REFUND_ONLY.equals(resolvedType) ? "仅退款" : "退货退款") + "申请");
        }

        // 重复申请守卫（先查后插，唯一性由业务约束 + 事务内 CAS 共同保障）
        if (refundRequestDAO.findPendingByOrderNumber(orderNumber) != null) {
            throw new BusinessException("该订单已有待审核的退款申请，请勿重复提交");
        }

        RefundRequestDO requestDO = transactionTemplate.execute(status -> {
            // 多来源 CAS：仅订单仍处于允许来源状态时置为退款中，防并发状态漂移
            int updated = orderDAO.updateStatusWithGuardIn(
                    orderNumber, casSources, OrderStatus.TRADE_REFUNDING.name());
            if (updated <= 0) {
                throw new BusinessException("订单状态已变化，请刷新后重试");
            }
            Date now = new Date();
            RefundRequestDO req = new RefundRequestDO();
            req.setId(UUIDUtils.uuid());
            req.setOrderNumber(orderNumber);
            req.setUserId(userId);
            req.setType(resolvedType);
            req.setReason(trimmedReason);
            req.setStatus(RefundRequestDO.STATUS_PENDING);
            // 驳回回退依据：记录申请前的订单状态
            req.setPreviousOrderStatus(currentStatus);
            req.setGmtCreated(now);
            req.setGmtModified(now);
            refundRequestDAO.insert(req);
            return req;
        });
        return toModel(requestDO);
    }

    @Override
    public void review(String orderNumber, boolean approve, String reviewComment) {
        if (StringUtils.isBlank(orderNumber)) {
            throw new IllegalArgumentException("订单号不能为空");
        }
        String comment = reviewComment == null ? null : reviewComment.trim();
        if (comment != null && comment.length() > 200) {
            throw new BusinessException("审核意见不能超过 200 字");
        }
        if (comment != null && comment.isEmpty()) {
            comment = null;
        }

        RefundRequestDO pending = refundRequestDAO.findPendingByOrderNumber(orderNumber);
        if (pending != null) {
            final String finalComment = comment;
            transactionTemplate.execute(status -> {
                // 申请单 CAS：仅 PENDING 可流转，防并发重复审核
                String target = approve
                        ? RefundRequestDO.STATUS_APPROVED : RefundRequestDO.STATUS_REJECTED;
                int updated = refundRequestDAO.reviewWithGuard(pending.getId(), target, finalComment);
                if (updated <= 0) {
                    throw new BusinessException("该申请已被审核，请刷新后重试");
                }
                OrderDO order = orderDAO.findByOrderNumber(orderNumber);
                if (order == null) {
                    throw new BusinessException("订单不存在");
                }
                if (approve) {
                    finishRefundApproved(order);
                } else {
                    rollbackToPreviousStatus(order, pending.getPreviousOrderStatus());
                }
                return true;
            });
            return;
        }

        // 兼容通道：订单处于退款中但无申请单（历史管理端改单所致）。
        // 同样完成状态流转与库存回补，避免此类订单无法善后。
        OrderDO order = orderDAO.findByOrderNumber(orderNumber);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!OrderStatus.TRADE_REFUNDING.name().equals(order.getStatus())) {
            throw new BusinessException("该订单没有待审核的退款申请");
        }
        transactionTemplate.execute(status -> {
            if (approve) {
                finishRefundApproved(order);
            } else {
                // 无申请单时无法得知申请前状态，按最小影响回退到已付款
                rollbackToPreviousStatus(order, OrderStatus.TRADE_PAID_SUCCESS.name());
            }
            return true;
        });
    }

    @Override
    public List<RefundRequest> listByUser(Long userId) {
        if (userId == null) {
            throw new BusinessException("请先登录");
        }
        List<RefundRequest> result = new ArrayList<>();
        for (RefundRequestDO requestDO : refundRequestDAO.findByUserId(userId)) {
            result.add(toModel(requestDO));
        }
        return result;
    }

    @Override
    public List<RefundRequest> listByOrder(String orderNumber) {
        if (StringUtils.isBlank(orderNumber)) {
            throw new IllegalArgumentException("订单号不能为空");
        }
        List<RefundRequest> result = new ArrayList<>();
        for (RefundRequestDO requestDO : refundRequestDAO.findByOrderNumber(orderNumber)) {
            result.add(toModel(requestDO));
        }
        return result;
    }

    /**
     * 审核通过的收尾（须在事务内调用）：
     * 订单 退款中→已退款（CAS） + 库存回补 + 流水留痕 + 释放优惠券 + 分账冲销（A2）。
     */
    private void finishRefundApproved(OrderDO order) {
        int updated = orderDAO.updateStatusWithGuard(
                order.getOrderNumber(),
                OrderStatus.TRADE_REFUNDING.name(), OrderStatus.TRADE_REFUNDED.name());
        if (updated <= 0) {
            throw new BusinessException("订单状态已变化，无法完成退款");
        }
        restoreStockForRefund(order);
        // 订单已全额退款并关闭交易：归还优惠券（与取消订单口径一致）。
        // 购物车级用券（v1.8）：仅当本单是最后一张持券在途单时才归还，部分退款不还整券
        if (StringUtils.isNotBlank(order.getCouponId())
                && orderDAO.countActiveByCouponExcluding(
                        order.getCouponId(), order.getUserId(), order.getOrderNumber()) == 0) {
            couponService.releaseCoupon(order.getCouponId());
        }
        // A2：若该订单已确认收货分账（EARN 存在），退款通过即冲销商家货款（幂等；未分账订单自动跳过）
        settlementService.recordReversal(order);
    }

    /**
     * 审核驳回的回退（须在事务内调用）：订单 退款中→申请前状态。
     * 交易继续有效，优惠券保持已使用，不做释放。
     */
    private void rollbackToPreviousStatus(OrderDO order, String previousStatus) {
        String target = StringUtils.isNotBlank(previousStatus)
                ? previousStatus : OrderStatus.TRADE_PAID_SUCCESS.name();
        int updated = orderDAO.updateStatusWithGuard(
                order.getOrderNumber(), OrderStatus.TRADE_REFUNDING.name(), target);
        if (updated <= 0) {
            throw new BusinessException("订单状态已变化，无法完成审核");
        }
    }

    /** 退货库存回补 + REFUND_RESTORE 流水留痕（须在事务内调用） */
    private void restoreStockForRefund(OrderDO order) {
        int quantity = order.getQuantity() != null && order.getQuantity() > 0 ? order.getQuantity() : 1;
        String detailId = order.getProductDetailId();
        ProductDetail detail = productDetailService.findById(detailId);
        int beforeStock = detail == null ? 0 : detail.getStock();
        productDetailService.increaseStock(detailId, quantity);
        stockLogService.record(detailId, detail != null ? detail.getProductId() : null,
                order.getOrderNumber(), StockLogService.TYPE_REFUND_RESTORE,
                quantity, beforeStock, beforeStock + quantity);
    }

    private RefundRequest toModel(RefundRequestDO requestDO) {
        RefundRequest model = new RefundRequest();
        model.setId(requestDO.getId());
        model.setOrderNumber(requestDO.getOrderNumber());
        model.setUserId(requestDO.getUserId());
        model.setType(requestDO.getType());
        model.setReason(requestDO.getReason());
        model.setStatus(requestDO.getStatus());
        model.setReviewComment(requestDO.getReviewComment());
        model.setGmtCreated(requestDO.getGmtCreated());
        model.setGmtModified(requestDO.getGmtModified());
        return model;
    }
}
