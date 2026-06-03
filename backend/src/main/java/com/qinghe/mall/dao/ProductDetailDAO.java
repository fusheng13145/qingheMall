package com.qinghe.mall.dao;

import com.qinghe.mall.dataobject.ProductDetailDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProductDetailDAO {

    List<ProductDetailDO> findByProductId(@Param("productId") String productId);

    ProductDetailDO findById(@Param("id") String id);

    int updateStock(@Param("id") String id, @Param("stock") Integer stock);
}
