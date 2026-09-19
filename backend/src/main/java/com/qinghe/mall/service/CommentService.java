package com.qinghe.mall.service;

import com.qinghe.mall.model.Comment;
import com.qinghe.mall.model.Paging;
import java.util.Map;

public interface CommentService {

    /**
     * 提交评价：校验订单归属当前用户且已支付、该订单未评价、评分 1-5。
     * 每个订单仅可评价一次（order_number 唯一约束兜底）。
     */
    Comment addComment(Long userId, String productId, String orderNumber, Integer rating, String content);

    /** 商品评价列表（分页，最新在前，含评价人昵称） */
    Paging<Comment> listByProduct(String productId, Integer pageNum, Integer pageSize);

    /** 商品评分汇总：{ avgRating, ratingCount } */
    Map<String, Object> summary(String productId);

    /** 某订单是否已评价（订单中心评价按钮状态用） */
    boolean hasCommented(String orderNumber);

    /**
     * 商家回复评价（A4，v1.5）：仅本店商品的评价可回复，一对一（重复回复拒绝）。
     * 归属校验：comment.product_id → product.merchant_id 必须等于当前商家。
     */
    void merchantReply(Long merchantId, String commentId, String content);

    /** 本店商品的评价分页（商家端评价管理用，含商品名与回复状态） */
    Paging<Comment> listByMerchant(Long merchantId, int pageNum, int pageSize);
}
