package com.qinghe.mall.order.service;

import com.github.pagehelper.PageInfo;
import com.qinghe.mall.order.dto.OrderCreateDTO;
import com.qinghe.mall.order.vo.OrderVO;

/**
 * 订单服务接口
 */
public interface OrderService {

    /**
     * 创建订单
     * @param userId 用户ID
     * @param dto 创建订单参数
     * @return 订单ID
     */
    Long createOrder(Long userId, OrderCreateDTO dto);

    /**
     * 查询订单列表
     * @param userId 用户ID
     * @param status 订单状态
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 分页订单列表
     */
    PageInfo<OrderVO> getOrderList(Long userId, Integer status, int pageNum, int pageSize);

    /**
     * 查询订单详情
     * @param orderId 订单ID
     * @return 订单详情
     */
    OrderVO getOrderDetail(Long orderId);

    /**
     * 取消订单
     * @param orderId 订单ID
     */
    void cancelOrder(Long orderId);

    /**
     * 确认收货
     * @param orderId 订单ID
     */
    void confirmReceive(Long orderId);

    /**
     * 支付订单（修改状态为已付款）
     * @param orderId 订单ID
     */
    void payOrder(Long orderId);

    /**
     * 发货
     * @param orderId 订单ID
     */
    void deliverOrder(Long orderId);

    /**
     * 超时取消订单
     * @param orderId 订单ID
     */
    void timeoutCancelOrder(Long orderId);
}
