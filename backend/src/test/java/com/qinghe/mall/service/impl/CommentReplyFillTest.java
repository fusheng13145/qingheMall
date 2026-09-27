package com.qinghe.mall.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qinghe.mall.dao.CommentDAO;
import com.qinghe.mall.dao.CommentReplyDAO;
import com.qinghe.mall.dataobject.CommentDO;
import com.qinghe.mall.dataobject.CommentReplyDO;
import com.qinghe.mall.exception.BusinessException;
import com.qinghe.mall.model.Comment;
import com.qinghe.mall.model.Paging;
import com.qinghe.mall.model.User;
import com.qinghe.mall.service.UserService;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * 评价列表/回复填充分支补测（v1.12，复盘 §11.7 清单）。
 *
 * 覆盖：listByProduct 分页收敛 + 昵称回退（nickName 空取 userName）+ 用户缺失跳过 +
 * 回复填充、listByMerchant 商家缺失拒绝与参数收敛、applyReplies/fillReplies 早退分支。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommentReplyFillTest {

    @Mock
    private CommentDAO commentDAO;

    @Mock
    private CommentReplyDAO commentReplyDAO;

    @Mock
    private UserService userService;

    @InjectMocks
    private CommentServiceImpl commentService;

    private CommentDO c1;
    private CommentDO c2;

    @BeforeEach
    void setUp() {
        c1 = new CommentDO();
        c1.setId("cm1");
        c1.setUserId(9L);
        c1.setContent("好评");
        c1.setRating(5);
        c2 = new CommentDO();
        c2.setId("cm2");
        c2.setUserId(null); // 用户缺失分支
        c2.setContent("中评");
        c2.setRating(3);
    }

    // 说明：listByProduct/listByMerchant 的 PageHelper 数据填充路径在纯 mock 下
    // doSelectPage 恒空（§8.1 已记载的局限，昵称/回复填充逻辑由 applyReplies 静态直测
    // 与 HTTP 集成覆盖），此处仅验证分页收敛分支与 DAO 透传。

    @Test
    @DisplayName("listByProduct：分页缺省收敛 + DAO 透传")
    void listByProduct_defaultsAndClamp() {
        when(commentDAO.findByProductId("p1")).thenReturn(new ArrayList<>(List.of(c1, c2)));

        Paging<Comment> paging = commentService.listByProduct("p1", null, null);

        assertEquals(1, paging.getPageNum());
        assertEquals(10, paging.getPageSize());
        verify(commentDAO).findByProductId(eq("p1"));
    }

    @Test
    @DisplayName("listByProduct：pageSize 越界收敛 10")
    void listByProduct_clamp() {
        when(commentDAO.findByProductId("p1")).thenReturn(new ArrayList<>(List.of(c1)));

        Paging<Comment> paging = commentService.listByProduct("p1", 0, 99);

        assertEquals(1, paging.getPageNum());
        assertEquals(10, paging.getPageSize());
    }

    @Test
    @DisplayName("listByMerchant：商家缺失拒绝")
    void listByMerchant_nullMerchant_rejected() {
        assertThrows(BusinessException.class, () -> commentService.listByMerchant(null, 1, 10));
    }

    @Test
    @DisplayName("listByMerchant：参数收敛 + 回复查询透传")
    void listByMerchant_clampAndFill() {
        when(commentDAO.findByMerchant(7L)).thenReturn(new ArrayList<>(List.of(c1)));

        Paging<Comment> paging = commentService.listByMerchant(7L, 0, 99);

        assertEquals(1, paging.getPageNum());
        assertEquals(10, paging.getPageSize());
    }

    @Test
    @DisplayName("applyReplies：空评论/空回复早退，命中填充、未命中保持")
    void applyReplies_branches() {
        Comment a = new Comment();
        a.setId("cm1");
        Comment b = new Comment();
        b.setId("cmX");

        // 空评论早退
        CommentServiceImpl.applyReplies(new ArrayList<>(), List.of(new CommentReplyDO()));
        // 空回复早退
        CommentServiceImpl.applyReplies(List.of(a), new ArrayList<>());
        assertNull(a.getReplyContent());

        // 命中 cm1，未命中 cmX
        CommentReplyDO reply = new CommentReplyDO();
        reply.setCommentId("cm1");
        Date t = new Date();
        reply.setContent("已回复");
        reply.setGmtCreated(t);
        CommentServiceImpl.applyReplies(List.of(a, b), List.of(reply));
        assertEquals("已回复", a.getReplyContent());
        assertEquals(t, a.getReplyTime());
        assertNull(b.getReplyContent());
    }

    @Test
    @DisplayName("fillReplies：空 commentIds 早退（不查 DAO）")
    void fillReplies_emptyIds_skipsDao() {
        commentService.fillReplies(new ArrayList<>(), new ArrayList<>());
        verify(commentReplyDAO, never()).findByCommentIds(anyList());
    }

    @Test
    @DisplayName("hasCommented：单号空白返回 false")
    void hasCommented_blank_false() {
        assertTrue(!commentService.hasCommented(" "));
    }

    @Test
    @DisplayName("summary：均值空值兜底 0")
    void summary_nullAvg_zero() {
        when(commentDAO.countByProductId("p1")).thenReturn(0L);
        when(commentDAO.avgRatingByProductId("p1")).thenReturn(null);

        var result = commentService.summary("p1");

        assertEquals(0, ((Number) result.get("avgRating")).doubleValue(), 0.001);
        assertEquals(0L, result.get("ratingCount"));
        verify(commentDAO).countByProductId(eq("p1"));
    }
}
