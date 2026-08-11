package com.qinghe.mall.service;

/**
 * 库存流水服务：下单扣减/取消回滚/超时回滚留痕，用于对账与追溯。
 */
public interface StockLogService {

    /** 变动类型常量 */
    String TYPE_ORDER_DEDUCT = "ORDER_DEDUCT";
    String TYPE_ORDER_RESTORE = "ORDER_RESTORE";
    String TYPE_EXPIRE_RESTORE = "EXPIRE_RESTORE";
    String TYPE_STOCK_SET = "STOCK_SET";
    /** P2-18：退货退款审核通过后的库存回补 */
    String TYPE_REFUND_RESTORE = "REFUND_RESTORE";

    /**
     * 记录一条库存变动流水。
     *
     * @param productDetailId 商品规格ID
     * @param productId       商品ID（可空）
     * @param orderNumber     关联订单号（可空）
     * @param changeType      变动类型（TYPE_* 常量）
     * @param changeQuantity  变动数量（扣减为负数，回滚为正数）
     * @param beforeStock     变动前库存
     * @param afterStock      变动后库存
     */
    void record(String productDetailId, String productId, String orderNumber,
                String changeType, int changeQuantity, int beforeStock, int afterStock);
}
