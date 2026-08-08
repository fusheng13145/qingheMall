package com.qinghe.mall.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qinghe.mall.dao.MerchantDAO;
import com.qinghe.mall.dataobject.MerchantDO;
import com.qinghe.mall.dataobject.UserDO;
import com.qinghe.mall.model.Paging;
import com.qinghe.mall.model.User;
import com.qinghe.mall.service.MerchantService;
import com.qinghe.mall.service.UserService;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MerchantServiceImpl implements MerchantService {

    @Autowired
    private MerchantDAO merchantDAO;

    @Autowired
    private UserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User registerMerchant(String userName, String pwd, String shopName, String shopLogo, String shopDesc) {
        if (StringUtils.isBlank(shopName)) {
            throw new RuntimeException("请填写店铺名称");
        }
        User user = userService.register(userName, pwd, UserDO.ROLE_MERCHANT);
        apply(user.getId(), shopName, shopLogo, shopDesc);
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MerchantDO apply(Long userId, String shopName, String shopLogo, String shopDesc) {
        if (userId == null) {
            throw new RuntimeException("未登录");
        }
        MerchantDO exist = merchantDAO.findByUserId(userId);
        if (exist != null) {
            return exist;
        }
        if (StringUtils.isBlank(shopName)) {
            throw new RuntimeException("请填写店铺名称");
        }
        MerchantDO merchant = new MerchantDO();
        merchant.setUserId(userId);
        merchant.setShopName(shopName.trim());
        merchant.setShopLogo(shopLogo);
        merchant.setShopDesc(shopDesc);
        merchant.setStatus(MerchantDO.STATUS_PENDING);
        merchant.setGmtCreated(LocalDateTime.now());
        merchant.setGmtModified(LocalDateTime.now());
        merchantDAO.insert(merchant);
        return merchant;
    }

    @Override
    public MerchantDO getByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        return merchantDAO.findByUserId(userId);
    }

    @Override
    public MerchantDO getById(Long id) {
        if (id == null) {
            return null;
        }
        return merchantDAO.findById(id);
    }

    @Override
    public Paging<MerchantDO> list(String status, int pageNum, int pageSize) {
        Page<MerchantDO> page = PageHelper.startPage(pageNum, pageSize)
                .doSelectPage(() -> merchantDAO.query(status));
        Paging<MerchantDO> paging = new Paging<>();
        paging.setPageNum(pageNum);
        paging.setPageSize(pageSize);
        paging.setTotalPage(page.getPages());
        paging.setTotalCount(page.getTotal());
        paging.setData(page.getResult());
        return paging;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MerchantDO audit(Long merchantId, boolean approve, String reason) {
        MerchantDO merchant = getById(merchantId);
        if (merchant == null) {
            throw new RuntimeException("商家不存在");
        }
        if (approve) {
            merchantDAO.updateStatus(merchantId, MerchantDO.STATUS_ACTIVE, null);
            // 审核通过后确保账号角色为商家
            userService.updateRole(merchant.getUserId(), UserDO.ROLE_MERCHANT);
        } else {
            if (StringUtils.isBlank(reason)) {
                throw new RuntimeException("驳回必须填写原因");
            }
            merchantDAO.updateStatus(merchantId, MerchantDO.STATUS_REJECTED, reason.trim());
        }
        return getById(merchantId);
    }
}
