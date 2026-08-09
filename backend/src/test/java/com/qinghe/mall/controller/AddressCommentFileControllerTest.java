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
}
