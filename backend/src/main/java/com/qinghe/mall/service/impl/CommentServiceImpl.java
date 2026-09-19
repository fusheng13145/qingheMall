package com.qinghe.mall.service.impl;

import com.qinghe.mall.exception.BusinessException;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qinghe.mall.dao.CommentDAO;
import com.qinghe.mall.dao.CommentReplyDAO;
import com.qinghe.mall.dao.OrderDAO;
import com.qinghe.mall.dataobject.CommentDO;
import com.qinghe.mall.dataobject.CommentReplyDO;
import com.qinghe.mall.dataobject.OrderDO;
import com.qinghe.mall.model.Comment;
import com.qinghe.mall.model.OrderStatus;
import com.qinghe.mall.model.Paging;
import com.qinghe.mall.model.ProductDetail;
import com.qinghe.mall.model.User;
import com.qinghe.mall.service.CommentService;
import com.qinghe.mall.service.ProductDetailService;
import com.qinghe.mall.service.UserService;
import com.qinghe.mall.util.UUIDUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentDAO commentDAO;

    @Autowired
    private OrderDAO orderDAO;

    @Autowired
    private ProductDetailService productDetailService;

    @Autowired
    private UserService userService;

    @Autowired
    private com.qinghe.mall.service.ProductService productService;

    @Autowired
    private CommentReplyDAO commentReplyDAO;

    @Override
    public Comment addComment(Long userId, String productId, String orderNumber, Integer rating, String content) {
        if (StringUtils.isBlank(productId)) {
            throw new BusinessException("商品ID不能为空");
        }
        if (StringUtils.isBlank(orderNumber)) {
            throw new BusinessException("订单号不能为空");
        }
        if (rating == null || rating < 1 || rating > 5) {
            throw new BusinessException("评分必须为 1-5 星");
        }
        if (StringUtils.isBlank(content)) {
            throw new BusinessException("评价内容不能为空");
        }
        if (content.length() > 500) {
            throw new BusinessException("评价内容不能超过 500 字");
        }
        // 订单校验：存在、归属当前用户、已支付（P2：直查 OrderDAO 轻量校验，避免 fillExtra 冗余组装）
        OrderDO orderDO = orderDAO.findByOrderNumber(orderNumber);
        if (orderDO == null) {
            throw new BusinessException("订单不存在");
        }
        if (!orderDO.getUserId().equals(userId)) {
            throw new BusinessException("无权评价该订单");
        }
        if (!OrderStatus.TRADE_PAID_SUCCESS.name().equals(orderDO.getStatus())
                && !OrderStatus.TRADE_SHIPPED.name().equals(orderDO.getStatus())
                && !OrderStatus.TRADE_COMPLETED.name().equals(orderDO.getStatus())) {
            throw new BusinessException("仅已支付或已收货的订单可评价");
        }
        // 商品一致性：订单实际购买的商品必须与评价商品一致（防串评）
        if (StringUtils.isNotBlank(orderDO.getProductDetailId())) {
            ProductDetail detail = productDetailService.findById(orderDO.getProductDetailId());
            if (detail != null && !productId.equals(detail.getProductId())) {
                throw new BusinessException("评价商品与订单商品不一致");
            }
        }
        // 防重复评价（数据库唯一索引 uk_order_number 兜底）
        if (commentDAO.countByOrderNumber(orderNumber) > 0) {
            throw new BusinessException("该订单已评价");
        }

        CommentDO commentDO = new CommentDO();
        commentDO.setId(UUIDUtils.uuid());
        commentDO.setUserId(userId);
        commentDO.setProductId(productId);
        commentDO.setOrderNumber(orderNumber);
        commentDO.setRating(rating);
        commentDO.setContent(content.trim());
        commentDO.setGmtCreated(new Date());
        commentDO.setGmtModified(new Date());
        commentDAO.insert(commentDO);
        return commentDO.convertToModel();
    }

    @Override
    public Paging<Comment> listByProduct(String productId, Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1 || pageSize > 50) {
            pageSize = 10;
        }
        Page<CommentDO> page = PageHelper.startPage(pageNum, pageSize)
                .doSelectPage(() -> commentDAO.findByProductId(productId));

        Paging<Comment> paging = new Paging<>();
        paging.setPageNum(pageNum);
        paging.setPageSize(pageSize);
        paging.setTotalPage(page.getPages());
        paging.setTotalCount(page.getTotal());

        List<Comment> comments = new ArrayList<>();
        List<Long> userIds = new ArrayList<>();
        List<String> commentIds = new ArrayList<>();
        for (CommentDO commentDO : page.getResult()) {
            Comment comment = commentDO.convertToModel();
            comments.add(comment);
            commentIds.add(comment.getId());
            if (comment.getUserId() != null) {
                userIds.add(comment.getUserId());
            }
        }
        // 批量填充用户昵称（消除 N+1，P2-3）
        if (!userIds.isEmpty()) {
            Map<Long, User> userMap = new HashMap<>();
            for (User u : userService.findByIds(userIds)) {
                userMap.put(u.getId(), u);
            }
            for (Comment comment : comments) {
                User user = userMap.get(comment.getUserId());
                if (user != null) {
                    comment.setUserNickName(StringUtils.isNotBlank(user.getNickName())
                            ? user.getNickName() : user.getUserName());
                }
            }
        }
        // A4：批量填充商家回复（一次 IN 查询，消除 N+1）
        fillReplies(comments, commentIds);
        paging.setData(comments);
        return paging;
    }

    @Override
    public Map<String, Object> summary(String productId) {
        Map<String, Object> result = new HashMap<>();
        long count = commentDAO.countByProductId(productId);
        Double avg = commentDAO.avgRatingByProductId(productId);
        result.put("avgRating", avg == null ? 0 : Math.round(avg * 10) / 10.0);
        result.put("ratingCount", count);
        return result;
    }

    @Override
    public boolean hasCommented(String orderNumber) {
        if (StringUtils.isBlank(orderNumber)) {
            return false;
        }
        return commentDAO.countByOrderNumber(orderNumber) > 0;
    }

    // ===================== A4：商家评价回复（v1.5） =====================

    @Override
    public void merchantReply(Long merchantId, String commentId, String content) {
        if (merchantId == null) {
            throw new BusinessException("商家身份缺失");
        }
        if (StringUtils.isBlank(commentId)) {
            throw new BusinessException("评价ID不能为空");
        }
        String trimmed = StringUtils.isBlank(content) ? "" : content.trim();
        if (trimmed.isEmpty()) {
            throw new BusinessException("回复内容不能为空");
        }
        if (trimmed.length() > 200) {
            throw new BusinessException("回复内容不能超过 200 字");
        }
        CommentDO comment = commentDAO.findById(commentId);
        if (comment == null) {
            throw new BusinessException("评价不存在");
        }
        // 归属校验：仅本店商品的评价可回复（防跨店水平越权）
        com.qinghe.mall.model.Product product = productService.findById(comment.getProductId());
        if (product == null || product.getMerchantId() == null
                || !merchantId.equals(product.getMerchantId())) {
            throw new BusinessException("仅可回复本店商品的评价");
        }
        // 防重复回复（uk_comment_id 兜底）
        if (commentReplyDAO.findByCommentId(commentId) != null) {
            throw new BusinessException("该评价已回复");
        }
        CommentReplyDO reply = new CommentReplyDO();
        reply.setId(UUIDUtils.uuid());
        reply.setCommentId(commentId);
        reply.setMerchantId(merchantId);
        reply.setContent(trimmed);
        reply.setGmtCreated(new Date());
        commentReplyDAO.insert(reply);
    }

    @Override
    public Paging<Comment> listByMerchant(Long merchantId, int pageNum, int pageSize) {
        if (merchantId == null) {
            throw new BusinessException("商家身份缺失");
        }
        int pn = pageNum < 1 ? 1 : pageNum;
        int ps = (pageSize < 1 || pageSize > 50) ? 10 : pageSize;
        Page<CommentDO> page = PageHelper.startPage(pn, ps)
                .doSelectPage(() -> commentDAO.findByMerchant(merchantId));

        Paging<Comment> paging = new Paging<>();
        paging.setPageNum(pn);
        paging.setPageSize(ps);
        paging.setTotalPage(page.getPages());
        paging.setTotalCount(page.getTotal());

        List<Comment> comments = new ArrayList<>();
        List<String> commentIds = new ArrayList<>();
        for (CommentDO commentDO : page.getResult()) {
            comments.add(commentDO.convertToModel());
            commentIds.add(commentDO.getId());
        }
        fillReplies(comments, commentIds);
        paging.setData(comments);
        return paging;
    }

    /** 批量填充商家回复（commentIds 与 comments 一一对应） */
    void fillReplies(List<Comment> comments, List<String> commentIds) {
        if (comments.isEmpty() || commentIds.isEmpty()) {
            return;
        }
        applyReplies(comments, commentReplyDAO.findByCommentIds(commentIds));
    }

    /** 包可见：回复填充核心逻辑（可直测，绕开 PageHelper 纯 mock 限制） */
    static void applyReplies(List<Comment> comments, List<CommentReplyDO> replies) {
        if (comments.isEmpty() || replies == null || replies.isEmpty()) {
            return;
        }
        Map<String, CommentReplyDO> replyMap = new HashMap<>();
        for (CommentReplyDO reply : replies) {
            replyMap.put(reply.getCommentId(), reply);
        }
        for (Comment comment : comments) {
            CommentReplyDO reply = replyMap.get(comment.getId());
            if (reply != null) {
                comment.setReplyContent(reply.getContent());
                comment.setReplyTime(reply.getGmtCreated());
            }
        }
    }
}
