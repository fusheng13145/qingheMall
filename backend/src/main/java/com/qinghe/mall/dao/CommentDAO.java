package com.qinghe.mall.dao;

import com.qinghe.mall.dataobject.CommentDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CommentDAO {

    int insert(CommentDO commentDO);

    /** 按主键查（A4 商家回复归属校验用） */
    CommentDO findById(@Param("commentId") String commentId);

    List<CommentDO> findByProductId(@Param("productId") String productId);

    /** 商品评价总数 */
    long countByProductId(@Param("productId") String productId);

    /** 商品评分均值（无评价返回 null） */
    Double avgRatingByProductId(@Param("productId") String productId);

    /** 某订单是否已评价（>0 表示已评价） */
    int countByOrderNumber(@Param("orderNumber") String orderNumber);

    /** 批量查询已评价的订单号集合（订单列表填充 commented 标记，一次 IN 查询消除 N+1） */
    List<String> findCommentedOrderNumbers(@Param("orderNumbers") List<String> orderNumbers);

    /** 按商品删除（仅测试数据清理使用） */
    int deleteByProductIdForTest(@Param("productId") String productId);

    /** 本店商品的评价（A4 商家端评价管理，JOIN product 带商品名；分页由 PageHelper 包装） */
    List<CommentDO> findByMerchant(@Param("merchantId") Long merchantId);

    /** 店铺主页：本店商品评分均值（A3，无评价返回 null） */
    Double avgRatingByMerchant(@Param("merchantId") Long merchantId);
}
