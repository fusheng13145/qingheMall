package com.qinghe.mall.service;

import java.util.List;
import java.util.Set;

/**
 * 用户偏好画像（v1.13，复盘 §11.7-P2 结构演进）：
 * 把个性化推荐所需的跨域订单读（品牌偏好 / 已购集合）从商品服务收敛到独立协作服务，
 * ProductServiceImpl 不再直接依赖 OrderDAO。
 */
public interface UserAffinityService {

    /** 用户品牌偏好（按历史购买次数降序，前 limit；未登录或无历史返回空列表） */
    List<String> preferredBrands(Long userId, int limit);

    /** 用户已购商品 ID 集合（全状态订单去重；未登录返回空集合） */
    Set<String> purchasedProductIds(Long userId);
}
