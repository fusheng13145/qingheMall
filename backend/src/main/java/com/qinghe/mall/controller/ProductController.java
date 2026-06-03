package com.qinghe.mall.controller;

import com.qinghe.mall.model.Paging;
import com.qinghe.mall.model.Product;
import com.qinghe.mall.model.ProductDetail;
import com.qinghe.mall.model.Result;
import com.qinghe.mall.service.ProductDetailService;
import com.qinghe.mall.service.ProductService;
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

    @GetMapping("/product/page")
    public Result<Paging<Product>> pageQuery(@RequestParam(value = "pagination", defaultValue = "1") Integer pagination,
                                             @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        if (pagination < 1) {
            pagination = 1;
        }
        if (pageSize < 1 || pageSize > 50) {
            pageSize = 10;
        }
        Paging<Product> paging = productService.queryPage(pagination, pageSize);
        return Result.success(paging);
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
