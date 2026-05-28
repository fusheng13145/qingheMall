package com.qinghe.mall.goods.service;

import com.qinghe.mall.goods.dto.GoodsDTO;
import com.qinghe.mall.goods.vo.GoodsDetailVO;
import com.qinghe.mall.goods.vo.PageResult;

/**
 * 商品服务接口
 */
public interface GoodsService {

    /**
     * 分页查询商品列表
     *
     * @param pageNum   页码
     * @param pageSize  每页大小
     * @param categoryId 分类ID
     * @param keyword   关键词
     * @return 分页结果
     */
    PageResult<GoodsDTO> getGoodsPage(Integer pageNum, Integer pageSize, Long categoryId, String keyword);

    /**
     * 获取商品详情
     *
     * @param id 商品ID
     * @return 商品详情
     */
    GoodsDetailVO getGoodsDetail(Long id);

    /**
     * 更新商品库存
     *
     * @param id    商品ID
     * @param stock 库存数量
     * @return 是否成功
     */
    boolean updateStock(Long id, Integer stock);
}
