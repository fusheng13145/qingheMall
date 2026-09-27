package com.qinghe.mall.dao;

import com.qinghe.mall.dataobject.BannerDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BannerDAO {

    int insert(BannerDO banner);

    int update(BannerDO banner);

    int deleteById(@Param("id") String id);

    int updateStatus(@Param("id") String id, @Param("status") String status);

    BannerDO findById(@Param("id") String id);

    /** 管理端全量列表（分页由 PageHelper 包装） */
    List<BannerDO> findAll();

    /** 顾客端仅上架运营位：sort_order 升序、新置顶 */
    List<BannerDO> findActive();
}
