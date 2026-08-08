package com.qinghe.mall.dao;

import com.qinghe.mall.dataobject.CommentDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CommentDAO {

    int insert(CommentDO commentDO);

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
}
