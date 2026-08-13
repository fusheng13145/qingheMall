package com.qinghe.mall.dao;

import com.qinghe.mall.dataobject.SeckillActivityDO;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 秒杀活动 DAO（M5-A3）。
 */
public interface SeckillActivityDAO {

    int insert(SeckillActivityDO activity);

    SeckillActivityDO findById(@Param("id") String id);

    /** 用户端进行中活动：status=ONGOING 且当前时间在 [start_time, end_time] 内 */
    List<SeckillActivityDO> findActive();

    /** 管理端列表：status 为空时查全部，否则按状态过滤 */
    List<SeckillActivityDO> query(@Param("status") String status);

    int update(SeckillActivityDO activity);

    int updateStatus(@Param("id") String id, @Param("status") String status);

    /** 商家活动列表（按 merchant_id 过滤，status 为空查全部，供 PageHelper 分页） */
    List<SeckillActivityDO> findByMerchant(@Param("merchantId") Long merchantId, @Param("status") String status);

    /** 商家进行中活动（店铺页展示/顾客端按店过滤） */
    List<SeckillActivityDO> findActiveByMerchant(@Param("merchantId") Long merchantId);

    /**
     * 活动库存 CAS 预扣：仅当 remain_stock >= quantity 时扣减，返回受影响行数。
     * 返回 0 表示库存不足（售罄），由调用方据此拒绝下单。防超卖主防线。
     */
    int decreaseRemainStock(@Param("id") String id, @Param("quantity") int quantity);

    /** 活动库存回滚（超时/取消未付）：remain_stock 加回 */
    int increaseRemainStock(@Param("id") String id, @Param("quantity") int quantity);

    /** 仅测试清理使用 */
    int deleteByIdForTest(@Param("id") String id);
}
