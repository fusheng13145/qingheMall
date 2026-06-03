package com.qinghe.mall.dao;

import com.qinghe.mall.dataobject.ProductDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProductDAO {

    List<ProductDO> queryAll();

    ProductDO findById(@Param("id") String id);

    int insert(ProductDO productDO);

    int update(ProductDO productDO);

    int deleteById(@Param("id") String id);
}
