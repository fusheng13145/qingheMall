package com.qinghe.mall.dao;

import com.qinghe.mall.dataobject.MerchantDO;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 入驻商家 DAO（M6 平台化）。
 */
public interface MerchantDAO {

    int insert(MerchantDO merchant);

    MerchantDO findById(@Param("id") Long id);

    MerchantDO findByUserId(@Param("userId") Long userId);

    /** status 为空查全部；LEFT JOIN user 填充店主用户名/昵称 */
    List<MerchantDO> query(@Param("status") String status);

    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("rejectReason") String rejectReason);

    int deleteByIdForTest(@Param("id") Long id);
}
