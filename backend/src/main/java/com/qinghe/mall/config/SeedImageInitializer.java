package com.qinghe.mall.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

/**
 * 种子图片本地化（RR-5）：应用启动时，将 classpath 下的占位商品图
 * （seed-images/products/*.svg）拷贝到上传目录 <uploadDir>/products/ 下，
 * 仅当目标文件不存在时写入，避免覆盖用户上传。
 *
 * 这样 product_imgs 中指向 /uploads/products/... 的本地路径即可在
 * 开发、Docker 与生产环境一致渲染，无需依赖任何外部图像服务。
 */
@Component
public class SeedImageInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedImageInitializer.class);

    private static final String SEED_PATTERN = "classpath*:seed-images/products/*.svg";

    @Value("${app.upload.dir:./upload}")
    private String uploadDir;

    @Override
    public void run(String... args) {
        Path targetDir = Paths.get(new File(uploadDir, "products").getAbsolutePath());
        try {
            Files.createDirectories(targetDir);
        } catch (IOException e) {
            log.warn("种子图片目录创建失败，跳过本地化拷贝: {}", targetDir, e);
            return;
        }

        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources;
        try {
            resources = resolver.getResources(SEED_PATTERN);
        } catch (IOException e) {
            log.warn("未找到种子占位图资源: {}", SEED_PATTERN, e);
            return;
        }
        if (resources.length == 0) {
            return;
        }

        int copied = 0;
        for (Resource res : resources) {
            String filename = res.getFilename();
            if (filename == null) {
                continue;
            }
            Path dest = targetDir.resolve(filename);
            if (Files.exists(dest)) {
                continue; // 已存在则保留（可能是用户真实图片）
            }
            try (InputStream in = res.getInputStream()) {
                Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
                copied++;
            } catch (IOException e) {
                log.warn("种子图片拷贝失败: {}", filename, e);
            }
        }
        if (copied > 0) {
            log.info("已本地化 {} 张种子占位图 -> {}", copied, targetDir);
        }
    }
}
