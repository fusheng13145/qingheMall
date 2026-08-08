package com.qinghe.mall.dao;

import com.qinghe.mall.dataobject.StockLogDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StockLogDAO {

    int insert(StockLogDO stockLogDO);

    /** 按订单号查询流水（对账/详情用） */
    List<StockLogDO> findByOrderNumber(@Param("orderNumber") String orderNumber);

    /** 按商品规格查询流水（最近 limit 条） */
    List<StockLogDO> findByProductDetailId(@Param("productDetailId") String productDetailId,
                                           @Param("limit") int limit);

    /** 按商品删除（仅测试数据清理使用） */
    int deleteByProductDetailIdForTest(@Param("productDetailId") String productDetailId);
}
