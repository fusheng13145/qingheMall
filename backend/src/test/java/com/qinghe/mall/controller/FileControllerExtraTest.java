package com.qinghe.mall.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qinghe.mall.config.GlobalExceptionHandler;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * FileController 分支覆盖补充（T3 续补）。
 * 覆盖 upload() 未登录 / 空文件 / 非图片扩展名 / 超大 / 魔数不符 / 无扩展名 /
 * originalName 为 null / 真实 jpg·png·gif·webp 头 等分支。
 */
class FileControllerExtraTest {

    private FileController controller = new FileController();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // standaloneSetup 不解析 @Value，显式注入临时上传目录，避免 NPE 且不在仓库落文件
        ReflectionTestUtils.setField(controller, "uploadDir", "target/test-upload-files");
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private MockHttpSession userSession() {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute("userId", 1L);
        return s;
    }

    @Test
    void upload_unauthenticated_401() throws Exception {
        MockMultipartFile f = new MockMultipartFile("file", "a.png", "image/png", pngBytes());
        mockMvc.perform(multipart("/api/file/upload").file(f))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void upload_emptyFile_400() throws Exception {
        MockMultipartFile f = new MockMultipartFile("file", "a.png", "image/png", new byte[0]);
        mockMvc.perform(multipart("/api/file/upload").file(f).session(userSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void upload_nonImageExt_400() throws Exception {
        MockMultipartFile f = new MockMultipartFile("file", "doc.txt", "text/plain", "hello".getBytes());
        mockMvc.perform(multipart("/api/file/upload").file(f).session(userSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void upload_oversize_400() throws Exception {
        byte[] big = new byte[5 * 1024 * 1024 + 1];
        MockMultipartFile f = new MockMultipartFile("file", "big.png", "image/png", big);
        mockMvc.perform(multipart("/api/file/upload").file(f).session(userSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void upload_magicMismatch_400() throws Exception {
        // 声称 png，但内容非真实图片头
        MockMultipartFile f = new MockMultipartFile("file", "x.png", "image/png", "not really png".getBytes());
        mockMvc.perform(multipart("/api/file/upload").file(f).session(userSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void upload_originalNameNull_400() throws Exception {
        // originalFilename=null -> ext=""，ALLOWED_EXT 不含空串
        MockMultipartFile f = new MockMultipartFile("file", null, "image/png", pngBytes());
        mockMvc.perform(multipart("/api/file/upload").file(f).session(userSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void upload_noExtension_400() throws Exception {
        MockMultipartFile f = new MockMultipartFile("file", "image", "application/octet-stream", pngBytes());
        mockMvc.perform(multipart("/api/file/upload").file(f).session(userSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void upload_realJpg_200() throws Exception {
        MockMultipartFile f = new MockMultipartFile("file", "a.jpg", "image/jpeg", jpgBytes());
        mockMvc.perform(multipart("/api/file/upload").file(f).session(userSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.url").exists());
    }

    @Test
    void upload_realPng_200() throws Exception {
        MockMultipartFile f = new MockMultipartFile("file", "a.png", "image/png", pngBytes());
        mockMvc.perform(multipart("/api/file/upload").file(f).session(userSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void upload_realGif_200() throws Exception {
        MockMultipartFile f = new MockMultipartFile("file", "a.gif", "image/gif", gifBytes());
        mockMvc.perform(multipart("/api/file/upload").file(f).session(userSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void upload_realWebp_200() throws Exception {
        MockMultipartFile f = new MockMultipartFile("file", "a.webp", "image/webp", webpBytes());
        mockMvc.perform(multipart("/api/file/upload").file(f).session(userSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    private byte[] jpgBytes() {
        return new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01};
    }

    private byte[] pngBytes() {
        return new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D};
    }

    private byte[] gifBytes() {
        return new byte[]{0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 0x01, 0x00, 0x01, 0x00, 0x00, 0x00};
    }

    private byte[] webpBytes() {
        return new byte[]{0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00, 0x57, 0x45, 0x42, 0x50};
    }
}
