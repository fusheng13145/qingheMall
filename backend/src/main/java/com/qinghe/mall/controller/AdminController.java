package com.qinghe.mall.controller;

import com.qinghe.mall.model.*;
import com.qinghe.mall.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired private ProductService productService;
    @Autowired private OrderService orderService;
    @Autowired private UserService userService;

    // 检查管理员权限的私有方法
    private boolean checkAdmin(HttpServletRequest request) {
        Object role = request.getSession().getAttribute("role");
        return "ADMIN".equals(role);
    }

    // ========== Dashboard 统计 ==========
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard(HttpServletRequest request) {
        if (!checkAdmin(request)) {
            return Result.fail(403, "无管理员权限");
        }
        Map<String, Object> stats = new HashMap<>();
        stats.put("productCount", productService.queryPage(1, 1).getTotalCount());
        stats.put("orderCount", orderService.findAll().size());
        stats.put("userCount", userService.findAll().size());
        // 计算总收入
        double totalRevenue = orderService.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.TRADE_PAID_SUCCESS)
                .mapToDouble(o -> o.getTotalPrice() != null ? o.getTotalPrice() : 0)
                .sum();
        stats.put("totalRevenue", totalRevenue);
        return Result.success(stats);
    }

    // ========== 商品管理 ==========
    @PostMapping("/product/add")
    public Result<Product> addProduct(@RequestBody Product product, HttpServletRequest request) {
        if (!checkAdmin(request)) {
            return Result.fail(403, "无管理员权限");
        }
        try {
            Product result = productService.add(product);
            return Result.success(result);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/product/update")
    public Result<Product> updateProduct(@RequestBody Product product, HttpServletRequest request) {
        if (!checkAdmin(request)) {
            return Result.fail(403, "无管理员权限");
        }
        try {
            Product result = productService.update(product);
            return Result.success(result);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/product/delete")
    public Result<Void> deleteProduct(@RequestParam("id") String id, HttpServletRequest request) {
        if (!checkAdmin(request)) {
            return Result.fail(403, "无管理员权限");
        }
        productService.delete(id);
        return Result.success();
    }

    // ========== 订单管理 ==========
    @GetMapping("/order/list")
    public Result<List<Order>> listAllOrders(HttpServletRequest request) {
        if (!checkAdmin(request)) {
            return Result.fail(403, "无管理员权限");
        }
        List<Order> orders = orderService.findAll();
        return Result.success(orders);
    }

    @PostMapping("/order/updateStatus")
    public Result<Void> updateOrderStatus(@RequestParam("orderNumber") String orderNumber,
                                          @RequestParam("status") String status,
                                          HttpServletRequest request) {
        if (!checkAdmin(request)) {
            return Result.fail(403, "无管理员权限");
        }
        orderService.updateOrderStatus(orderNumber, status);
        return Result.success();
    }

    // ========== 用户管理 ==========
    @GetMapping("/user/list")
    public Result<List<User>> listAllUsers(HttpServletRequest request) {
        if (!checkAdmin(request)) {
            return Result.fail(403, "无管理员权限");
        }
        List<User> users = userService.findAll();
        // 不返回密码
        users.forEach(u -> u.setPwd(null));
        return Result.success(users);
    }

    @PostMapping("/user/updateRole")
    public Result<Void> updateUserRole(@RequestParam("id") Long id,
                                       @RequestParam("role") String role,
                                       HttpServletRequest request) {
        if (!checkAdmin(request)) {
            return Result.fail(403, "无管理员权限");
        }
        userService.updateRole(id, role);
        return Result.success();
    }
}
