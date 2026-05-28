package com.qinghe.mall.order.dto;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 创建订单参数
 */
@Data
public class OrderCreateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 地址ID
     */
    private Long addressId;

    /**
     * 购物车ID列表
     */
    private List<Long> cartIds;

    /**
     * 订单备注
     */
    private String remark;
}
