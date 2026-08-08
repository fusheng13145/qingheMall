package com.qinghe.mall.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * 购物车服务 单元测试（RR-4 测试广度补充）
 * 覆盖：加购与数量上限、改数量（库存/上限校验）、勾选、删除、清空、列表与计数。
 */
class CartServiceImplTest {

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
        // 让 fillExtra 能组装商品/规格冗余信息，避免 NPE
        ProductDetail pd = new ProductDetail();
        pd.setId("pd001");
        pd.setProductId("p001");
        pd.setPrice(new BigDecimal("99.00"));
        pd.setSize(42.0);
        pd.setStock(100);
        when(productDetailService.findById("pd001")).thenReturn(pd);
        Product product = new Product();
        product.setId("p001");
        product.setName("测试商品");
        product.setProductImgs("img1.jpg;img2.jpg");
        when(productService.findById("p001")).thenReturn(product);
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

    @Test
    void add_nullDetailId_shouldThrow() {
        assertThrows(RuntimeException.class, () -> cartService.add(1L, null, 1));
    }

    @Test
    void add_detailNotExist_shouldThrow() {
        when(productDetailService.findById("pd999")).thenReturn(null);
        assertThrows(RuntimeException.class, () -> cartService.add(1L, "pd999", 1));
    }

    @Test
    void add_normal_shouldReturnFilledCart() {
        when(cartDAO.insert(any(CartDO.class))).thenReturn(1);
        when(cartDAO.findByUserAndDetail(1L, "pd001")).thenReturn(cartDO(10L, 1L, "pd001", 1, 1));

        Cart cart = cartService.add(1L, "pd001", 1);

        assertEquals(10L, cart.getId());
        assertEquals("测试商品", cart.getProductName());
        assertEquals(new BigDecimal("99.00"), cart.getPrice());
        verify(cartDAO).insert(any(CartDO.class));
    }

    @Test
    void add_defaultQuantityToOne_whenNotPositive() {
        when(cartDAO.insert(any(CartDO.class))).thenReturn(1);
        // 初次加购后返回数量 1
        when(cartDAO.findByUserAndDetail(1L, "pd001")).thenReturn(cartDO(10L, 1L, "pd001", 1, 1));

        Cart cart = cartService.add(1L, "pd001", 0);
        assertEquals(1, cart.getQuantity());
    }

    @Test
    void add_overMaxQuantity_shouldCapTo99() {
        when(cartDAO.insert(any(CartDO.class))).thenReturn(1);
        // 叠加后超过上限 99，应被回写为 99
        when(cartDAO.findByUserAndDetail(1L, "pd001")).thenReturn(cartDO(10L, 1L, "pd001", 150, 1));

        Cart cart = cartService.add(1L, "pd001", 150);

        assertEquals(99, cart.getQuantity());
        verify(cartDAO).updateQuantity(10L, 99);
    }

    @Test
    void updateQuantity_entryNotExist_shouldThrow() {
        when(cartDAO.findByUserId(1L)).thenReturn(Collections.emptyList());
        assertThrows(RuntimeException.class, () -> cartService.updateQuantity(1L, 9L, 2));
    }

    @Test
    void updateQuantity_overMax_shouldCapTo99() {
        when(cartDAO.findByUserId(1L)).thenReturn(Collections.singletonList(cartDO(10L, 1L, "pd001", 1, 1)));
        when(cartDAO.updateQuantity(10L, 99)).thenReturn(1);
        assertTrue(cartService.updateQuantity(1L, 10L, 200));
        verify(cartDAO).updateQuantity(10L, 99);
    }

    @Test
    void updateQuantity_overStock_shouldThrow() {
        when(cartDAO.findByUserId(1L)).thenReturn(Collections.singletonList(cartDO(10L, 1L, "pd001", 1, 1)));
        // 覆盖 setUp 中的 stock=100：库存设为 50，请求 80（<=99 不被上限截断）超过库存
        ProductDetail pdSmall = new ProductDetail();
        pdSmall.setId("pd001");
        pdSmall.setProductId("p001");
        pdSmall.setPrice(new BigDecimal("99.00"));
        pdSmall.setStock(50);
        when(productDetailService.findById("pd001")).thenReturn(pdSmall);
        assertThrows(RuntimeException.class, () -> cartService.updateQuantity(1L, 10L, 80));
        verify(cartDAO, never()).updateQuantity(anyLong(), anyInt());
    }

    @Test
    void updateSelected_entryNotExist_shouldThrow() {
        when(cartDAO.findByUserId(1L)).thenReturn(Collections.emptyList());
        assertThrows(RuntimeException.class, () -> cartService.updateSelected(1L, 9L, true));
    }

    @Test
    void updateSelected_success() {
        when(cartDAO.findByUserId(1L)).thenReturn(Collections.singletonList(cartDO(10L, 1L, "pd001", 1, 1)));
        when(cartDAO.updateSelected(10L, 0)).thenReturn(1);
        assertTrue(cartService.updateSelected(1L, 10L, false));
    }

    @Test
    void remove_notFound_shouldReturnFalse() {
        when(cartDAO.findByUserId(1L)).thenReturn(Collections.emptyList());
        assertFalse(cartService.remove(1L, 9L));
        verify(cartDAO, never()).deleteById(anyLong());
    }

    @Test
    void remove_found_shouldReturnTrue() {
        when(cartDAO.findByUserId(1L)).thenReturn(Collections.singletonList(cartDO(10L, 1L, "pd001", 1, 1)));
        when(cartDAO.deleteById(10L)).thenReturn(1);
        assertTrue(cartService.remove(1L, 10L));
    }

    @Test
    void removeByDetail_shouldDelegate() {
        when(cartDAO.deleteByUserAndDetail(1L, "pd001")).thenReturn(1);
        assertTrue(cartService.removeByDetail(1L, "pd001"));
    }

    @Test
    void clearSelected_shouldReturnCount() {
        when(cartDAO.deleteSelectedByUserId(1L)).thenReturn(3);
        assertEquals(3, cartService.clearSelected(1L));
    }

    @Test
    void list_shouldFillExtra() {
        when(cartDAO.findByUserId(1L)).thenReturn(Arrays.asList(
                cartDO(10L, 1L, "pd001", 2, 1), cartDO(11L, 1L, "pd001", 3, 0)));
        List<Cart> carts = cartService.list(1L);
        assertEquals(2, carts.size());
        assertEquals("测试商品", carts.get(0).getProductName());
        assertTrue(carts.get(0).getSelected());
        assertFalse(carts.get(1).getSelected());
    }

    @Test
    void count_shouldDelegate() {
        when(cartDAO.countByUserId(1L)).thenReturn(5);
        assertEquals(5, cartService.count(1L));
    }

    @Test
    void add_whenFillExtraReturnsNull_shouldReturnNull() {
        when(cartDAO.insert(any(CartDO.class))).thenReturn(1);
        when(cartDAO.findByUserAndDetail(1L, "pd001")).thenReturn(null);
        assertNull(cartService.add(1L, "pd001", 1));
    }
}
