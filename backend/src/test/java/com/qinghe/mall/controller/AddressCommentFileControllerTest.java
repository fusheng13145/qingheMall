package com.qinghe.mall.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qinghe.mall.config.GlobalExceptionHandler;
import com.qinghe.mall.model.Address;
import com.qinghe.mall.model.Comment;
import com.qinghe.mall.model.Result;
import com.qinghe.mall.service.AddressService;
import com.qinghe.mall.service.CommentService;
import com.qinghe.mall.service.OrderService;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * 收货地址 + 评价 + 文件上传控制器 单元测试（遗留D 第三批：补齐最后 3 个零覆盖 Controller）。
 */
class AddressCommentFileControllerTest {

    @Mock
    private AddressService addressService;

    @Mock
    private CommentService commentService;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private AddressController addressController;

    @InjectMocks
    private CommentController commentController;

    @InjectMocks
    private FileController fileController;

    private MockMvc addressMvc;
    private MockMvc commentMvc;
    private MockMvc fileMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        addressMvc = MockMvcBuilders.standaloneSetup(addressController)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        commentMvc = MockMvcBuilders.standaloneSetup(commentController)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        fileMvc = MockMvcBuilders.standaloneSetup(fileController)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private MockHttpSession session(Long userId) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", userId);
        return session;
    }

    // ============ 收货地址 ============

    @Test
    void addressList_unauthenticated_401() throws Exception {
        addressMvc.perform(get("/api/address/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void addressList_authenticated_returnsList() throws Exception {
        when(addressService.list(1L)).thenReturn(Collections.emptyList());

        addressMvc.perform(get("/api/address/list").session(session(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void addressAdd_authenticated_delegates() throws Exception {
        Address created = new Address();
        created.setId(1L);
        when(addressService.add(anyLong(), any())).thenReturn(created);

        addressMvc.perform(post("/api/address/add").session(session(1L))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"receiverName\":\"张三\",\"receiverPhone\":\"13800138000\",\"receiverAddress\":\"北京市朝阳区\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ============ 评价 ============

    @Test
    void commentAdd_unauthenticated_401() throws Exception {
        commentMvc.perform(post("/api/comment/add")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"p001\",\"orderNumber\":\"O1\",\"rating\":5,\"content\":\"很好\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void commentProduct_public_returnsList() throws Exception {
        com.qinghe.mall.model.Paging<Comment> paging = new com.qinghe.mall.model.Paging<>();
        paging.setPageNum(1);
        paging.setPageSize(10);
        paging.setTotalCount(0L);
        paging.setData(Collections.emptyList());
        when(commentService.listByProduct("p001", 1, 10)).thenReturn(paging);

        commentMvc.perform(get("/api/comment/product").param("productId", "p001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void commentAdd_authenticated_delegates() throws Exception {
        Comment created = new Comment();
        created.setId("c1");
        when(commentService.addComment(1L, "p001", "O1", 5, "很好")).thenReturn(created);

        commentMvc.perform(post("/api/comment/add").session(session(1L))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"p001\",\"orderNumber\":\"O1\",\"rating\":5,\"content\":\"很好\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(commentService).addComment(anyLong(), anyString(), anyString(), any(), anyString());
    }

    // ============ 文件上传 ============

    @Test
    void upload_unauthenticated_401() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[]{1, 2, 3});
        fileMvc.perform(multipart("/api/file/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void upload_invalidMagic_rejected() throws Exception {
        // 非图片魔数（1,2,3）→ 文件类型校验拒绝（P1-13 魔数校验），无副作用
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[]{1, 2, 3});
        fileMvc.perform(multipart("/api/file/upload").file(file).session(session(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ========== 补充用例（P1 覆盖补强） ==========

    @org.junit.jupiter.api.io.TempDir
    java.nio.file.Path tempDir;

    /** 真实 PNG 文件头（89 50 4E 47 0D 0A 1A 0A）+ 截断填充 */
    private byte[] pngHeader() {
        byte[] header = new byte[12];
        byte[] magic = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        System.arraycopy(magic, 0, header, 0, magic.length);
        return header;
    }

    @Test
    void upload_validPng_success() throws Exception {
        // 注入临时上传目录（避免污染项目 upload/）
        org.springframework.test.util.ReflectionTestUtils.setField(fileController, "uploadDir", tempDir.toString());
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", pngHeader());
        fileMvc.perform(multipart("/api/file/upload").file(file).session(session(1L)))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.url").value(org.hamcrest.Matchers.startsWith("/uploads/")));
    }

    @Test
    void upload_invalidExt_rejected() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", pngHeader());
        fileMvc.perform(multipart("/api/file/upload").file(file).session(session(1L)))
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void upload_emptyFile_rejected() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[0]);
        fileMvc.perform(multipart("/api/file/upload").file(file).session(session(1L)))
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void upload_exceed5MB_rejected() throws Exception {
        byte[] big = new byte[5 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", big);
        fileMvc.perform(multipart("/api/file/upload").file(file).session(session(1L)))
                .andExpect(jsonPath("$.code").value(500));
    }

    // ========== 地址：更新/删除/默认 ==========

    @Test
    void addressUpdate_authenticated_delegates() throws Exception {
        com.qinghe.mall.model.Address updated = new com.qinghe.mall.model.Address();
        when(addressService.update(org.mockito.Mockito.eq(1L),
                org.mockito.ArgumentMatchers.any(com.qinghe.mall.model.Address.class)))
                .thenReturn(updated);

        addressMvc.perform(post("/api/address/update")
                        .session(session(1L))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,\"receiverName\":\"张三\"}"))
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void addressDelete_authenticated_delegates() throws Exception {
        when(addressService.delete(1L, 1L)).thenReturn(true);
        addressMvc.perform(post("/api/address/delete").session(session(1L)).param("id", "1"))
                .andExpect(jsonPath("$.code").value(200));
        org.mockito.Mockito.verify(addressService).delete(1L, 1L);
    }

    @Test
    void addressSetDefault_authenticated_delegates() throws Exception {
        addressMvc.perform(post("/api/address/setDefault").session(session(1L)).param("id", "1"))
                .andExpect(jsonPath("$.code").value(200));
        org.mockito.Mockito.verify(addressService).setDefault(1L, 1L);
    }

    // ========== 魔数全分支（matchesMagic 覆盖补强） ==========

    @Test
    void upload_validGif_success() throws Exception {
        org.springframework.test.util.ReflectionTestUtils.setField(fileController, "uploadDir", tempDir.toString());
        MockMultipartFile file = new MockMultipartFile("file", "a.gif", "image/gif",
                "GIF89a".getBytes());
        fileMvc.perform(multipart("/api/file/upload").file(file).session(session(1L)))
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void upload_validWebp_success() throws Exception {
        org.springframework.test.util.ReflectionTestUtils.setField(fileController, "uploadDir", tempDir.toString());
        byte[] webp = "RIFF1234WEBP".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "a.webp", "image/webp", webp);
        fileMvc.perform(multipart("/api/file/upload").file(file).session(session(1L)))
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void upload_validJpeg_success() throws Exception {
        org.springframework.test.util.ReflectionTestUtils.setField(fileController, "uploadDir", tempDir.toString());
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x00, 0x00};
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", jpeg);
        fileMvc.perform(multipart("/api/file/upload").file(file).session(session(1L)))
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void upload_pngHeaderClaimedJpg_rejected() throws Exception {
        // PNG 文件头配 .jpg 扩展名 → 魔数与扩展名不符拒绝（防伪装）
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
        fileMvc.perform(multipart("/api/file/upload").file(file).session(session(1L)))
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void upload_shortHeader_rejected() throws Exception {
        // 文件头不足 4 字节 → 无法判定魔数，拒绝
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1, 2});
        fileMvc.perform(multipart("/api/file/upload").file(file).session(session(1L)))
                .andExpect(jsonPath("$.code").value(500));
    }

    // ========== 评价状态接口（归属校验，P1 覆盖补强） ==========

    @Test
    void orderStatus_unauthenticated_401() throws Exception {
        commentMvc.perform(get("/api/comment/orderStatus").param("orderNumber", "O1"))
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void orderStatus_orderNotFound_fails() throws Exception {
        when(orderService.findByOrderNumber("O1")).thenReturn(null);
        commentMvc.perform(get("/api/comment/orderStatus").session(session(1L)).param("orderNumber", "O1"))
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void orderStatus_notOwner_forbidden() throws Exception {
        com.qinghe.mall.model.Order order = new com.qinghe.mall.model.Order();
        order.setOrderNumber("O1");
        order.setUserId(99L);
        when(orderService.findByOrderNumber("O1")).thenReturn(order);

        commentMvc.perform(get("/api/comment/orderStatus").session(session(1L)).param("orderNumber", "O1"))
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void orderStatus_owner_returnsCommented() throws Exception {
        com.qinghe.mall.model.Order order = new com.qinghe.mall.model.Order();
        order.setOrderNumber("O1");
        order.setUserId(1L);
        when(orderService.findByOrderNumber("O1")).thenReturn(order);
        when(commentService.hasCommented("O1")).thenReturn(true);

        commentMvc.perform(get("/api/comment/orderStatus").session(session(1L)).param("orderNumber", "O1"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.commented").value(true));
    }
}
