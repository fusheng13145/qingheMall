package com.qinghe.mall.controller;

import com.qinghe.mall.config.RateLimit;
import com.qinghe.mall.model.Comment;
import com.qinghe.mall.model.Order;
import com.qinghe.mall.model.Paging;
import com.qinghe.mall.model.Result;
import com.qinghe.mall.service.CommentService;
import com.qinghe.mall.service.OrderService;
import java.util.HashMap;
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
@RequestMapping("/api/comment")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @Autowired
    private OrderService orderService;

    /**
     * 提交评价（仅已支付订单可评，每订单一次）。
     * POST /api/comment/add  body: { productId, orderNumber, rating, content }
     */
    @RateLimit(rate = 20, message = "评价过于频繁，请稍后再试")
    @PostMapping("/add")
    public Result<Comment> add(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Object userIdObj = request.getSession().getAttribute("userId");
        if (userIdObj == null) {
            return Result.fail(401, "未登录");
        }
        Long userId = (Long) userIdObj;
        Comment comment = commentService.addComment(
                userId,
                (String) body.get("productId"),
                (String) body.get("orderNumber"),
                body.get("rating") == null ? null : Integer.parseInt(String.valueOf(body.get("rating"))),
                (String) body.get("content"));
        return Result.success(comment);
    }

    /**
     * 商品评价列表（分页）：GET /api/comment/product?productId=&pagination=&pageSize=
     */
    @GetMapping("/product")
    public Result<Paging<Comment>> listByProduct(@RequestParam("productId") String productId,
                                                 @RequestParam(value = "pagination", defaultValue = "1") Integer pagination,
                                                 @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        return Result.success(commentService.listByProduct(productId, pagination, pageSize));
    }

    /**
     * 商品评分汇总：GET /api/comment/summary?productId=  → { avgRating, ratingCount }
     */
    @GetMapping("/summary")
    public Result<Map<String, Object>> summary(@RequestParam("productId") String productId) {
        return Result.success(commentService.summary(productId));
    }

    /**
     * 查询某订单是否已评价（订单中心评价按钮状态）：GET /api/comment/orderStatus?orderNumber=
     * 返回 { commented: true/false }
     */
    @GetMapping("/orderStatus")
    public Result<Map<String, Object>> orderStatus(@RequestParam("orderNumber") String orderNumber,
                                                   HttpServletRequest request) {
        Object userIdObj = request.getSession().getAttribute("userId");
        if (userIdObj == null) {
            return Result.fail(401, "未登录");
        }
        // 归属校验（P3-3）：仅订单本人可查其评价状态，防泄露他人订单信息
        Order order = orderService.findByOrderNumber(orderNumber);
        if (order == null) {
            return Result.fail("订单不存在");
        }
        if (!order.getUserId().equals((Long) userIdObj)) {
            return Result.fail(403, "无权查看该订单");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("commented", commentService.hasCommented(orderNumber));
        return Result.success(result);
    }
}
