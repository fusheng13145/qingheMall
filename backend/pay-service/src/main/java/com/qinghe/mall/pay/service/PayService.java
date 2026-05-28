package com.qinghe.mall.pay.service;

import com.qinghe.mall.pay.dto.PayCreateDTO;
import com.qinghe.mall.pay.dto.PayVO;

/**
 * 支付服务接口
 */
public interface PayService {

    /**
     * 创建支付订单
     * @param payCreateDTO 支付创建参数
     * @param userId 用户ID
     * @return 支付信息
     */
    PayVO createPay(PayCreateDTO payCreateDTO, Long userId);

    /**
     * 支付回调处理
     * @param payType 支付类型
     * @param params 回调参数
     * @return 回调结果
     */
    String callback(Integer payType, java.util.Map<String, String> params);

    /**
     * 查询支付状态
     * @param orderId 订单ID
     * @return 支付状态
     */
    Integer getPayStatus(Long orderId);
}