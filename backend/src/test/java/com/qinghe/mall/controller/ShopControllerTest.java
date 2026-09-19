package com.qinghe.mall.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.qinghe.mall.dao.CommentDAO;
import com.qinghe.mall.dao.ProductDAO;
import com.qinghe.mall.dataobject.CouponDO;
import com.qinghe.mall.dataobject.MerchantDO;
import com.qinghe.mall.dataobject.SeckillActivityDO;
import com.qinghe.mall.model.Paging;
import com.qinghe.mall.model.Product;
import com.qinghe.mall.service.CouponService;
import com.qinghe.mall.service.MerchantService;
import com.qinghe.mall.service.ProductService;
import com.qinghe.mall.service.SeckillService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 店铺主页聚合单元测试（A3，v1.5）。
 *
 * 覆盖：正常聚合（店铺信息/评分销量统计/在售商品/店铺券/秒杀）、
 * 非营业店铺与非法入参拒绝、评分空值兜底、分页参数收敛。
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class ShopControllerTest {

    @Mock
    private MerchantService merchantService;

    @Mock
    private ProductService productService;

    @Mock
    private CouponService couponService;

    @Mock
    private SeckillService seckillService;

    @Mock
    private CommentDAO commentDAO;

    @Mock
    private ProductDAO productDAO;

    @InjectMocks
    private ShopController shopController;

    private MerchantDO merchant;

    @BeforeEach
    void setUp() {
        merchant = new MerchantDO();
        merchant.setId(5L);
        merchant.setShopName("青禾旗舰店");
        merchant.setShopDesc("品质生活");
        merchant.setStatus("ACTIVE");
    }

    @Test
    @DisplayName("shopHome 正常聚合：店铺信息 + 统计 + 在售商品 + 店铺券 + 秒杀")
    void shopHome_aggregatesAll() {
        when(merchantService.getById(5L)).thenReturn(merchant);
        Paging<Product> products = new Paging<>();
        products.setTotalCount(3L);
        products.setData(List.of(new Product()));
        // productCount 统计调用 (1,1) + 商品分页调用 (1,12)
        when(productService.queryMerchantPage(eq(5L), any(), eq("ON"), eq(1), eq(1))).thenReturn(products);
        when(productService.queryMerchantPage(eq(5L), any(), eq("ON"), eq(1), eq(12))).thenReturn(products);
        when(commentDAO.avgRatingByMerchant(5L)).thenReturn(4.56);
        when(productDAO.sumSalesByMerchant(5L)).thenReturn(321L);
        when(couponService.listActiveByMerchant(5L)).thenReturn(List.of(new CouponDO()));
        when(seckillService.listOngoingByMerchant(5L)).thenReturn(List.of(new SeckillActivityDO()));

        @SuppressWarnings("unchecked")
        Map<String, Object> home = (Map<String, Object>) shopController.shopHome(5L, 1, 12, null).getData();

        Map<String, Object> shop = (Map<String, Object>) home.get("shop");
        assertEquals("青禾旗舰店", shop.get("shopName"));
        Map<String, Object> stats = (Map<String, Object>) home.get("stats");
        assertEquals(3, ((Number) stats.get("productCount")).intValue());
        assertEquals(4.6, (double) stats.get("avgRating")); // 评分四舍五入 1 位
        assertEquals(321L, ((Number) stats.get("totalSales")).longValue());
        assertEquals(1, ((List<?>) home.get("coupons")).size());
        assertEquals(1, ((List<?>) home.get("seckills")).size());
        assertNotNull(home.get("products"));
    }

    @Test
    @DisplayName("shopHome 非营业/不存在店铺与非法 merchantId 拒绝")
    void shopHome_guards() {
        when(merchantService.getById(5L)).thenReturn(null);
        assertThrows(RuntimeException.class, () -> shopController.shopHome(5L, 1, 12, null));

        merchant.setStatus("DISABLED");
        when(merchantService.getById(5L)).thenReturn(merchant);
        assertThrows(RuntimeException.class, () -> shopController.shopHome(5L, 1, 12, null));

        assertThrows(RuntimeException.class, () -> shopController.shopHome(0L, 1, 12, null));
        assertThrows(RuntimeException.class, () -> shopController.shopHome(null, 1, 12, null));
        assertThrows(RuntimeException.class, () -> shopController.shopHome(-1L, 1, 12, null));
    }

    @Test
    @DisplayName("shopHome 无评分兜底 0 分，越界分页参数收敛")
    void shopHome_defaultRatingAndClamp() {
        when(merchantService.getById(5L)).thenReturn(merchant);
        Paging<Product> products = new Paging<>();
        products.setData(List.of());
        when(productService.queryMerchantPage(eq(5L), any(), eq("ON"), eq(1), eq(1))).thenReturn(products);
        when(productService.queryMerchantPage(eq(5L), any(), eq("ON"), eq(1), eq(12))).thenReturn(products);
        when(commentDAO.avgRatingByMerchant(5L)).thenReturn(null);
        when(productDAO.sumSalesByMerchant(5L)).thenReturn(0L);
        when(couponService.listActiveByMerchant(5L)).thenReturn(List.of());
        when(seckillService.listOngoingByMerchant(5L)).thenReturn(List.of());
        // 越界 (0,999) 与 null 参数均收敛为默认 (1,12)
        when(productService.queryMerchantPage(eq(5L), any(), eq("ON"), eq(1), eq(12))).thenReturn(products);

        @SuppressWarnings("unchecked")
        Map<String, Object> home = (Map<String, Object>) shopController.shopHome(5L, 0, 999, null).getData();
        shopController.shopHome(5L, null, null, null);

        assertEquals(0, (double) ((Map<String, Object>) home.get("stats")).get("avgRating"));
        // 分页参数 (0, 999) 被收敛为 (1, 12)——上方 queryMerchantPage stub 仅匹配 (1, 12)，匹配即证收敛
        assertNotNull(home.get("products"));
    }
}
