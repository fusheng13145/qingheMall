package com.qinghe.mall.dao;

import com.qinghe.mall.dataobject.LogisticsTraceDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LogisticsTraceDAO {

    int insert(LogisticsTraceDO traceDO);

    /** 按订单号查询轨迹（时间正序，供前端时间线渲染） */
    List<LogisticsTraceDO> findByOrderNumber(@Param("orderNumber") String orderNumber);

    /** 仅测试数据清理使用 */
    int deleteByOrderNumberForTest(@Param("orderNumber") String orderNumber);
}
