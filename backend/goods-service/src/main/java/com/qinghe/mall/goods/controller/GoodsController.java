package com.qinghe.mall.goods.controller;

import com.qinghe.mall.common.R;
import com.qinghe.mall.goods.dto.GoodsDTO;
import com.qinghe.mall.goods.service.GoodsService;
import com.qinghe.mall.goods.vo.GoodsDetailVO;
import com.qinghe.mall.goods.vo.PageResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品控制器
 */
@RestController
@RequestMapping("/api/goods")
public class GoodsController {

    private final GoodsService goodsService;

    public GoodsController(GoodsService goodsService) {
        this.goodsService = goodsService;
    }

    /**
     * 商品列表
     *
     * @param pageNum    页码
     * @param pageSize   每页大小
     * @param categoryId 分类ID
     * @param keyword    关键词
     * @return 商品分页列表
     */
    @GetMapping("/list")
    public R<PageResult<GoodsDTO>> getGoodsList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword) {
        PageResult<GoodsDTO> result = goodsService.getGoodsPage(pageNum, pageSize, categoryId, keyword);
        return R.success(result);
    }

    /**
     * 商品详情
     *
     * @param id 商品ID
     * @return 商品详情
     */
    @GetMapping("/{id}")
    public R<GoodsDetailVO> getGoodsDetail(@PathVariable Long id) {
        GoodsDetailVO detail = goodsService.getGoodsDetail(id);
        if (detail == null) {
            return R.error(2001, "商品不存在");
        }
        return R.success(detail);
    }
}
