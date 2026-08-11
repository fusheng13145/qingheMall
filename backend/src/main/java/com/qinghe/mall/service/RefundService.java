package com.qinghe.mall.service;

import com.qinghe.mall.model.RefundRequest;
import java.util.List;

/**
 * 退货退款服务（P2-18）：申请留痕 + 审核流转 + 库存回补。
 *
 * 申请来源（类型由订单状态决定）：
 * - 已付款（未发货）  → REFUND_ONLY 仅退款
 * - 已发货 / 已完成   → RETURN_REFUND 退货退款
 *
 * 审核语义（均在同一事务内原子完成）：
 * - 通过：订单 退款中→已退款（终态） + 库存回补（REFUND_RESTORE 流水） + 释放优惠券
 * - 驳回：订单 退款中→申请前状态（previous_order_status 回退）
 */
public interface RefundService {

    /**
     * 用户发起退款/退货申请：同一订单至多一条待审核申请（防重复），
     * 订单 CAS 置为退款中并落 previous_order_status。
     *
     * @param orderNumber 订单号
     * @param userId      申请人（须为订单归属用户）
     * @param type        申请类型（可空：按订单状态自动推导）
     * @param reason      退款原因（必填，不超过 200 字）
     */
    RefundRequest apply(String orderNumber, Long userId, String type, String reason);

    /**
     * 审核退款申请（商家/管理员，归属校验由调用方完成）：
     * 存在待审核申请走申请单 CAS 审核；订单处于退款中但无申请单时（历史管理端改单场景）
     * 走兼容通道，同样完成库存回补。
     *
     * @param orderNumber   订单号
     * @param approve       true 通过 / false 驳回
     * @param reviewComment 审核意见（驳回时建议填写，可空）
     */
    void review(String orderNumber, boolean approve, String reviewComment);

    /** 当前用户的申请列表（倒序，最多 100 条） */
    List<RefundRequest> listByUser(Long userId);

    /** 指定订单的申请历史（倒序） */
    List<RefundRequest> listByOrder(String orderNumber);
}
