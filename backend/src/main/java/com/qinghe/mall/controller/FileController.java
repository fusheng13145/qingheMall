package com.qinghe.mall.controller;

import com.qinghe.mall.config.RateLimit;
import com.qinghe.mall.model.Result;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 图片上传（M3-4 商品图片本地化）。
 *
 * 上传文件保存到本地目录（app.upload.dir，默认 ./upload），
 * 经 /uploads/** 静态映射对外访问，返回相对路径 URL（如 /uploads/xxx.jpg）。
 */
@RestController
@RequestMapping("/api/file")
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    private static final List<String> ALLOWED_EXT = Arrays.asList("jpg", "jpeg", "png", "webp", "gif");

    @Value("${app.upload.dir:./upload}")
    private String uploadDir;

    /**
     * 图片上传（登录 + 限流 10/s，P1-4 修复：禁止匿名写盘）。
     * POST /api/file/upload（multipart 字段 file）
     */
    @RateLimit(rate = 10, message = "上传过于频繁，请稍后再试")
    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(@RequestParam("file") MultipartFile file,
                                              javax.servlet.http.HttpServletRequest request) {
        Object userIdObj = request.getSession().getAttribute("userId");
        if (userIdObj == null) {
            return Result.fail(401, "未登录");
        }
        if (file == null || file.isEmpty()) {
            return Result.fail("请选择要上传的文件");
        }
        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = "";
        int dot = originalName.lastIndexOf('.');
        if (dot >= 0) {
            ext = originalName.substring(dot + 1).toLowerCase();
        }
        if (!ALLOWED_EXT.contains(ext)) {
            return Result.fail("仅支持 jpg/png/webp/gif 格式图片");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            return Result.fail("图片大小不能超过 5MB");
        }

        File dir = new File(uploadDir);
        if (!dir.exists() && !dir.mkdirs()) {
            return Result.fail("上传目录创建失败");
        }
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        File dest = new File(dir, fileName);
        // 用流拷贝替代 transferTo：兼容 Windows + Tomcat 下的部分写入失败问题
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("保存上传文件失败: {}", fileName, e);
            return Result.fail("文件保存失败，请稍后重试");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("url", "/uploads/" + fileName);
        result.put("name", StringUtils.isNotBlank(originalName) ? originalName : fileName);
        return Result.success(result);
    }
}
