package com.qinghe.mall.goods.mapper;

import com.qinghe.mall.goods.dto.GoodsDTO;
import com.qinghe.mall.goods.entity.Goods;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 商品Mapper
 */
@Mapper
public interface GoodsMapper {

    /**
     * 分页查询商品列表
     */
    List<GoodsDTO> selectGoodsPage(@Param("pageNum") Integer pageNum,
                                   @Param("pageSize") Integer pageSize,
                                   @Param("categoryId") Long categoryId,
                                   @Param("keyword") String keyword);

    /**
     * 统计商品总数
     */
    Long countGoods(@Param("categoryId") Long categoryId,
                    @Param("keyword") String keyword);

    /**
     * 根据ID查询商品
     */
    Goods selectById(@Param("id") Long id);

    /**
     * 更新库存
     */
    int updateStock(@Param("id") Long id, @Param("stock") Integer stock);

    /**
     * 根据分类ID查询商品数量
     */
    Long countByCategoryId(@Param("categoryId") Long categoryId);
}
