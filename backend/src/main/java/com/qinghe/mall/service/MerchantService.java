package com.qinghe.mall.service;

import com.qinghe.mall.dataobject.MerchantDO;
import com.qinghe.mall.model.Paging;
import com.qinghe.mall.model.User;

/**
 * 入驻商家服务（M6 平台化）。
 */
public interface MerchantService {

    /** 商家注册：创建商家账号（role=MERCHANT）+ 入驻申请（PENDING） */
    User registerMerchant(String userName, String pwd, String shopName, String shopLogo, String shopDesc);

    /** 已有用户申请开店（幂等：已有商家记录则返回） */
    MerchantDO apply(Long userId, String shopName, String shopLogo, String shopDesc);

    /** 查询我的商家信息（无则返回 null） */
    MerchantDO getByUserId(Long userId);

    MerchantDO getById(Long id);

    /** 管理端分页列表（status 为空查全部） */
    Paging<MerchantDO> list(String status, int pageNum, int pageSize);

    /** 审核：approve=true 置 ACTIVE，否则置 REJECTED + 驳回原因 */
    MerchantDO audit(Long merchantId, boolean approve, String reason);
}
