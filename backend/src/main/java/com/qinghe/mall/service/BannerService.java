package com.qinghe.mall.service;

import com.qinghe.mall.dataobject.BannerDO;
import com.qinghe.mall.model.Paging;
import java.util.List;

/**
 * 首页运营位（D2，v1.7）：管理端配置、顾客端展示。
 */
public interface BannerService {

    /** 新建运营位（title/image 必填，默认上架、排序 0） */
    BannerDO createBanner(BannerDO banner);

    /** 更新运营位内容（标题/图片/链接/排序；状态走 toggle） */
    BannerDO updateBanner(BannerDO banner);

    /** 删除运营位 */
    void deleteBanner(String bannerId);

    /** 上下架（status: ON / OFF） */
    void toggleBanner(String bannerId, String status);

    /** 管理端全量分页（含下架） */
    Paging<BannerDO> listBanners(int pageNum, int pageSize);

    /** 顾客端仅上架位，sort_order 升序、新置顶 */
    List<BannerDO> listActive();
}
