package com.qinghe.mall.dao;

import com.qinghe.mall.dataobject.ProductDetailDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProductDetailDAO {

    List<ProductDetailDO> findByProductId(@Param("productId") String productId);

    ProductDetailDO findById(@Param("id") String id);

    /** 批量查询规格（订单列表消除 N+1） */
    List<ProductDetailDO> findByIds(@Param("ids") List<String> ids);

    int updateStock(@Param("id") String id, @Param("stock") Integer stock);

    /** 新增规格（商家商品保存用，M6） */
    int insert(ProductDetailDO detailDO);

    /** 删除商品的全部规格（商家 SKU 整体替换用，M6） */
    int deleteByProductId(@Param("productId") String productId);

    /**
     * 原子扣减库存：仅当库存充足（stock >= quantity）时扣减，返回受影响行数。
     * 避免「查-改-写」竞态，即使分布式锁失效也不会超卖。
     */
    int decreaseStock(@Param("id") String id, @Param("quantity") Integer quantity);

    /**
     * 回滚库存：取消订单/超时关单时把已扣减的库存加回，返回受影响行数。
     */
    int increaseStock(@Param("id") String id, @Param("quantity") Integer quantity);
}
