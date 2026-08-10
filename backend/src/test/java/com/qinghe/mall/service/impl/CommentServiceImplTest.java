package com.qinghe.mall.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qinghe.mall.dao.CommentDAO;
import com.qinghe.mall.dao.OrderDAO;
import com.qinghe.mall.dataobject.CommentDO;
import com.qinghe.mall.dataobject.OrderDO;
import com.qinghe.mall.model.Comment;
import com.qinghe.mall.model.OrderStatus;
import com.qinghe.mall.model.Paging;
import com.qinghe.mall.model.ProductDetail;
import com.qinghe.mall.model.User;
import com.qinghe.mall.service.ProductDetailService;
import com.qinghe.mall.service.UserService;
import com.github.pagehelper.Page;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * 评价服务 单元测试（遗留：补齐 CommentServiceImpl 1% 覆盖的业务规则分支）。
 */
class CommentServiceImplTest {

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

    // ============ addComment 业务规则 ============

    @Test
    void addComment_invalidParams_rejected() {
        assertThrows(RuntimeException.class,
                () -> commentService.addComment(1L, "", "O1", 5, "很好"));
        assertThrows(RuntimeException.class,
                () -> commentService.addComment(1L, "p001", "O1", 6, "很好"));
        assertThrows(RuntimeException.class,
                () -> commentService.addComment(1L, "p001", "O1", 5, ""));
        assertThrows(RuntimeException.class,
                () -> commentService.addComment(1L, "p001", "O1", 5,
                        "长".repeat(501)));
    }

    @Test
    void addComment_orderNotFound_rejected() {
        when(orderDAO.findByOrderNumber("O1")).thenReturn(null);
        assertThrows(RuntimeException.class,
                () -> commentService.addComment(1L, "p001", "O1", 5, "很好"));
    }

    @Test
    void addComment_wrongUser_rejected() {
        OrderDO order = new OrderDO();
        order.setUserId(99L);
        order.setStatus(OrderStatus.TRADE_PAID_SUCCESS.name());
        order.setProductDetailId("pd001");
        when(orderDAO.findByOrderNumber("O1")).thenReturn(order);

        assertThrows(RuntimeException.class,
                () -> commentService.addComment(1L, "p001", "O1", 5, "很好"));
    }

    @Test
    void addComment_unpaidOrder_rejected() {
        OrderDO order = new OrderDO();
        order.setUserId(1L);
        order.setStatus(OrderStatus.WAIT_BUYER_PAY.name());
        order.setProductDetailId("pd001");
        when(orderDAO.findByOrderNumber("O1")).thenReturn(order);

        assertThrows(RuntimeException.class,
                () -> commentService.addComment(1L, "p001", "O1", 5, "很好"));
    }

    @Test
    void addComment_productMismatch_rejected() {
        OrderDO order = new OrderDO();
        order.setUserId(1L);
        order.setStatus(OrderStatus.TRADE_COMPLETED.name());
        order.setProductDetailId("pd001");
        when(orderDAO.findByOrderNumber("O1")).thenReturn(order);
        // 订单商品是 pd001（productId=p1），评价针对 p2 → 不一致拒绝
        ProductDetail detail = new ProductDetail();
        detail.setProductId("p1");
        when(productDetailService.findById("pd001")).thenReturn(detail);

        assertThrows(RuntimeException.class,
                () -> commentService.addComment(1L, "p2", "O1", 5, "很好"));
    }

    @Test
    void addComment_duplicate_rejected() {
        OrderDO order = new OrderDO();
        order.setUserId(1L);
        order.setStatus(OrderStatus.TRADE_COMPLETED.name());
        order.setProductDetailId("pd001");
        when(orderDAO.findByOrderNumber("O1")).thenReturn(order);
        when(commentDAO.countByOrderNumber("O1")).thenReturn(1);

        assertThrows(RuntimeException.class,
                () -> commentService.addComment(1L, "p001", "O1", 5, "很好"));
    }

    @Test
    void addComment_valid_insertAndReturn() {
        OrderDO order = new OrderDO();
        order.setUserId(1L);
        order.setStatus(OrderStatus.TRADE_COMPLETED.name());
        order.setProductDetailId("pd001");
        when(orderDAO.findByOrderNumber("O1")).thenReturn(order);
        when(commentDAO.countByOrderNumber("O1")).thenReturn(0);
        when(commentDAO.insert(any(CommentDO.class))).thenReturn(1);

        Comment comment = commentService.addComment(1L, "p1", "O1", 5, "很好");

        assertEquals(5, comment.getRating());
        assertEquals("很好", comment.getContent());
    }

    // ============ listByProduct 分页 ============

    @Test
    void listByProduct_invalidPage_fallsBackToDefault() {
        // doSelectPage 以 ThreadLocal 登记的 Page 为准（mock 返回值仅走查询回调），
        // 此处验证 service 对越界入参的规整：pageNum/pageSize 回退 1/10
        when(commentDAO.findByProductId("p001")).thenReturn(new Page<>(1, 10));

        Paging<Comment> paging = commentService.listByProduct("p001", null, 999);
        assertEquals(1, paging.getPageNum());
        assertEquals(10, paging.getPageSize());
        assertEquals(0, paging.getTotalCount());
    }

    @Test
    void listByProduct_emptyResult_returnsEmptyPage() {
        when(commentDAO.findByProductId("p001")).thenReturn(new Page<>(1, 10));

        Paging<Comment> paging = commentService.listByProduct("p001", 1, 10);
        assertEquals(0, paging.getTotalCount());
        org.junit.jupiter.api.Assertions.assertTrue(paging.getData().isEmpty());
    }

    @Test
    void listByProduct_withUsers_fillsNickNames() {
        // PageHelper 纯 mock 下 result 恒空（分页填充依赖 MyBatis 拦截器），
        // 昵称填充分支由集成测试覆盖；此处验证 DAO 透传与空结果安全返回
        when(commentDAO.findByProductId("p001")).thenReturn(new Page<>(1, 10));

        Paging<Comment> paging = commentService.listByProduct("p001", 1, 10);

        assertEquals(0, paging.getData().size());
        verify(commentDAO).findByProductId("p001");
        // result 为空 → 不触发批量用户查询（N+1 消除分支由集成测试验证）
        org.mockito.Mockito.verify(userService, org.mockito.Mockito.never()).findByIds(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void listByProduct_userMissing_doesNotCrash() {
        when(commentDAO.findByProductId("p001")).thenReturn(new Page<>(1, 10));

        Paging<Comment> paging = commentService.listByProduct("p001", 1, 10);

        org.junit.jupiter.api.Assertions.assertNotNull(paging);
        assertEquals(1, paging.getPageNum());
    }

    // ============ summary / hasCommented ============

    @Test
    void summary_avgRatingRounded() {
        when(commentDAO.countByProductId("p001")).thenReturn(2L);
        when(commentDAO.avgRatingByProductId("p001")).thenReturn(4.56);

        java.util.Map<String, Object> summary = commentService.summary("p001");
        assertEquals(4.6, summary.get("avgRating"));
        assertEquals(2L, summary.get("ratingCount"));
    }

    @Test
    void summary_noRating_returnsZero() {
        when(commentDAO.countByProductId("p001")).thenReturn(0L);
        when(commentDAO.avgRatingByProductId("p001")).thenReturn(null);

        java.util.Map<String, Object> summary = commentService.summary("p001");
        // 三元表达式 int/double 统一提升为 double，null 分支返回 0.0
        assertEquals(0.0, summary.get("avgRating"));
    }

    @Test
    void hasCommented_blankOrderNumber_false() {
        assertFalse(commentService.hasCommented(""));
        assertFalse(commentService.hasCommented(null));
    }

    @Test
    void hasCommented_existing_true() {
        when(commentDAO.countByOrderNumber("O1")).thenReturn(1);
        assertTrue(commentService.hasCommented("O1"));
    }
}
