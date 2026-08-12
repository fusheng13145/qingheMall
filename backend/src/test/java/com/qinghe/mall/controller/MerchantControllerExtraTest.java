package com.qinghe.mall.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qinghe.mall.dataobject.MerchantDO;
import com.qinghe.mall.dataobject.UserDO;
import com.qinghe.mall.model.Product;
import com.qinghe.mall.service.MerchantService;
import com.qinghe.mall.service.OrderService;
import com.qinghe.mall.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * MerchantController 分支覆盖补强（T3 续补）：补齐 MerchantControllerTest 未覆盖分支——
 * info 角色已为 MERCHANT 不刷新 / merchant==null；saveProduct 价格<=0 / edit 模式 existing 分支；
 * toggleProduct 商品不存在；saveProduct 新增且已显式给定 status 不走默认上架。
 */
class MerchantControllerExtraTest {

    @Mock
    private MerchantService merchantService;
    @Mock
    private ProductService productService;
    @Mock
    private OrderService orderService;
    @Mock
    private com.qinghe.mall.service.LogisticsService logisticsService;
    @Mock
    private com.qinghe.mall.service.RefundService refundService;

    @InjectMocks
    private MerchantController merchantController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(merchantController)
                .setControllerAdvice(new com.qinghe.mall.config.GlobalExceptionHandler())
                .build();
    }

    private MerchantDO merchant(Long userId, String status) {
        MerchantDO m = new MerchantDO();
        m.setId(10L);
        m.setUserId(userId);
        m.setShopName("青禾小铺");
        m.setStatus(status);
        return m;
    }

    private MockHttpSession merchantSession(Long userId) {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute("userId", userId);
        s.setAttribute("role", UserDO.ROLE_MERCHANT);
        return s;
    }

    private void mockActiveMerchant() {
        when(merchantService.getByUserId(10L)).thenReturn(merchant(10L, MerchantDO.STATUS_ACTIVE));
    }

    @Test
    void info_roleAlreadyMerchant_shouldNotChangeRole() throws Exception {
        // 会话角色已是 MERCHANT：info 内 if(!equals) 为 false 分支，不应再 setAttribute
        MockHttpSession session = merchantSession(1L);
        when(merchantService.getByUserId(1L)).thenReturn(merchant(1L, MerchantDO.STATUS_ACTIVE));

        mockMvc.perform(get("/api/merchant/info").session(session))
                .andExpect(status().isOk());

        // 角色仍为 MERCHANT，未被重复刷新（覆盖 !equals 的 false 分支）
        org.junit.jupiter.api.Assertions.assertEquals(UserDO.ROLE_MERCHANT, session.getAttribute("role"));
    }

    @Test
    void info_merchantNull_returnsSuccessNull() throws Exception {
        // 角色 MERCHANT 但商家记录不存在：覆盖 if(merchant != null) 的 false 分支
        MockHttpSession session = merchantSession(1L);
        when(merchantService.getByUserId(1L)).thenReturn(null);

        mockMvc.perform(get("/api/merchant/info").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void saveProduct_priceZero_throws() throws Exception {
        mockActiveMerchant();
        mockMvc.perform(post("/api/merchant/product/save")
                        .session(merchantSession(10L))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"低价\",\"price\":0}"))
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void saveProduct_editExistingOwnershipOk_returnsSaved() throws Exception {
        mockActiveMerchant();
        Product existing = new Product();
        existing.setId("p1");
        existing.setMerchantId(10L);
        when(productService.findById("p1")).thenReturn(existing);
        Product saved = new Product();
        saved.setId("p1");
        when(productService.saveWithDetails(any(Product.class), any())).thenReturn(saved);

        // 覆盖 edit 分支（id 非空）+ existing 非空 + 归属一致 的成功路径
        mockMvc.perform(post("/api/merchant/product/save")
                        .session(merchantSession(10L))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"p1\",\"name\":\"改\",\"price\":99}"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("p1"));
    }

    @Test
    void saveProduct_editExistingNotFound_throws() throws Exception {
        mockActiveMerchant();
        when(productService.findById("pX")).thenReturn(null);

        // 覆盖 edit 分支中 existing == null 的「商品不存在」分支
        mockMvc.perform(post("/api/merchant/product/save")
                        .session(merchantSession(10L))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"pX\",\"name\":\"改\",\"price\":99}"))
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void saveProduct_newWithStatusProvided_keepsStatus() throws Exception {
        mockActiveMerchant();
        Product saved = new Product();
        saved.setId("p-new");
        when(productService.saveWithDetails(any(Product.class), any())).thenReturn(saved);

        // 覆盖新增分支中 isBlank(status) 为 false 的分支（不强制默认上架 ON）
        mockMvc.perform(post("/api/merchant/product/save")
                        .session(merchantSession(10L))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"新增\",\"price\":99,\"status\":\"OFF\"}"))
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void toggleProduct_productNotFound_throws() throws Exception {
        mockActiveMerchant();
        when(productService.findById("pX")).thenReturn(null);

        // 覆盖 toggle 中 existing == null 的「商品不存在」分支
        mockMvc.perform(post("/api/merchant/product/toggle")
                        .session(merchantSession(10L))
                        .param("productId", "pX")
                        .param("status", "OFF"))
                .andExpect(jsonPath("$.code").value(500));
    }
}
