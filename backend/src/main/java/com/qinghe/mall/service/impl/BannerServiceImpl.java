package com.qinghe.mall.service.impl;

import com.qinghe.mall.dao.BannerDAO;
import com.qinghe.mall.dataobject.BannerDO;
import com.qinghe.mall.exception.BusinessException;
import com.qinghe.mall.model.Paging;
import com.qinghe.mall.service.BannerService;
import com.qinghe.mall.util.UUIDUtils;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BannerServiceImpl implements BannerService {

    @Autowired
    private BannerDAO bannerDAO;

    @Override
    public BannerDO createBanner(BannerDO banner) {
        validateContent(banner);
        banner.setId(UUIDUtils.uuid());
        if (banner.getSortOrder() == null) {
            banner.setSortOrder(0);
        }
        if (StringUtils.isBlank(banner.getStatus())) {
            banner.setStatus("ON");
        }
        Date now = new Date();
        banner.setGmtCreated(now);
        banner.setGmtModified(now);
        bannerDAO.insert(banner);
        return banner;
    }

    @Override
    public BannerDO updateBanner(BannerDO banner) {
        if (banner == null || StringUtils.isBlank(banner.getId())) {
            throw new BusinessException("运营位ID不能为空");
        }
        validateContent(banner);
        BannerDO existing = bannerDAO.findById(banner.getId());
        if (existing == null) {
            throw new BusinessException("运营位不存在");
        }
        if (banner.getSortOrder() == null) {
            banner.setSortOrder(existing.getSortOrder());
        }
        banner.setGmtModified(new Date());
        bannerDAO.update(banner);
        return bannerDAO.findById(banner.getId());
    }

    @Override
    public void deleteBanner(String bannerId) {
        requireExisting(bannerId);
        bannerDAO.deleteById(bannerId);
    }

    @Override
    public void toggleBanner(String bannerId, String status) {
        requireExisting(bannerId);
        if (!"ON".equals(status) && !"OFF".equals(status)) {
            throw new BusinessException("非法的运营位状态");
        }
        bannerDAO.updateStatus(bannerId, status);
    }

    @Override
    public Paging<BannerDO> listBanners(int pageNum, int pageSize) {
        if (pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize < 1 || pageSize > 50) {
            pageSize = 10;
        }
        Page<BannerDO> page = PageHelper.startPage(pageNum, pageSize)
                .doSelectPage(() -> bannerDAO.findAll());
        Paging<BannerDO> paging = new Paging<>();
        paging.setPageNum(pageNum);
        paging.setPageSize(pageSize);
        paging.setTotalPage(page.getPages());
        paging.setTotalCount(page.getTotal());
        paging.setData(page.getResult());
        return paging;
    }

    @Override
    public List<BannerDO> listActive() {
        return bannerDAO.findActive();
    }

    private void validateContent(BannerDO banner) {
        if (banner == null || StringUtils.isBlank(banner.getTitle())) {
            throw new BusinessException("运营位标题不能为空");
        }
        if (StringUtils.isBlank(banner.getImage())) {
            throw new BusinessException("运营位图片不能为空");
        }
        if (banner.getTitle().length() > 64) {
            throw new BusinessException("运营位标题过长（≤64 字符）");
        }
    }

    private void requireExisting(String bannerId) {
        if (StringUtils.isBlank(bannerId)) {
            throw new BusinessException("运营位ID不能为空");
        }
        if (bannerDAO.findById(bannerId) == null) {
            throw new BusinessException("运营位不存在");
        }
    }
}
