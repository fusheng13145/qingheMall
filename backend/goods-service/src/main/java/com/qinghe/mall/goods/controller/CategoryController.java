package com.qinghe.mall.goods.controller;

import com.qinghe.mall.common.R;
import com.qinghe.mall.goods.service.CategoryService;
import com.qinghe.mall.goods.vo.CategoryVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 分类控制器
 */
@RestController
@RequestMapping("/api/category")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * 获取分类树
     *
     * @return 分类树
     */
    @GetMapping("/tree")
    public R<List<CategoryVO>> getCategoryTree() {
        List<CategoryVO> tree = categoryService.getCategoryTree();
        return R.success(tree);
    }

    /**
     * 获取分类信息
     *
     * @param id 分类ID
     * @return 分类信息
     */
    @GetMapping("/{id}")
    public R<CategoryVO> getCategory(@PathVariable Long id) {
        CategoryVO category = categoryService.getCategoryById(id);
        if (category == null) {
            return R.error(2001, "分类不存在");
        }
        return R.success(category);
    }
}
