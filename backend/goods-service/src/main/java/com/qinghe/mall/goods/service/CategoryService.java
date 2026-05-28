package com.qinghe.mall.goods.service;

import com.qinghe.mall.goods.vo.CategoryVO;

import java.util.List;

/**
 * 分类服务接口
 */
public interface CategoryService {

    /**
     * 获取分类树
     *
     * @return 分类树
     */
    List<CategoryVO> getCategoryTree();

    /**
     * 获取分类信息
     *
     * @param id 分类ID
     * @return 分类信息
     */
    CategoryVO getCategoryById(Long id);
}
