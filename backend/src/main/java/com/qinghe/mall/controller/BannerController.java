package com.qinghe.mall.controller;

import com.qinghe.mall.dataobject.BannerDO;
import com.qinghe.mall.model.Result;
import com.qinghe.mall.service.BannerService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页运营位（D2，v1.7）：顾客端公开接口，仅返回上架位（排序小在前、新置顶）。
 */
@RestController
@RequestMapping("/api/banner")
public class BannerController {

    @Autowired
    private BannerService bannerService;

    @GetMapping("/list")
    public Result<List<BannerDO>> list() {
        return Result.success(bannerService.listActive());
    }
}
