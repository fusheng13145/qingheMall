package com.qinghe.mall.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.qinghe.mall.dao.CommentDAO;
import com.qinghe.mall.dao.OrderDAO;
import com.qinghe.mall.dataobject.OrderDO;
import com.qinghe.mall.exception.BusinessException;
import com.qinghe.mall.model.Comment;
import com.qinghe.mall.model.Paging;
import com.qinghe.mall.model.ProductDetail;
import com.qinghe.mall.service.ProductDetailService;
import com.qinghe.mall.service.UserService;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * CommentServiceImpl 分支覆盖补测（#37）：覆盖 addComment 缺失守卫（评分上限、内容超长、商品串评）、
 * listByProduct 分页归一化、hasCommented 空参分支。PageHelper 结果集循环体分支在纯 Mockito 下不可达，
 * 仅覆盖分页归一化逻辑（位于 PageHelper 调用之前）。
 */
class CommentServiceImplCoverageTest {

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
        when(commentDAO.findByProductId(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Collections.emptyList());
    }

    private OrderDO paidOrder(String productDetailId) {
        OrderDO o = new OrderDO();
        o.setUserId(1L);
        o.setStatus("TRADE_PAID_SUCCESS");
        o.setProductDetailId(productDetailId);
        return o;
    }

    // ========== addComment 守卫 ==========

    @Test
    void addComment_ratingTooHigh_shouldThrow() {
        when(orderDAO.findByOrderNumber("QH1")).thenReturn(paidOrder("pd1"));
        when(commentDAO.countByOrderNumber("QH1")).thenReturn(0);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> commentService.addComment(1L, "p1", "QH1", 6, "很好"));
        assertEquals("评分必须为 1-5 星", ex.getMessage());
    }

    @Test
    void addComment_contentTooLong_shouldThrow() {
        when(orderDAO.findByOrderNumber("QH1")).thenReturn(paidOrder("pd1"));
        when(commentDAO.countByOrderNumber("QH1")).thenReturn(0);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 501; i++) sb.append("a");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> commentService.addComment(1L, "p1", "QH1", 5, sb.toString()));
        assertEquals("评价内容不能超过 500 字", ex.getMessage());
    }

    @Test
    void addComment_productMismatch_shouldThrow() {
        OrderDO o = paidOrder("pd1");
        when(orderDAO.findByOrderNumber("QH1")).thenReturn(o);
        when(commentDAO.countByOrderNumber("QH1")).thenReturn(0);
        ProductDetail detail = new ProductDetail();
        detail.setId("pd1");
        detail.setProductId("otherProduct");
        when(productDetailService.findById("pd1")).thenReturn(detail);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> commentService.addComment(1L, "p1", "QH1", 5, "很好"));
        assertEquals("评价商品与订单商品不一致", ex.getMessage());
    }

    @Test
    void addComment_valid_shouldInsert() {
        when(orderDAO.findByOrderNumber("QH1")).thenReturn(paidOrder("pd1"));
        when(commentDAO.countByOrderNumber("QH1")).thenReturn(0);
        when(productDetailService.findById("pd1")).thenReturn(null);
        Comment c = commentService.addComment(1L, "p1", "QH1", 5, "很好");
        assertTrue(c != null);
    }

    // ========== listByProduct 分页归一化 ==========

    @Test
    void listByProduct_nullPagination_shouldDefault() {
        Paging<Comment> p = commentService.listByProduct("p1", null, null);
        assertEquals(1, p.getPageNum());
        assertEquals(10, p.getPageSize());
    }

    @Test
    void listByProduct_lowBounds_shouldDefault() {
        Paging<Comment> p = commentService.listByProduct("p1", 0, 0);
        assertEquals(1, p.getPageNum());
        assertEquals(10, p.getPageSize());
    }

    @Test
    void listByProduct_highBounds_shouldClamp() {
        Paging<Comment> p = commentService.listByProduct("p1", -3, 100);
        assertEquals(1, p.getPageNum());
        assertEquals(10, p.getPageSize());
    }

    @Test
    void listByProduct_normal_shouldKeep() {
        Paging<Comment> p = commentService.listByProduct("p1", 2, 20);
        assertEquals(2, p.getPageNum());
        assertEquals(20, p.getPageSize());
    }

    // ========== hasCommented ==========

    @Test
    void hasCommented_blank_shouldReturnFalse() {
        assertFalse(commentService.hasCommented("   "));
    }

    @Test
    void hasCommented_none_shouldReturnFalse() {
        when(commentDAO.countByOrderNumber("QH1")).thenReturn(0);
        assertFalse(commentService.hasCommented("QH1"));
    }

    @Test
    void hasCommented_existing_shouldReturnTrue() {
        when(commentDAO.countByOrderNumber("QH1")).thenReturn(1);
        assertTrue(commentService.hasCommented("QH1"));
    }
}
