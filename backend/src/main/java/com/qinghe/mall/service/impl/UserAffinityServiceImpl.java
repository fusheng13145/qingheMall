package com.qinghe.mall.service.impl;

import com.qinghe.mall.dao.OrderDAO;
import com.qinghe.mall.service.UserAffinityService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 用户偏好画像实现（v1.13）：订单域聚合查询的薄封装。
 * 未登录（userId == null）一律返回空结果，由调用方决定降级策略。
 */
@Service
public class UserAffinityServiceImpl implements UserAffinityService {

    @Autowired
    private OrderDAO orderDAO;

    @Override
    public List<String> preferredBrands(Long userId, int limit) {
        if (userId == null) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> rows = orderDAO.brandAffinity(userId, limit);
        return rows == null ? Collections.emptyList()
                : rows.stream().map(m -> String.valueOf(m.get("brand"))).collect(Collectors.toList());
    }

    @Override
    public Set<String> purchasedProductIds(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        List<String> ids = orderDAO.findPurchasedProductIds(userId);
        return ids == null ? Collections.emptySet() : Set.copyOf(ids);
    }
}
