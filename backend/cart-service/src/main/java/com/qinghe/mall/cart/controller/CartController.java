package com.qinghe.mall.cart.controller;

import com.qinghe.mall.cart.dto.CartAddDTO;
import com.qinghe.mall.cart.dto.CartItemVO;
import com.qinghe.mall.cart.dto.CartUpdateDTO;
import com.qinghe.mall.cart.service.CartService;
import com.qinghe.mall.common.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 购物车控制器
 */
@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    /**
     * 添加购物车
     */
    @PostMapping("/add")
    public R<Void> addCart(@RequestBody CartAddDTO cartAddDTO, HttpServletRequest request) {
        Long userId = getUserId(request);
        cartService.addCart(userId, cartAddDTO);
        return R.success();
    }

    /**
     * 获取购物车列表
     */
    @GetMapping("/list")
    public R<List<CartItemVO>> getCartList(HttpServletRequest request) {
        Long userId = getUserId(request);
        List<CartItemVO> list = cartService.getCartList(userId);
        return R.success(list);
    }

    /**
     * 更新购物车数量
     */
    @PutMapping("/update")
    public R<Void> updateQuantity(@RequestBody CartUpdateDTO cartUpdateDTO, HttpServletRequest request) {
        Long userId = getUserId(request);
        cartService.updateQuantity(userId, cartUpdateDTO);
        return R.success();
    }

    /**
     * 删除购物车项
     */
    @DeleteMapping("/{id}")
    public R<Void> deleteCart(@PathVariable("id") Long id, HttpServletRequest request) {
        Long userId = getUserId(request);
        cartService.deleteCart(userId, id);
        return R.success();
    }

    /**
     * 切换选中状态
     */
    @PutMapping("/select/{id}")
    public R<Void> toggleChecked(@PathVariable("id") Long id, HttpServletRequest request) {
        Long userId = getUserId(request);
        cartService.toggleChecked(userId, id);
        return R.success();
    }

    /**
     * 全选/取消全选
     */
    @PutMapping("/selectAll")
    public R<Void> selectAll(@RequestParam("checked") Boolean checked, HttpServletRequest request) {
        Long userId = getUserId(request);
        cartService.selectAll(userId, checked);
        return R.success();
    }

    /**
     * 从请求中获取用户ID
     * 简化处理，实际应从Token或Session中获取
     */
    private Long getUserId(HttpServletRequest request) {
        String userIdStr = request.getHeader("X-User-Id");
        if (userIdStr == null || userIdStr.isEmpty()) {
            // 默认测试用户ID，实际应从登录状态获取
            return 1L;
        }
        try {
            return Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            return 1L;
        }
    }
}
