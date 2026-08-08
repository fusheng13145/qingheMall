package com.qinghe.mall.dao;

import com.qinghe.mall.dataobject.ProductDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProductDAO {

    /**
     * 商品分页查询（支持搜索/品牌筛选/排序）。
     *
     * @param keyword 名称关键词（LIKE，可空）
     * @param brand   品牌精确匹配（可空）
     * @param sort    排序：sales_desc / price_asc / price_desc / 空（默认上新）
     * @param status  上架状态过滤（ON/OFF，空为全部；顾客端传 ON，后台/商家传空）
     */
    List<ProductDO> queryAll(@Param("keyword") String keyword,
                             @Param("brand") String brand,
                             @Param("sort") String sort,
                             @Param("status") String status);

    /** 商家店铺商品分页（M6：按 merchant_id 过滤，可叠加关键词与状态） */
    List<ProductDO> queryByMerchantId(@Param("merchantId") Long merchantId,
                                      @Param("keyword") String keyword,
                                      @Param("status") String status);

    List<String> listBrands();

    ProductDO findById(@Param("id") String id);

    /** 批量查询商品（订单列表消除 N+1） */
    List<ProductDO> findByIds(@Param("ids") List<String> ids);

    int insert(ProductDO productDO);

    int update(ProductDO productDO);

    int deleteById(@Param("id") String id);

    /**
     * 累加销量（支付成功时调用）。
     */
    int increasePurchaseNum(@Param("id") String id, @Param("num") Integer num);
}
