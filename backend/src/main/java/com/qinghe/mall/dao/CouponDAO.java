package com.qinghe.mall.dao;

import com.qinghe.mall.dataobject.CouponDO;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface CouponDAO {

    int insert(CouponDO coupon);

    CouponDO findById(@Param("id") String id);

    List<CouponDO> findAll();

    /** 上架中的券列表（领券中心用），按创建时间倒序 */
    List<CouponDO> findActive();

    /** 分页列表（管理端），按创建时间倒序 */
    List<CouponDO> findPage();

    /** 原子自增已领取数，仅当 issued < total 时生效；返回影响行数 */
    int incrementIssued(@Param("id") String id);

    int update(CouponDO coupon);

    int updateStatus(@Param("id") String id, @Param("status") String status);

    /** 商家券列表（按 merchant_id 过滤，供 PageHelper 分页） */
    List<CouponDO> findByMerchant(@Param("merchantId") Long merchantId);

    /** 商家上架且在有效期内的券（店铺页展示/顾客端按店过滤） */
    List<CouponDO> findActiveByMerchant(@Param("merchantId") Long merchantId);
}
