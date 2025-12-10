package com.university.exam.service;

import com.university.exam.entity.KnowledgeFile;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * <p>
 * RAG资料表 服务类
 * </p>
 *
 * @author MySQL数据库架构师
 * @since 2025-12-09
 */
public interface KnowledgeFileService extends IService<KnowledgeFile> {

    /**
     * 上传知识库文件
     *
     * @param courseId 课程ID
     * @param file     文件
     * @return 保存的文件信息
     */
    KnowledgeFile uploadFile(Long courseId, MultipartFile file);

    /**
     * 获取知识库文件列表（带权限控制）
     *
     * @param courseId 课程ID
     * @return 文件列表
     */
    List<KnowledgeFile> getList(Long courseId);

    /**
     * 删除知识库文件
     *
     * @param id 文件ID
     */
    void deleteFile(Long id);
}