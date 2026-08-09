package com.qinghe.mall.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qinghe.mall.dao.CommentDAO;
import com.qinghe.mall.dao.OrderDAO;
import com.qinghe.mall.dataobject.CommentDO;
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

    @Override
    public Comment addComment(Long userId, String productId, String orderNumber, Integer rating, String content) {
        if (StringUtils.isBlank(productId)) {
            throw new RuntimeException("商品ID不能为空");
        }
        if (StringUtils.isBlank(orderNumber)) {
            throw new RuntimeException("订单号不能为空");
        }
        if (rating == null || rating < 1 || rating > 5) {
            throw new RuntimeException("评分必须为 1-5 星");
        }
        if (StringUtils.isBlank(content)) {
            throw new RuntimeException("评价内容不能为空");
        }
        if (content.length() > 500) {
            throw new RuntimeException("评价内容不能超过 500 字");
        }
        // 订单校验：存在、归属当前用户、已支付（P2：直查 OrderDAO 轻量校验，避免 fillExtra 冗余组装）
        OrderDO orderDO = orderDAO.findByOrderNumber(orderNumber);
        if (orderDO == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!orderDO.getUserId().equals(userId)) {
            throw new RuntimeException("无权评价该订单");
        }
        if (!OrderStatus.TRADE_PAID_SUCCESS.name().equals(orderDO.getStatus())
                && !OrderStatus.TRADE_SHIPPED.name().equals(orderDO.getStatus())
                && !OrderStatus.TRADE_COMPLETED.name().equals(orderDO.getStatus())) {
            throw new RuntimeException("仅已支付或已收货的订单可评价");
        }
        // 商品一致性：订单实际购买的商品必须与评价商品一致（防串评）
        if (StringUtils.isNotBlank(orderDO.getProductDetailId())) {
            ProductDetail detail = productDetailService.findById(orderDO.getProductDetailId());
            if (detail != null && !productId.equals(detail.getProductId())) {
                throw new RuntimeException("评价商品与订单商品不一致");
            }
        }
        // 防重复评价（数据库唯一索引 uk_order_number 兜底）
        if (commentDAO.countByOrderNumber(orderNumber) > 0) {
            throw new RuntimeException("该订单已评价");
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
        for (CommentDO commentDO : page.getResult()) {
            Comment comment = commentDO.convertToModel();
            comments.add(comment);
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
}
