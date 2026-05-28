package com.qinghe.mall.goods.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.qinghe.mall.goods.dto.GoodsDTO;
import com.qinghe.mall.goods.entity.Category;
import com.qinghe.mall.goods.entity.Goods;
import com.qinghe.mall.goods.mapper.CategoryMapper;
import com.qinghe.mall.goods.mapper.GoodsMapper;
import com.qinghe.mall.goods.service.GoodsService;
import com.qinghe.mall.goods.vo.GoodsDetailVO;
import com.qinghe.mall.goods.vo.PageResult;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 商品服务实现类
 */
@Service
public class GoodsServiceImpl implements GoodsService {

    private final GoodsMapper goodsMapper;
    private final CategoryMapper categoryMapper;

    public GoodsServiceImpl(GoodsMapper goodsMapper, CategoryMapper categoryMapper) {
        this.goodsMapper = goodsMapper;
        this.categoryMapper = categoryMapper;
    }

    @Override
    public PageResult<GoodsDTO> getGoodsPage(Integer pageNum, Integer pageSize, Long categoryId, String keyword) {
        // 计算偏移量
        int offset = (pageNum - 1) * pageSize;
        
        // 查询数据列表
        List<GoodsDTO> list = goodsMapper.selectGoodsPage(offset, pageSize, categoryId, keyword);
        
        // 统计总数
        Long total = goodsMapper.countGoods(categoryId, keyword);
        
        // 计算总页数
        int pages = (int) Math.ceil((double) total / pageSize);
        
        return new PageResult<>(total, pageNum, pageSize, pages, list);
    }

    @Override
    public GoodsDetailVO getGoodsDetail(Long id) {
        Goods goods = goodsMapper.selectById(id);
        if (goods == null) {
            return null;
        }
        
        GoodsDetailVO vo = new GoodsDetailVO();
        BeanUtils.copyProperties(goods, vo);
        
        // 解析轮播图JSON
        if (StringUtils.hasText(goods.getImages())) {
            JSONArray imagesArray = JSON.parseArray(goods.getImages());
            vo.setImages(imagesArray.toList(String.class));
        }
        
        // 查询分类名称
        Category category = categoryMapper.selectById(goods.getCategoryId());
        if (category != null) {
            vo.setCategoryName(category.getName());
        }
        
        return vo;
    }

    @Override
    public boolean updateStock(Long id, Integer stock) {
        int rows = goodsMapper.updateStock(id, stock);
        return rows > 0;
    }
}
