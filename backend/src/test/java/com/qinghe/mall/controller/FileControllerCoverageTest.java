package com.qinghe.mall.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.qinghe.mall.model.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.multipart.MultipartFile;

/**
 * FileController 分支覆盖补测（#37）：覆盖 upload 的登录/空文件/扩展名/大小/魔数校验分支，
 * 以及 matchesMagic 各图片格式（jpg/png/gif/webp）与边界（null/过短/未知扩展名）分支。
 * 通过反射直接调用私有 matchesMagic 以覆盖纯 Mockito 下不易触达的多格式分支。
 */
class FileControllerCoverageTest {

    @Mock
    private MultipartFile file;
    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private FileController fileController;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        org.springframework.test.util.ReflectionTestUtils.setField(
                fileController, "uploadDir", tempDir.toString());
        HttpSession session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(1L);
    }

    @Test
    void upload_notLoggedIn_shouldFail() {
        HttpSession session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(null);
        Result<?> r = fileController.upload(file, request);
        assertEquals(401, r.getCode());
    }

    @Test
    void upload_emptyFile_shouldFail() {
        when(file.isEmpty()).thenReturn(true);
        Result<?> r = fileController.upload(file, request);
        assertFalse(r.isSuccess());
    }

    @Test
    void upload_invalidExt_shouldFail() {
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("evil.txt");
        Result<?> r = fileController.upload(file, request);
        assertFalse(r.isSuccess());
    }

    @Test
    void upload_tooLarge_shouldFail() {
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("big.png");
        when(file.getSize()).thenReturn(6L * 1024 * 1024);
        Result<?> r = fileController.upload(file, request);
        assertFalse(r.isSuccess());
    }

    @Test
    void upload_magicMismatch_shouldFail() throws Exception {
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("fake.png");
        when(file.getSize()).thenReturn(1024L);
        // 声称 png，但内容实为 GIF 头 → 魔数校验失败
        byte[] gifHeader = "GIF89a".getBytes();
        when(file.getInputStream()).thenAnswer(inv -> new ByteArrayInputStream(gifHeader));
        Result<?> r = fileController.upload(file, request);
        assertFalse(r.isSuccess());
    }

    @Test
    void upload_headerTooShort_shouldFail() throws Exception {
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("short.png");
        when(file.getSize()).thenReturn(1024L);
        byte[] shortHeader = new byte[]{(byte) 0x89, 0x50};
        when(file.getInputStream()).thenAnswer(inv -> new ByteArrayInputStream(shortHeader));
        Result<?> r = fileController.upload(file, request);
        assertFalse(r.isSuccess());
    }

    @Test
    void upload_validPng_shouldSucceed() throws Exception {
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("ok.png");
        when(file.getSize()).thenReturn(1024L);
        byte[] png = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        when(file.getInputStream()).thenAnswer(inv -> new ByteArrayInputStream(png));
        Result<?> r = fileController.upload(file, request);
        assertTrue(r.isSuccess());
        assertNotNull(r.getData());
    }

    // ========== matchesMagic 反射直测 ==========

    private boolean matchesMagic(byte[] header, String ext) throws Exception {
        Method m = FileController.class.getDeclaredMethod("matchesMagic", byte[].class, String.class);
        m.setAccessible(true);
        return (Boolean) m.invoke(fileController, header, ext);
    }

    @Test
    void matchesMagic_nullHeader_shouldReturnFalse() throws Exception {
        assertFalse(matchesMagic(null, "png"));
    }

    @Test
    void matchesMagic_tooShort_shouldReturnFalse() throws Exception {
        assertFalse(matchesMagic(new byte[]{(byte) 0x89, 0x50}, "png"));
    }

    @Test
    void matchesMagic_jpgValid_shouldReturnTrue() throws Exception {
        assertTrue(matchesMagic(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00}, "jpg"));
    }

    @Test
    void matchesMagic_jpgInvalid_shouldReturnFalse() throws Exception {
        assertFalse(matchesMagic(new byte[]{0x00, (byte) 0xD8, (byte) 0xFF, 0x00}, "jpg"));
    }

    @Test
    void matchesMagic_pngValid_shouldReturnTrue() throws Exception {
        assertTrue(matchesMagic(new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}, "png"));
    }

    @Test
    void matchesMagic_pngInvalid_shouldReturnFalse() throws Exception {
        assertFalse(matchesMagic(new byte[]{(byte) 0x89, 0x50, 0x4E, 0x00}, "png"));
    }

    @Test
    void matchesMagic_gifValid_shouldReturnTrue() throws Exception {
        assertTrue(matchesMagic(new byte[]{0x47, 0x49, 0x46, 0x38}, "gif"));
    }

    @Test
    void matchesMagic_gifInvalid_shouldReturnFalse() throws Exception {
        assertFalse(matchesMagic(new byte[]{0x47, 0x49, 0x46, 0x00}, "gif"));
    }

    @Test
    void matchesMagic_webpValid_shouldReturnTrue() throws Exception {
        byte[] webp = new byte[]{0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0, 0x57, 0x45, 0x42, 0x50};
        assertTrue(matchesMagic(webp, "webp"));
    }

    @Test
    void matchesMagic_webpTooShort_shouldReturnFalse() throws Exception {
        assertFalse(matchesMagic(new byte[]{0x52, 0x49, 0x46, 0x46}, "webp"));
    }

    @Test
    void matchesMagic_webpInvalid_shouldReturnFalse() throws Exception {
        byte[] webp = new byte[]{0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0, 0, 0, 0, 0};
        assertFalse(matchesMagic(webp, "webp"));
    }

    @Test
    void matchesMagic_unknownExt_shouldReturnFalse() throws Exception {
        assertFalse(matchesMagic(new byte[]{0x00, 0x01, 0x02, 0x03}, "bmp"));
    }

    // 逐位失败用例：覆盖各 && 短路的"中间条件为假"分支
    @Test
    void matchesMagic_pngFailPos0_shouldReturnFalse() throws Exception {
        assertFalse(matchesMagic(new byte[]{0x00, 0x50, 0x4E, 0x47}, "png"));
    }

    @Test
    void matchesMagic_pngFailPos1_shouldReturnFalse() throws Exception {
        assertFalse(matchesMagic(new byte[]{(byte) 0x89, 0x00, 0x4E, 0x47}, "png"));
    }

    @Test
    void matchesMagic_pngFailPos2_shouldReturnFalse() throws Exception {
        assertFalse(matchesMagic(new byte[]{(byte) 0x89, 0x50, 0x00, 0x47}, "png"));
    }

    @Test
    void matchesMagic_gifFailPos0_shouldReturnFalse() throws Exception {
        assertFalse(matchesMagic(new byte[]{0x00, 0x49, 0x46, 0x38}, "gif"));
    }

    @Test
    void matchesMagic_gifFailPos1_shouldReturnFalse() throws Exception {
        assertFalse(matchesMagic(new byte[]{0x47, 0x00, 0x46, 0x38}, "gif"));
    }

    @Test
    void matchesMagic_gifFailPos2_shouldReturnFalse() throws Exception {
        assertFalse(matchesMagic(new byte[]{0x47, 0x49, 0x00, 0x38}, "gif"));
    }

    @Test
    void matchesMagic_jpgFailPos1_shouldReturnFalse() throws Exception {
        assertFalse(matchesMagic(new byte[]{(byte) 0xFF, 0x00, (byte) 0xFF, 0x00}, "jpg"));
    }

    @Test
    void matchesMagic_jpgFailPos2_shouldReturnFalse() throws Exception {
        assertFalse(matchesMagic(new byte[]{(byte) 0xFF, (byte) 0xD8, 0x00, 0x00}, "jpg"));
    }

    @Test
    void matchesMagic_webpFailH0_shouldReturnFalse() throws Exception {
        assertFalse(matchesMagic(new byte[]{0x00, 0x49, 0x46, 0x46, 0, 0, 0, 0, 0x57, 0x45, 0x42, 0x50}, "webp"));
    }

    @Test
    void matchesMagic_webpFailH9_shouldReturnFalse() throws Exception {
        assertFalse(matchesMagic(new byte[]{(byte) 0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0, 0x00, 0x45, 0x42, 0x50}, "webp"));
    }

    @Test
    void matchesMagic_webpFailH10_shouldReturnFalse() throws Exception {
        assertFalse(matchesMagic(new byte[]{(byte) 0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0, 0x57, 0x00, 0x42, 0x50}, "webp"));
    }

    @Test
    void matchesMagic_webpFailH11_shouldReturnFalse() throws Exception {
        assertFalse(matchesMagic(new byte[]{(byte) 0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0, 0x57, 0x45, 0x42, 0x00}, "webp"));
    }
}
