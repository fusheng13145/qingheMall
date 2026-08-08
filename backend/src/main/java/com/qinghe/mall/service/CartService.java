package com.qinghe.mall.service;

import com.qinghe.mall.model.Cart;
import java.util.List;

public interface CartService {

    /** 加入购物车：已存在则数量叠加 */
    Cart add(Long userId, String productDetailId, Integer quantity);

    /** 修改数量 */
    boolean updateQuantity(Long userId, Long id, Integer quantity);

    /** 修改勾选状态 */
    boolean updateSelected(Long userId, Long id, boolean selected);

    /** 删除条目（校验归属） */
    boolean remove(Long userId, Long id);

    /** 删除指定商品的购物车条目（结算后调用） */
    boolean removeByDetail(Long userId, String productDetailId);

    /** 清空已勾选条目（结算完成后调用） */
    int clearSelected(Long userId);

    /** 购物车列表（含商品名/图/规格/单价/库存冗余字段） */
    List<Cart> list(Long userId);

    /** 购物车条目数 */
    int count(Long userId);
}
