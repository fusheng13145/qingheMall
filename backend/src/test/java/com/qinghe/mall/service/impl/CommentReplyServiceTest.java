package com.qinghe.mall.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qinghe.mall.dao.CommentDAO;
import com.qinghe.mall.dao.CommentReplyDAO;
import com.qinghe.mall.dataobject.CommentDO;
import com.qinghe.mall.dataobject.CommentReplyDO;
import com.qinghe.mall.model.Comment;
import com.qinghe.mall.model.Product;
import com.qinghe.mall.service.ProductService;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 评价回复单元测试（A4，v1.5）。
 *
 * 覆盖：merchantReply 归属校验（本店放行/跨店拒绝/平台商品拒绝/商品缺失拒绝）、
 * 参数校验（空内容/超长/空 ID/评价缺失）、防重复回复、listByMerchant 分页参数收敛。
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class CommentReplyServiceTest {

    @Mock
    private CommentDAO commentDAO;

    @Mock
    private CommentReplyDAO commentReplyDAO;

    @Mock
    private com.qinghe.mall.dao.OrderDAO orderDAO;

    @Mock
    private com.qinghe.mall.service.ProductDetailService productDetailService;

    @Mock
    private ProductService productService;

    @Mock
    private com.qinghe.mall.service.UserService userService;

    @InjectMocks
    private CommentServiceImpl commentService;

    private CommentDO comment;

    @BeforeEach
    void setUp() {
        comment = new CommentDO();
        comment.setId("C1");
        comment.setUserId(1L);
        comment.setProductId("p001");
        comment.setOrderNumber("QH1");
        comment.setRating(5);
        comment.setContent("很好");
        comment.setGmtCreated(new Date());
        comment.setGmtModified(new Date());
    }

    private Product product(Long merchantId) {
        Product p = new Product();
        p.setId("p001");
        p.setMerchantId(merchantId);
        return p;
    }

    @Test
    @DisplayName("merchantReply 本店商品评价：放行并落库")
    void merchantReply_ownShop_ok() {
        when(commentDAO.findById("C1")).thenReturn(comment);
        when(productService.findById("p001")).thenReturn(product(5L));
        when(commentReplyDAO.findByCommentId("C1")).thenReturn(null);

        commentService.merchantReply(5L, "C1", "  感谢支持，欢迎再次购买！  ");

        ArgumentCaptor<CommentReplyDO> captor = ArgumentCaptor.forClass(CommentReplyDO.class);
        verify(commentReplyDAO).insert(captor.capture());
        assertEquals("C1", captor.getValue().getCommentId());
        assertEquals(5L, captor.getValue().getMerchantId());
        assertEquals("感谢支持，欢迎再次购买！", captor.getValue().getContent()); // trim
    }

    @Test
    @DisplayName("merchantReply 跨店商品/平台自营商品/商品缺失：拒绝")
    void merchantReply_ownershipGuards() {
        when(commentDAO.findById("C1")).thenReturn(comment);

        // 他店商品
        when(productService.findById("p001")).thenReturn(product(9L));
        assertThrows(RuntimeException.class, () -> commentService.merchantReply(5L, "C1", "回复"));
        // 平台自营（merchant_id NULL）
        when(productService.findById("p001")).thenReturn(product(null));
        assertThrows(RuntimeException.class, () -> commentService.merchantReply(5L, "C1", "回复"));
        // 商品已被删除
        when(productService.findById("p001")).thenReturn(null);
        assertThrows(RuntimeException.class, () -> commentService.merchantReply(5L, "C1", "回复"));

        verify(commentReplyDAO, never()).insert(any(CommentReplyDO.class));
    }

    @Test
    @DisplayName("merchantReply 已回复拒绝（防重复）")
    void merchantReply_duplicateRejected() {
        when(commentDAO.findById("C1")).thenReturn(comment);
        when(productService.findById("p001")).thenReturn(product(5L));
        when(commentReplyDAO.findByCommentId("C1")).thenReturn(new CommentReplyDO());

        assertThrows(RuntimeException.class, () -> commentService.merchantReply(5L, "C1", "回复"));
        verify(commentReplyDAO, never()).insert(any(CommentReplyDO.class));
    }

    @Test
    @DisplayName("merchantReply 参数守卫：空商家/空评价ID/评价不存在/空内容/超长")
    void merchantReply_paramGuards() {
        assertThrows(RuntimeException.class, () -> commentService.merchantReply(null, "C1", "回复"));
        assertThrows(RuntimeException.class, () -> commentService.merchantReply(5L, "", "回复"));
        assertThrows(RuntimeException.class, () -> commentService.merchantReply(5L, "C1", "   "));

        when(commentDAO.findById("C404")).thenReturn(null);
        assertThrows(RuntimeException.class, () -> commentService.merchantReply(5L, "C404", "回复"));

        when(commentDAO.findById("C1")).thenReturn(comment);
        when(productService.findById("p001")).thenReturn(product(5L));
        when(commentReplyDAO.findByCommentId(anyString())).thenReturn(null);
        assertThrows(RuntimeException.class, () -> commentService.merchantReply(5L, "C1", "长".repeat(201)));
        verify(commentReplyDAO, never()).insert(any(CommentReplyDO.class));
    }

    @Test
    @DisplayName("listByMerchant 分页参数收敛并透传 DAO；空结果安全返回")
    void listByMerchant_clampsAndDelegates() {
        when(commentDAO.findByMerchant(5L)).thenReturn(List.of());

        var paging = commentService.listByMerchant(5L, 0, 999);

        assertEquals(1, paging.getPageNum());
        assertEquals(10, paging.getPageSize());
        assertEquals(0, paging.getTotalCount());
        verify(commentDAO).findByMerchant(5L);
        // 空结果不触发回复批量查询
        verify(commentReplyDAO, never()).findByCommentIds(anyList());
        assertNull(paging.getData() == null || paging.getData().isEmpty() ? null : "unexpected");
    }

    @Test
    @DisplayName("listByMerchant 商家缺失拒绝")
    void listByMerchant_nullMerchant_throws() {
        assertThrows(RuntimeException.class, () -> commentService.listByMerchant(null, 1, 10));
    }

    @Test
    @DisplayName("applyReplies 全分支：命中填充 / 未命中跳过 / 空入参早退")
    void applyReplies_allBranches() {
        Comment c1 = comment("C1");
        Comment c2 = comment("C2");
        Comment c3 = comment("C3");

        CommentReplyDO r1 = reply("C1", "回复一");
        CommentReplyDO r2 = reply("C2", "回复二");
        CommentReplyDO r3 = reply("C9", "悬挂回复（对应评价不在列表）");

        List<Comment> comments = List.of(c1, c2, c3);

        // 命中 r1/r2，C3 未命中，r3 悬挂跳过
        CommentServiceImpl.applyReplies(comments, List.of(r1, r2, r3));
        assertEquals("回复一", c1.getReplyContent());
        assertEquals("回复二", c2.getReplyContent());
        assertNull(c3.getReplyContent());

        // 空回复列表早退
        Comment c4 = comment("C4");
        CommentServiceImpl.applyReplies(List.of(c4), List.of());
        assertNull(c4.getReplyContent());

        // 空评价列表早退
        CommentServiceImpl.applyReplies(List.of(), List.of(r1));
    }

    private Comment comment(String id) {
        Comment c = new Comment();
        c.setId(id);
        c.setRating(5);
        c.setContent("内容");
        return c;
    }

    private CommentReplyDO reply(String commentId, String content) {
        CommentReplyDO r = new CommentReplyDO();
        r.setId("R-" + commentId);
        r.setCommentId(commentId);
        r.setMerchantId(5L);
        r.setContent(content);
        r.setGmtCreated(new Date());
        return r;
    }
}
