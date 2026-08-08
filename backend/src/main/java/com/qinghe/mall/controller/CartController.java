package com.qinghe.mall.controller;

import com.qinghe.mall.model.Cart;
import com.qinghe.mall.model.Result;
import com.qinghe.mall.service.CartService;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    private Long requireUserId(HttpServletRequest request) {
        Object userIdObj = request.getSession().getAttribute("userId");
        if (userIdObj == null) {
            return null;
        }
        return (Long) userIdObj;
    }

    /** 加入购物车 */
    @PostMapping("/add")
    public Result<Cart> add(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = requireUserId(request);
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        // 业务异常由 GlobalExceptionHandler 统一转为 Result.fail
        Cart cart = cartService.add(userId, (String) body.get("productDetailId"),
                body.get("quantity") == null ? 1 : Integer.parseInt(String.valueOf(body.get("quantity"))));
        return Result.success(cart);
    }

    /** 修改数量 */
    @PostMapping("/updateQuantity")
    public Result<Void> updateQuantity(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = requireUserId(request);
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        Long id = Long.parseLong(String.valueOf(body.get("id")));
        int quantity = body.get("quantity") == null ? 1 : Integer.parseInt(String.valueOf(body.get("quantity")));
        cartService.updateQuantity(userId, id, quantity);
        return Result.success();
    }

    /** 修改勾选状态 */
    @PostMapping("/updateSelected")
    public Result<Void> updateSelected(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = requireUserId(request);
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        Long id = Long.parseLong(String.valueOf(body.get("id")));
        boolean selected = body.get("selected") != null && Boolean.parseBoolean(String.valueOf(body.get("selected")));
        cartService.updateSelected(userId, id, selected);
        return Result.success();
    }

    /** 删除条目 */
    @PostMapping("/remove")
    public Result<Void> remove(@RequestParam("id") Long id, HttpServletRequest request) {
        Long userId = requireUserId(request);
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        cartService.remove(userId, id);
        return Result.success();
    }

    /** 购物车列表 */
    @GetMapping("/list")
    public Result<List<Cart>> list(HttpServletRequest request) {
        Long userId = requireUserId(request);
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        return Result.success(cartService.list(userId));
    }

    /** 购物车条目数（顶部角标） */
    @GetMapping("/count")
    public Result<Integer> count(HttpServletRequest request) {
        Long userId = requireUserId(request);
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        return Result.success(cartService.count(userId));
    }

    /** 清空已勾选条目（结算完成后由前端调用） */
    @PostMapping("/clearSelected")
    public Result<Void> clearSelected(HttpServletRequest request) {
        Long userId = requireUserId(request);
        if (userId == null) {
            return Result.fail(401, "未登录");
        }
        cartService.clearSelected(userId);
        return Result.success();
    }
}
