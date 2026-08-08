package com.qinghe.mall.controller;

import com.qinghe.mall.config.RateLimit;
import com.qinghe.mall.dataobject.SeckillActivityDO;
import com.qinghe.mall.model.Result;
import com.qinghe.mall.service.SeckillService;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 秒杀用户端接口（M5-A3）。
 * 活动列表/详情公开；下单需登录并受 {@link RateLimit} 限流保护。
 */
@RestController
@RequestMapping("/api/seckill")
public class SeckillController {

    @Autowired
    private SeckillService seckillService;

    /** 进行中活动列表（含剩余库存与起止时间，前端据此倒计时） */
    @GetMapping("/activities")
    public Result<List<SeckillActivityDO>> activities() {
        return Result.success(seckillService.listOngoing());
    }

    /** 活动详情 */
    @GetMapping("/activity/{id}")
    public Result<SeckillActivityDO> activity(@PathVariable("id") String id) {
        return Result.success(seckillService.getActivity(id));
    }

    /**
     * 秒杀下单（核心）：限流 + 登录校验。
     * 成功返回普通订单号，前端跳转 /pay?orderNumber= 完成支付。
     */
    @RateLimit(key = "seckill.createOrder", rate = 100, message = "抢购过于火爆，请稍后再试")
    @PostMapping("/{id}/createOrder")
    public Result<String> createOrder(@PathVariable("id") String id,
                                       @RequestParam(value = "quantity", defaultValue = "1") Integer quantity,
                                       @RequestParam(value = "receiverName", required = false) String receiverName,
                                       @RequestParam(value = "receiverPhone", required = false) String receiverPhone,
                                       @RequestParam(value = "receiverAddress", required = false) String receiverAddress,
                                       HttpServletRequest request) {
        Object userIdObj = request.getSession().getAttribute("userId");
        if (userIdObj == null) {
            return Result.fail(401, "未登录");
        }
        Long userId = (Long) userIdObj;
        String orderNumber = seckillService.createOrder(id, userId, quantity, receiverName, receiverPhone, receiverAddress);
        return Result.success(orderNumber);
    }
}
