package com.qinghe.mall.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qinghe.mall.dao.CartDAO;
import com.qinghe.mall.dataobject.CartDO;
import com.qinghe.mall.model.Cart;
import com.qinghe.mall.model.Product;
import com.qinghe.mall.model.ProductDetail;
import com.qinghe.mall.service.ProductDetailService;
import com.qinghe.mall.service.ProductService;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * CartServiceImpl 分支补强（T3 冲刺 80%）：覆盖 add 数量 null、updateQuantity 数量 null/零与
 * 规格缺失、updateSelected 选中态、list 空/规格缺失/商品缺失与各 >0 返回 false 分支、
 * fillExtra 规格/商品缺失与 firstImg 空白分支。
 */
class CartServiceImplExtraTest {

    @Mock
    private CartDAO cartDAO;
    @Mock
    private ProductDetailService productDetailService;
    @Mock
    private ProductService productService;

    @InjectMocks
    private CartServiceImpl cartService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private CartDO cartDO(Long id, Long userId, String detailId, int qty, int selected) {
        CartDO c = new CartDO();
        c.setId(id);
        c.setUserId(userId);
        c.setProductDetailId(detailId);
        c.setQuantity(qty);
        c.setSelected(selected);
        c.setGmtCreated(new Date());
        c.setGmtModified(new Date());
        return c;
    }

    private ProductDetail pd(String id, String productId, int stock) {
        ProductDetail d = new ProductDetail();
        d.setId(id);
        d.setProductId(productId);
        d.setPrice(new BigDecimal("99.00"));
        d.setSize(42.0);
        d.setStock(stock);
        return d;
    }

    private Product product(String id, String imgs) {
        Product p = new Product();
        p.setId(id);
        p.setName("商品");
        p.setProductImgs(imgs);
        return p;
    }

    // add：数量为 null → 默认 1
    @Test
    void add_quantityNull_defaultsToOne() {
        when(cartDAO.insert(any(CartDO.class))).thenReturn(1);
        when(cartDAO.findByUserAndDetail(1L, "pd001")).thenReturn(cartDO(10L, 1L, "pd001", 1, 1));
        when(productDetailService.findById("pd001")).thenReturn(pd("pd001", "p001", 100));
        when(productService.findById("p001")).thenReturn(product("p001", "img1.jpg"));
        Cart cart = cartService.add(1L, "pd001", null);
        assertEquals(1, cart.getQuantity());
    }

    // updateQuantity：数量为 null → 默认 1（不触发上限/库存拦截）
    @Test
    void updateQuantity_quantityNull_defaultsToOne() {
        when(cartDAO.findByUserId(1L)).thenReturn(Collections.singletonList(cartDO(10L, 1L, "pd001", 1, 1)));
        when(productDetailService.findById("pd001")).thenReturn(pd("pd001", "p001", 100));
        when(cartDAO.updateQuantity(10L, 1)).thenReturn(1);
        assertTrue(cartService.updateQuantity(1L, 10L, null));
    }

    // updateQuantity：数量 <= 0 → 默认 1（覆盖 quantity > 0 的 false 分支）
    @Test
    void updateQuantity_quantityZero_defaultsToOne() {
        when(cartDAO.findByUserId(1L)).thenReturn(Collections.singletonList(cartDO(10L, 1L, "pd001", 1, 1)));
        when(productDetailService.findById("pd001")).thenReturn(pd("pd001", "p001", 100));
        when(cartDAO.updateQuantity(10L, 1)).thenReturn(1);
        assertTrue(cartService.updateQuantity(1L, 10L, 0));
    }

    // updateQuantity：规格不存在 → 跳过库存校验（覆盖 productDetail == null 分支）
    @Test
    void updateQuantity_productDetailNull_skipsStockCheck() {
        when(cartDAO.findByUserId(1L)).thenReturn(Collections.singletonList(cartDO(10L, 1L, "pd001", 1, 1)));
        when(productDetailService.findById("pd001")).thenReturn(null);
        when(cartDAO.updateQuantity(10L, 5)).thenReturn(1);
        assertTrue(cartService.updateQuantity(1L, 10L, 5));
    }

    // updateSelected：选中 → 写 1（覆盖 selected ? 1 : 0 的 true 分支）
    @Test
    void updateSelected_true_writesOne() {
        when(cartDAO.findByUserId(1L)).thenReturn(Collections.singletonList(cartDO(10L, 1L, "pd001", 1, 1)));
        when(cartDAO.updateSelected(10L, 1)).thenReturn(1);
        assertTrue(cartService.updateSelected(1L, 10L, true));
        verify(cartDAO).updateSelected(10L, 1);
    }

    // list：空购物车 → 返回空（覆盖 cartDOs.isEmpty() true 分支）
    @Test
    void list_empty_returnsEmpty() {
        when(cartDAO.findByUserId(1L)).thenReturn(Collections.emptyList());
        assertTrue(cartService.list(1L).isEmpty());
    }

    // list：规格 productId 为空 → 跳过商品装配，product 为 null
    @Test
    void list_productIdBlank_skipsProductAssembly() {
        when(cartDAO.findByUserId(1L)).thenReturn(Collections.singletonList(cartDO(10L, 1L, "pd001", 1, 1)));
        when(productDetailService.findByIds(anyList())).thenReturn(Collections.singletonList(pd("pd001", "", 100)));
        List<Cart> carts = cartService.list(1L);
        assertEquals(1, carts.size());
        assertNull(carts.get(0).getProductName());
        verify(productService, never()).findByIds(anyList());
    }

    // list：购物车明细对应规格在 Map 中缺失 → productDetail 为 null 安全跳过
    @Test
    void list_productDetailMissing_mapsNullSafe() {
        when(cartDAO.findByUserId(1L)).thenReturn(Collections.singletonList(cartDO(10L, 1L, "pd002", 1, 1)));
        when(productDetailService.findByIds(anyList())).thenReturn(Collections.singletonList(pd("pd001", "p001", 100)));
        when(productService.findByIds(anyList())).thenReturn(Collections.singletonList(product("p001", "img1.jpg")));
        List<Cart> carts = cartService.list(1L);
        assertEquals(1, carts.size());
        assertNull(carts.get(0).getProductName());
    }

    // fillExtra：productId 为空 → 跳过商品查询（覆盖 fillExtra 中 isNotBlank 的 false 分支）
    @Test
    void add_fillExtra_productIdBlank_skipsProduct() {
        when(cartDAO.insert(any(CartDO.class))).thenReturn(1);
        when(cartDAO.findByUserAndDetail(1L, "pd001")).thenReturn(cartDO(10L, 1L, "pd001", 1, 1));
        when(productDetailService.findById("pd001")).thenReturn(pd("pd001", "", 100));
        Cart cart = cartService.add(1L, "pd001", 1);
        assertNull(cart.getProductName());
        verify(productService, never()).findById(any());
    }

    // fillExtra：商品不存在 → 跳过商品字段（覆盖 product != null 的 false 分支）
    @Test
    void add_fillExtra_productNull_skips() {
        when(cartDAO.insert(any(CartDO.class))).thenReturn(1);
        when(cartDAO.findByUserAndDetail(1L, "pd001")).thenReturn(cartDO(10L, 1L, "pd001", 1, 1));
        when(productDetailService.findById("pd001")).thenReturn(pd("pd001", "p001", 100));
        when(productService.findById("p001")).thenReturn(null);
        Cart cart = cartService.add(1L, "pd001", 1);
        assertNull(cart.getProductName());
    }

    // firstImg：空白图片串 → null（覆盖 StringUtils.isBlank 的 true 分支）
    @Test
    void add_firstImgBlank_returnsNull() {
        when(cartDAO.insert(any(CartDO.class))).thenReturn(1);
        when(cartDAO.findByUserAndDetail(1L, "pd001")).thenReturn(cartDO(10L, 1L, "pd001", 1, 1));
        when(productDetailService.findById("pd001")).thenReturn(pd("pd001", "p001", 100));
        when(productService.findById("p001")).thenReturn(product("p001", ""));
        Cart cart = cartService.add(1L, "pd001", 1);
        assertNull(cart.getProductImg());
    }

    // remove：删除返回 0 → false（覆盖 deleteById > 0 的 false 分支）
    @Test
    void remove_deleteReturnsZero_false() {
        when(cartDAO.findByUserId(1L)).thenReturn(Collections.singletonList(cartDO(10L, 1L, "pd001", 1, 1)));
        when(cartDAO.deleteById(10L)).thenReturn(0);
        assertFalse(cartService.remove(1L, 10L));
    }

    // removeByDetail：返回 0 → false
    @Test
    void removeByDetail_returnsZero_false() {
        when(cartDAO.deleteByUserAndDetail(1L, "pd001")).thenReturn(0);
        assertFalse(cartService.removeByDetail(1L, "pd001"));
    }

    // clearSelected：返回 0 → 0
    @Test
    void clearSelected_returnsZero() {
        when(cartDAO.deleteSelectedByUserId(1L)).thenReturn(0);
        assertEquals(0, cartService.clearSelected(1L));
    }

    // count：返回 0 → 0
    @Test
    void count_returnsZero() {
        when(cartDAO.countByUserId(1L)).thenReturn(0);
        assertEquals(0, cartService.count(1L));
    }
}
