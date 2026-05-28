package com.qinghe.mall.goods.mapper;

import com.qinghe.mall.goods.entity.Category;
import com.qinghe.mall.goods.vo.CategoryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 分类Mapper
 */
@Mapper
public interface CategoryMapper {

    /**
     * 查询所有一级分类
     */
    List<CategoryVO> selectRootCategories();

    /**
     * 根据父ID查询子分类
     */
    List<CategoryVO> selectByParentId(@Param("parentId") Long parentId);

    /**
     * 根据ID查询分类
     */
    Category selectById(@Param("id") Long id);

    /**
     * 查询所有分类
     */
    List<Category> selectAll();
}
