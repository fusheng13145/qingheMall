package com.qinghe.mall.service.impl;

import com.qinghe.mall.dao.CartDAO;
import com.qinghe.mall.dataobject.CartDO;
import com.qinghe.mall.model.Cart;
import com.qinghe.mall.model.Product;
import com.qinghe.mall.model.ProductDetail;
import com.qinghe.mall.service.CartService;
import com.qinghe.mall.service.ProductDetailService;
import com.qinghe.mall.service.ProductService;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CartServiceImpl implements CartService {

    /** 单条目数量上限 */
    private static final int MAX_QUANTITY = 99;

    @Autowired
    private CartDAO cartDAO;

    @Autowired
    private ProductDetailService productDetailService;

    @Autowired
    private ProductService productService;

    @Override
    public Cart add(Long userId, String productDetailId, Integer quantity) {
        if (productDetailId == null) {
            throw new RuntimeException("商品规格ID不能为空");
        }
        ProductDetail productDetail = productDetailService.findById(productDetailId);
        if (productDetail == null) {
            throw new RuntimeException("商品规格不存在");
        }
        int qty = quantity != null && quantity > 0 ? quantity : 1;

        CartDO cartDO = new CartDO();
        cartDO.setUserId(userId);
        cartDO.setProductDetailId(productDetailId);
        cartDO.setQuantity(qty);
        cartDO.setSelected(1);
        cartDO.setGmtCreated(new Date());
        cartDO.setGmtModified(new Date());
        cartDAO.insert(cartDO); // ON DUPLICATE KEY 数量叠加

        CartDO latest = cartDAO.findByUserAndDetail(userId, productDetailId);
        if (latest != null && latest.getQuantity() > MAX_QUANTITY) {
            cartDAO.updateQuantity(latest.getId(), MAX_QUANTITY);
            latest.setQuantity(MAX_QUANTITY);
        }
        return latest != null ? fillExtra(latest) : null;
    }

    @Override
    public boolean updateQuantity(Long userId, Long id, Integer quantity) {
        CartDO cartDO = cartDAO.findByUserId(userId).stream()
                .filter(c -> c.getId().equals(id))
                .findFirst().orElse(null);
        if (cartDO == null) {
            throw new RuntimeException("购物车条目不存在");
        }
        int qty = quantity != null && quantity > 0 ? quantity : 1;
        if (qty > MAX_QUANTITY) {
            qty = MAX_QUANTITY;
        }
        // 不超过库存
        ProductDetail productDetail = productDetailService.findById(cartDO.getProductDetailId());
        if (productDetail != null && qty > productDetail.getStock()) {
            throw new RuntimeException("超出库存，最多可购买 " + productDetail.getStock() + " 件");
        }
        return cartDAO.updateQuantity(id, qty) > 0;
    }

    @Override
    public boolean updateSelected(Long userId, Long id, boolean selected) {
        CartDO cartDO = cartDAO.findByUserId(userId).stream()
                .filter(c -> c.getId().equals(id))
                .findFirst().orElse(null);
        if (cartDO == null) {
            throw new RuntimeException("购物车条目不存在");
        }
        return cartDAO.updateSelected(id, selected ? 1 : 0) > 0;
    }

    @Override
    public boolean remove(Long userId, Long id) {
        CartDO cartDO = cartDAO.findByUserId(userId).stream()
                .filter(c -> c.getId().equals(id))
                .findFirst().orElse(null);
        if (cartDO == null) {
            return false;
        }
        return cartDAO.deleteById(id) > 0;
    }

    @Override
    public boolean removeByDetail(Long userId, String productDetailId) {
        return cartDAO.deleteByUserAndDetail(userId, productDetailId) > 0;
    }

    @Override
    public int clearSelected(Long userId) {
        return cartDAO.deleteSelectedByUserId(userId);
    }

    @Override
    public List<Cart> list(Long userId) {
        List<CartDO> cartDOs = cartDAO.findByUserId(userId);
        List<Cart> carts = new ArrayList<>();
        for (CartDO cartDO : cartDOs) {
            carts.add(fillExtra(cartDO));
        }
        return carts;
    }

    @Override
    public int count(Long userId) {
        return cartDAO.countByUserId(userId);
    }

    /** 组装商品/规格冗余信息 */
    private Cart fillExtra(CartDO cartDO) {
        Cart cart = cartDO.convertToModel();
        ProductDetail productDetail = productDetailService.findById(cartDO.getProductDetailId());
        if (productDetail != null) {
            cart.setProductId(productDetail.getProductId());
            cart.setSize(productDetail.getSize());
            cart.setPrice(productDetail.getPrice());
            cart.setStock(productDetail.getStock());
            if (StringUtils.isNotBlank(productDetail.getProductId())) {
                Product product = productService.findById(productDetail.getProductId());
                if (product != null) {
                    cart.setProductName(product.getName());
                    cart.setProductImg(firstImg(product.getProductImgs()));
                }
            }
        }
        return cart;
    }

    private String firstImg(String productImgs) {
        if (StringUtils.isBlank(productImgs)) {
            return null;
        }
        for (String part : productImgs.split("[;\\s]+")) {
            if (StringUtils.isNotBlank(part)) {
                return part.trim();
            }
        }
        return null;
    }
}
