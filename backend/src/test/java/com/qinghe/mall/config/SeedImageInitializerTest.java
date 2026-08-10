package com.qinghe.mall.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 种子图片本地化测试（原零覆盖，miss 43 行）。
 *
 * 真实执行：注入临时上传目录，验证 classpath 种子 SVG 实际拷贝到目标目录；
 * 已存在文件不覆盖（保护用户上传）。
 */
class SeedImageInitializerTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("run 真实拷贝种子 SVG 到上传目录")
    void run_copiesSeedImages() throws Exception {
        SeedImageInitializer initializer = new SeedImageInitializer();
        ReflectionTestUtils.setField(initializer, "uploadDir", tempDir.toString());

        initializer.run();

        Path targetDir = tempDir.resolve("products");
        assertTrue(Files.isDirectory(targetDir), "应创建 products 子目录");
        try (var stream = Files.list(targetDir)) {
            long count = stream.count();
            assertTrue(count > 0, "应拷贝至少一张种子占位图，实际 " + count);
        }
    }

    @Test
    @DisplayName("已存在的目标文件不被覆盖（保护用户上传）")
    void run_doesNotOverwriteExisting() throws Exception {
        SeedImageInitializer initializer = new SeedImageInitializer();
        ReflectionTestUtils.setField(initializer, "uploadDir", tempDir.toString());
        // 预创建目录与一个同名文件，内容标记为用户上传
        Path targetDir = tempDir.resolve("products");
        Files.createDirectories(targetDir);
        Path existing = targetDir.resolve("p001-1.svg");
        Files.writeString(existing, "USER_UPLOADED_CONTENT");

        initializer.run();

        String content = Files.readString(existing);
        assertTrue(content.contains("USER_UPLOADED_CONTENT"), "已存在文件必须保留用户内容");
    }
}
