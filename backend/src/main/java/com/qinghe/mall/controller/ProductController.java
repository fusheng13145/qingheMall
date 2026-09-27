package com.qinghe.mall.controller;

import com.qinghe.mall.model.Paging;
import com.qinghe.mall.model.Product;
import com.qinghe.mall.model.ProductDetail;
import com.qinghe.mall.model.Result;
import com.qinghe.mall.service.ProductDetailService;
import com.qinghe.mall.service.ProductService;
import com.qinghe.mall.util.PageParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductDetailService productDetailService;

    /**
     * 推荐位（D2，v1.7；personal 场景 v1.10）：公开接口，取前 limit 条。
     * scene=hot 热销（销量降序）/ new 新品（上架时间降序）/ personal 个性化
     * （登录用户按历史购买品牌偏好重排；未登录或无历史由服务端降级热销）。
     */
    @GetMapping("/product/recommend")
    public Result<List<Product>> recommend(@RequestParam(value = "scene", defaultValue = "hot") String scene,
                                           @RequestParam(value = "limit", defaultValue = "8") Integer limit,
                                           jakarta.servlet.http.HttpServletRequest request) {
        int size = limit == null ? 8 : Math.min(Math.max(limit, 1), 20);
        if ("personal".equals(scene)) {
            Long userId = (Long) request.getSession().getAttribute("userId");
            return Result.success(productService.recommendForUser(userId, size));
        }
        if (!"hot".equals(scene) && !"new".equals(scene)) {
            throw new com.qinghe.mall.exception.BusinessException("不支持的推荐场景（hot/new/personal）");
        }
        // hot 走销量排序（ES/MySQL 双路同语义）；new 复用默认上架时间降序
        String sort = "hot".equals(scene) ? "sales_desc" : null;
        Paging<Product> page = productService.queryOnSalePage(1, size, null, null, sort);
        return Result.success(page.getData() == null ? List.of() : page.getData());
    }

    /** 商品分页（P2-12：页码统一 pageNum，旧参数 pagination 仍兼容） */
    @GetMapping("/product/page")
    public Result<Paging<Product>> pageQuery(@RequestParam(value = "pageNum", required = false) Integer pageNum,
                                             @RequestParam(value = "pagination", required = false) Integer legacyPagination,
                                             @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
                                             @RequestParam(value = "keyword", required = false) String keyword,
                                             @RequestParam(value = "brand", required = false) String brand,
                                             @RequestParam(value = "sort", required = false) String sort) {
        int page = PageParams.resolve(pageNum, legacyPagination);
        if (pageSize < 1 || pageSize > 50) {
            pageSize = 10;
        }
        // 顾客端仅展示在售商品（status=ON，M6 上下架）
        Paging<Product> paging = productService.queryOnSalePage(page, pageSize, keyword, brand, sort);
        return Result.success(paging);
    }

    /** 品牌列表（筛选下拉用） */
    @GetMapping("/product/brands")
    public Result<List<String>> listBrands() {
        return Result.success(productService.listBrands());
    }

    @GetMapping("/product/get")
    public Result<Product> get(@RequestParam("productId") String productId) {
        Product product = productService.findById(productId);
        if (product == null) {
            return Result.fail("商品不存在");
        }
        return Result.success(product);
    }

    @GetMapping("/productdetail/productId")
    public Result<List<ProductDetail>> getProductDetails(@RequestParam("productId") String productId) {
        List<ProductDetail> details = productDetailService.findByProductId(productId);
        return Result.success(details);
    }
}
