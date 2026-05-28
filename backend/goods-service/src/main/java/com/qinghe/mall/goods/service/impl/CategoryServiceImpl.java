package com.qinghe.mall.goods.service.impl;

import com.qinghe.mall.goods.entity.Category;
import com.qinghe.mall.goods.mapper.CategoryMapper;
import com.qinghe.mall.goods.service.CategoryService;
import com.qinghe.mall.goods.vo.CategoryVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 分类服务实现类
 */
@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    public CategoryServiceImpl(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    @Override
    public List<CategoryVO> getCategoryTree() {
        // 查询所有分类
        List<Category> allCategories = categoryMapper.selectAll();
        
        // 转换为VO
        List<CategoryVO> allCategoryVOs = allCategories.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        
        // 按层级分组
        List<CategoryVO> rootCategories = allCategoryVOs.stream()
                .filter(c -> c.getParentId() == 0 && c.getLevel() == 1)
                .collect(Collectors.toList());
        
        // 递归构建分类树
        for (CategoryVO root : rootCategories) {
            buildCategoryTree(root, allCategoryVOs);
        }
        
        return rootCategories;
    }

    @Override
    public CategoryVO getCategoryById(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            return null;
        }
        return convertToVO(category);
    }

    /**
     * 递归构建分类树
     */
    private void buildCategoryTree(CategoryVO parent, List<CategoryVO> allCategories) {
        List<CategoryVO> children = allCategories.stream()
                .filter(c -> c.getParentId().equals(parent.getId()))
                .collect(Collectors.toList());
        
        for (CategoryVO child : children) {
            buildCategoryTree(child, allCategories);
        }
        
        parent.setChildren(children);
    }

    /**
     * 转换为VO
     */
    private CategoryVO convertToVO(Category category) {
        CategoryVO vo = new CategoryVO();
        BeanUtils.copyProperties(category, vo);
        vo.setChildren(new ArrayList<>());
        return vo;
    }
}
