package com.qinghe.mall.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.qinghe.mall.dao.CommentDAO;
import com.qinghe.mall.dao.OrderDAO;
import com.qinghe.mall.dataobject.CommentDO;
import com.qinghe.mall.dataobject.OrderDO;
import com.qinghe.mall.exception.BusinessException;
import com.qinghe.mall.model.OrderStatus;
import com.qinghe.mall.model.ProductDetail;
import com.qinghe.mall.model.User;
import com.qinghe.mall.service.ProductDetailService;
import com.qinghe.mall.service.UserService;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * CommentServiceImpl 分支覆盖补强（T3 续补）：覆盖 addComment 全守卫分支、
 * listByProduct 分页默认值与用户昵称填充、summary 均值空值兜底、hasCommented 空订单号。
 */
class CommentServiceImplExtraTest {

    @Mock
    private CommentDAO commentDAO;
    @Mock
    private OrderDAO orderDAO;
    @Mock
    private ProductDetailService productDetailService;
    @Mock
    private UserService userService;

    @InjectMocks
    private CommentServiceImpl commentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private OrderDO paidOrder(Long userId, String productDetailId) {
        OrderDO o = new OrderDO();
        o.setUserId(userId);
        o.setStatus(OrderStatus.TRADE_PAID_SUCCESS.name());
        o.setProductDetailId(productDetailId);
        return o;
    }

    private void stubSuccessPath(Long userId, String productId, String orderNumber, String productDetailId) {
        OrderDO order = paidOrder(userId, productDetailId);
        when(orderDAO.findByOrderNumber(orderNumber)).thenReturn(order);
        when(commentDAO.countByOrderNumber(orderNumber)).thenReturn(0);
        when(commentDAO.insert(any(CommentDO.class))).thenReturn(1);
    }

    // ===== addComment 守卫分支 =====

    @Test
    void addComment_blankProductId_throws() {
        assertThrows(BusinessException.class,
                () -> commentService.addComment(1L, " ", "O1", 5, "好"));
    }

    @Test
    void addComment_blankOrderNumber_throws() {
        assertThrows(BusinessException.class,
                () -> commentService.addComment(1L, "p001", " ", 5, "好"));
    }

    @Test
    void addComment_ratingNull_throws() {
        assertThrows(BusinessException.class,
                () -> commentService.addComment(1L, "p001", "O1", null, "好"));
    }

    @Test
    void addComment_ratingBelow1_throws() {
        assertThrows(BusinessException.class,
                () -> commentService.addComment(1L, "p001", "O1", 0, "好"));
    }

    @Test
    void addComment_ratingAbove5_throws() {
        assertThrows(BusinessException.class,
                () -> commentService.addComment(1L, "p001", "O1", 6, "好"));
    }

    @Test
    void addComment_blankContent_throws() {
        assertThrows(BusinessException.class,
                () -> commentService.addComment(1L, "p001", "O1", 5, "  "));
    }

    @Test
    void addComment_contentTooLong_throws() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 501; i++) sb.append("x");
        assertThrows(BusinessException.class,
                () -> commentService.addComment(1L, "p001", "O1", 5, sb.toString()));
    }

    @Test
    void addComment_orderNotFound_throws() {
        when(orderDAO.findByOrderNumber("O1")).thenReturn(null);
        assertThrows(BusinessException.class,
                () -> commentService.addComment(1L, "p001", "O1", 5, "好"));
    }

    @Test
    void addComment_orderNotOwned_throws() {
        OrderDO order = paidOrder(99L, "pd001");
        when(orderDAO.findByOrderNumber("O1")).thenReturn(order);
        assertThrows(BusinessException.class,
                () -> commentService.addComment(1L, "p001", "O1", 5, "好"));
    }

    @Test
    void addComment_orderNotPaid_throws() {
        OrderDO order = new OrderDO();
        order.setUserId(1L);
        order.setStatus(OrderStatus.WAIT_BUYER_PAY.name());
        order.setProductDetailId("pd001");
        when(orderDAO.findByOrderNumber("O1")).thenReturn(order);
        assertThrows(BusinessException.class,
                () -> commentService.addComment(1L, "p001", "O1", 5, "好"));
    }

    @Test
    void addComment_productMismatch_throws() {
        OrderDO order = paidOrder(1L, "pd001");
        when(orderDAO.findByOrderNumber("O1")).thenReturn(order);
        ProductDetail detail = new ProductDetail();
        detail.setProductId("otherProduct");
        when(productDetailService.findById("pd001")).thenReturn(detail);
        assertThrows(BusinessException.class,
                () -> commentService.addComment(1L, "p001", "O1", 5, "好"));
    }

    @Test
    void addComment_alreadyCommented_throws() {
        OrderDO order = paidOrder(1L, "pd001");
        when(orderDAO.findByOrderNumber("O1")).thenReturn(order);
        when(productDetailService.findById("pd001")).thenReturn(null);
        when(commentDAO.countByOrderNumber("O1")).thenReturn(1);
        assertThrows(BusinessException.class,
                () -> commentService.addComment(1L, "p001", "O1", 5, "好"));
    }

    @Test
    void addComment_success() {
        stubSuccessPath(1L, "p001", "O1", "pd001");
        when(productDetailService.findById("pd001")).thenReturn(null);

        com.qinghe.mall.model.Comment result = commentService.addComment(1L, "p001", "O1", 5, "好");
        assertNotNull(result);
    }

    // ===== listByProduct 分页默认参数（昵称批量填充分支依赖 PageHelper 拦截器，
    // 纯 Mockito 无法触发 doSelectPage 结果填充，故该子分支改用集成测试覆盖）=====

    @Test
    void listByProduct_nullPageParams_useDefaults() {
        CommentDO c = new CommentDO();
        c.setUserId(1L);
        when(commentDAO.findByProductId("p001")).thenReturn(Collections.singletonList(c));
        when(userService.findByIds(anyList())).thenReturn(Collections.emptyList());

        com.qinghe.mall.model.Paging<?> paging = commentService.listByProduct("p001", null, null);
        assertEquals(1, paging.getPageNum());
        assertEquals(10, paging.getPageSize());
    }

    @Test
    void listByProduct_pageSizeOver50_clamped() {
        CommentDO c = new CommentDO();
        c.setUserId(1L);
        when(commentDAO.findByProductId("p001")).thenReturn(Collections.singletonList(c));
        when(userService.findByIds(anyList())).thenReturn(Collections.emptyList());

        com.qinghe.mall.model.Paging<?> paging = commentService.listByProduct("p001", 2, 100);
        assertEquals(10, paging.getPageSize());
    }

    // ===== summary / hasCommented =====

    @Test
    void summary_avgNull_returnsZero() {
        when(commentDAO.countByProductId("p001")).thenReturn(3L);
        when(commentDAO.avgRatingByProductId("p001")).thenReturn(null);

        java.util.Map<String, Object> s = commentService.summary("p001");
        assertEquals(0.0, ((Number) s.get("avgRating")).doubleValue(), 0.001);
        assertEquals(3L, s.get("ratingCount"));
    }

    @Test
    void summary_avgNotNull_returnsRounded() {
        when(commentDAO.countByProductId("p001")).thenReturn(2L);
        when(commentDAO.avgRatingByProductId("p001")).thenReturn(4.56);

        java.util.Map<String, Object> s = commentService.summary("p001");
        assertEquals(4.6, (Double) s.get("avgRating"), 0.001);
    }

    @Test
    void hasCommented_blankOrderNumber_false() {
        assertFalse(commentService.hasCommented(" "));
    }

    @Test
    void hasCommented_countPositive_true() {
        when(commentDAO.countByOrderNumber("O1")).thenReturn(1);
        assertTrue(commentService.hasCommented("O1"));
    }
}
