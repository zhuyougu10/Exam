package com.university.exam.controller;

import com.university.exam.common.result.Result;
import com.university.exam.entity.KnowledgeFile;
import com.university.exam.service.KnowledgeFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 知识库管理控制器
 *
 * @author MySQL数据库架构师
 * @version 1.0.0
 * @since 2025-12-10
 */
@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole(2, 3)")
public class KnowledgeBaseController {

    private final KnowledgeFileService knowledgeFileService;

    /**
     * 上传文件到知识库
     * POST /api/knowledge/upload
     *
     * @param courseId 课程ID
     * @param file     文件
     * @return 上传结果
     */
    @PostMapping("/upload")
    public Result<?> upload(@RequestParam("courseId") Long courseId,
                            @RequestParam("file") MultipartFile file) {
        KnowledgeFile result = knowledgeFileService.uploadFile(courseId, file);
        return Result.success(result, "文件上传并开始索引");
    }

    /**
     * 获取知识库文件列表
     * GET /api/knowledge/list
     *
     * @param courseId 课程ID (对教师必填，管理员可选)
     * @return 文件列表
     */
    @GetMapping("/list")
    public Result<?> getList(@RequestParam(value = "courseId", required = false) Long courseId) {
        List<KnowledgeFile> list = knowledgeFileService.getList(courseId);
        return Result.success(list);
    }

    /**
     * 删除知识库文件
     * DELETE /api/knowledge/{id}
     *
     * @param id 文件ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        knowledgeFileService.deleteFile(id);
        return Result.success("文件删除成功");
    }
}