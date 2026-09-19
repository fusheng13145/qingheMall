package com.qinghe.mall.dao;

import com.qinghe.mall.dataobject.CommentReplyDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CommentReplyDAO {

    int insert(CommentReplyDO reply);

    CommentReplyDO findByCommentId(@Param("commentId") String commentId);

    /** 批量按评价 ID 查回复（商品详情评价列表填充用） */
    List<CommentReplyDO> findByCommentIds(@Param("commentIds") List<String> commentIds);

    /** 商家的全部回复（分页由 PageHelper 包装） */
    List<CommentReplyDO> findByMerchant(@Param("merchantId") Long merchantId);
}
