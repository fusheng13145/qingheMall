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
}
