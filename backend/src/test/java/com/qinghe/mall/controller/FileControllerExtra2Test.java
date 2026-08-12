package com.qinghe.mall.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qinghe.mall.config.GlobalExceptionHandler;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

/**
 * FileController 分支覆盖补强（T3 续补）：补齐 FileController 上传主链路剩余分支——
 * file==null、originalName 为 null、文件名无扩展名、各扩展名魔数 false 分支、
 * 成功路径 originalName 空白命名、两处 IOException 捕获。
 */
class FileControllerExtra2Test {

    @InjectMocks
    private FileController fileController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(fileController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private MockHttpSession session(Long userId) {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute("userId", userId);
        return s;
    }

    /** PNG 文件头（有效） */
    private byte[] pngHeader() {
        return new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    }

    @Test
    void upload_noFileParam_fileNull_rejected() throws Exception {
        // 不上传 file 参数 → MultipartFile 为 null → 覆盖 file==null 分支
        mockMvc.perform(multipart("/api/file/upload").session(session(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void upload_originalNameNull_treatedAsEmpty_rejected() throws Exception {
        // originalFilename 为 null → originalName=="" → lastIndexOf('.')<0 → 扩展名空 → 拒绝
        MockMultipartFile file = new MockMultipartFile("file", null, "image/png", pngHeader());
        mockMvc.perform(multipart("/api/file/upload").file(file).session(session(1L)))
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void upload_noDotInName_rejected() throws Exception {
        // 文件名无扩展名（非 null）→ 覆盖 dot<0 分支
        MockMultipartFile file = new MockMultipartFile("file", "picture", "image/png", pngHeader());
        mockMvc.perform(multipart("/api/file/upload").file(file).session(session(1L)))
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void upload_pngWrongMagic_rejected() throws Exception {
        // 扩展名 png 但魔数不匹配 → 覆盖 matchesMagic png 的 false 分支
        byte[] bad = new byte[]{0, 1, 2, 3, 4, 5, 6, 7};
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", bad);
        mockMvc.perform(multipart("/api/file/upload").file(file).session(session(1L)))
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void upload_gifWrongMagic_rejected() throws Exception {
        byte[] bad = new byte[]{0, 1, 2, 3, 4, 5, 6, 7};
        MockMultipartFile file = new MockMultipartFile("file", "a.gif", "image/gif", bad);
        mockMvc.perform(multipart("/api/file/upload").file(file).session(session(1L)))
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void upload_webpWrongMagic_rejected() throws Exception {
        byte[] bad = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
        MockMultipartFile file = new MockMultipartFile("file", "a.webp", "image/webp", bad);
        mockMvc.perform(multipart("/api/file/upload").file(file).session(session(1L)))
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void upload_jpegExt_validMagic_success() throws Exception {
        // 扩展名 jpeg（非 jpg）+ 合法 JPEG 魔数 → 覆盖 matchesMagic jpeg 的 true 分支
        org.springframework.test.util.ReflectionTestUtils.setField(fileController, "uploadDir",
                java.nio.file.Files.createTempDirectory("qh-upload").toString());
        byte[] jpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x00, 0x00, 0, 0};
        MockMultipartFile file = new MockMultipartFile("file", "a.jpeg", "image/jpeg", jpeg);
        mockMvc.perform(multipart("/api/file/upload").file(file).session(session(1L)))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.url").value(org.hamcrest.Matchers.startsWith("/uploads/")));
    }

    @Test
    void upload_ioErrorOnRead_caught_rejected() throws Exception {
        // getInputStream 抛 IOException → 覆盖第一处 catch（文件读取失败）
        // MockMvc 的 .file() 仅接受 MockMultipartFile，无法注入抛异常的 MultipartFile，
        // 故直接调用控制器方法并桩化 HttpServletRequest 会话。
        jakarta.servlet.http.HttpServletRequest req = org.mockito.Mockito.mock(jakarta.servlet.http.HttpServletRequest.class);
        jakarta.servlet.http.HttpSession sess = org.mockito.Mockito.mock(jakarta.servlet.http.HttpSession.class);
        org.mockito.Mockito.when(req.getSession()).thenReturn(sess);
        org.mockito.Mockito.when(sess.getAttribute("userId")).thenReturn(1L);

        MultipartFile bad = new ThrowingInputStreamFile("a.png");
        com.qinghe.mall.model.Result<?> result = fileController.upload(bad, req);
        org.junit.jupiter.api.Assertions.assertEquals(500, result.getCode());
    }

    @Test
    void upload_ioErrorOnWrite_caught_rejected() throws Exception {
        // uploadDir 指向一个已存在的普通文件 → Files.copy 因父路径非目录而抛 IOException
        // → 覆盖第二处 catch（文件保存失败）
        try {
            File f = File.createTempFile("qh-upload-notdir", ".tmp");
            f.deleteOnExit();
            org.springframework.test.util.ReflectionTestUtils.setField(fileController, "uploadDir", f.getAbsolutePath());
        } catch (IOException e) {
            org.junit.jupiter.api.Assertions.fail("无法创建临时文件");
        }
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", pngHeader());
        mockMvc.perform(multipart("/api/file/upload").file(file).session(session(1L)))
                .andExpect(jsonPath("$.code").value(500));
    }

    /** 自定义 MultipartFile：getInputStream 直接抛出 IOException，用于覆盖读取异常分支。 */
    static class ThrowingInputStreamFile implements MultipartFile {
        private final String name;
        ThrowingInputStreamFile(String name) { this.name = name; }
        public String getName() { return name; }
        public String getOriginalFilename() { return name; }
        public String getContentType() { return "image/png"; }
        public boolean isEmpty() { return false; }
        public long getSize() { return 12; }
        public byte[] getBytes() { return new byte[0]; }
        public InputStream getInputStream() throws IOException { throw new IOException("simulated read failure"); }
        public void transferTo(File dest) { /* noop */ }
    }
}
