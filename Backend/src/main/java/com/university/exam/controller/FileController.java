package com.university.exam.controller;

import com.university.exam.common.exception.BizException;
import com.university.exam.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传控制器
 *
 * @author MySQL数据库架构师
 * @version 1.0.0
 * @since 2025-12-16
 */
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${file.base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * 上传图片
     * POST /api/file/upload/image
     *
     * @param file 图片文件
     * @return 上传结果，包含图片访问URL
     */
    @PostMapping("/upload/image")
    public Result<?> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new BizException(400, "请选择要上传的文件");
        }

        // 验证文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BizException(400, "只能上传图片文件");
        }

        // 验证文件大小（最大5MB）
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new BizException(400, "图片大小不能超过5MB");
        }

        try {
            // 创建上传目录（使用绝对路径，基于用户工作目录）
            Path basePath = Paths.get(System.getProperty("user.dir"), uploadDir, "images");
            if (!Files.exists(basePath)) {
                Files.createDirectories(basePath);
            }
            Path uploadPath = basePath;

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFilename = UUID.randomUUID().toString() + extension;

            // 保存文件
            Path filePath = uploadPath.resolve(newFilename);
            file.transferTo(filePath.toFile());

            // 返回访问URL
            String imageUrl = baseUrl + "/uploads/images/" + newFilename;
            return Result.success(Map.of("url", imageUrl), "上传成功");

        } catch (IOException e) {
            throw new BizException(500, "文件上传失败：" + e.getMessage());
        }
    }
}
