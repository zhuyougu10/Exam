package com.university.exam.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.university.exam.common.exception.BizException;
import com.university.exam.common.utils.DifyClient;
import com.university.exam.entity.CourseUser;
import com.university.exam.entity.KnowledgeFile;
import com.university.exam.mapper.KnowledgeFileMapper;
import com.university.exam.service.CourseUserService;
import com.university.exam.service.KnowledgeFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * RAG资料表 服务实现类
 * </p>
 *
 * @author MySQL数据库架构师
 * @since 2025-12-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeFileServiceImpl extends ServiceImpl<KnowledgeFileMapper, KnowledgeFile> implements KnowledgeFileService {

    private final DifyClient difyClient;
    private final CourseUserService courseUserService;

    // 本地文件存储路径
    private static final String UPLOAD_DIR = System.getProperty("user.dir") + File.separator + "uploads" + File.separator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeFile uploadFile(Long courseId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new BizException(400, "文件不能为空");
        }

        Long userId = getCurrentUserId();

        // 1. 获取配置
        String apiKey = difyClient.getKnowledgeKey();
        String datasetId = difyClient.getGlobalDatasetId();

        // 2. 保存文件到本地
        String originalFilename = file.getOriginalFilename();
        String suffix = FileUtil.getSuffix(originalFilename);
        String fileName = IdUtil.simpleUUID() + "." + suffix;
        File dest = new File(UPLOAD_DIR + fileName);

        if (!dest.getParentFile().exists()) {
            dest.getParentFile().mkdirs();
        }

        try {
            file.transferTo(dest);
        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw new BizException(500, "文件保存失败");
        }

        String fileUrl = "/uploads/" + fileName;

        // 3. 调用 Dify API 上传
        String difyDocumentId;
        try {
            FileSystemResource fileResource = new FileSystemResource(dest);
            Map result = difyClient.uploadDocument(apiKey, datasetId, fileResource);

            if (result != null && result.containsKey("document")) {
                Map document = (Map) result.get("document");
                difyDocumentId = (String) document.get("id");
            } else {
                throw new BizException(500, "Dify 响应异常，未获取到文档ID");
            }
        } catch (Exception e) {
            FileUtil.del(dest); // 失败则清理本地文件
            throw e;
        }

        // 4. 保存数据库记录
        KnowledgeFile knowledgeFile = new KnowledgeFile();
        knowledgeFile.setCourseId(courseId);
        knowledgeFile.setFileName(originalFilename);
        knowledgeFile.setFileUrl(fileUrl);
        knowledgeFile.setDifyDocumentId(difyDocumentId);
        knowledgeFile.setStatus((byte) 1); // 1-indexing (索引中)
        knowledgeFile.setCreateBy(userId);
        knowledgeFile.setUpdateBy(userId);
        knowledgeFile.setCreateTime(LocalDateTime.now());
        knowledgeFile.setUpdateTime(LocalDateTime.now());

        this.save(knowledgeFile);

        return knowledgeFile;
    }

    @Override
    public List<KnowledgeFile> getList(Long courseId) {
        Long userId = getCurrentUserId();
        Integer role = getCurrentUserRole();

        List<KnowledgeFile> list;

        // 1. 权限校验与查询
        if (role == 3) { // 管理员
            LambdaQueryWrapper<KnowledgeFile> query = new LambdaQueryWrapper<>();
            if (courseId != null) {
                query.eq(KnowledgeFile::getCourseId, courseId);
            }
            query.orderByDesc(KnowledgeFile::getCreateTime);
            list = this.list(query);
        } else if (role == 2) { // 教师
            if (courseId == null) {
                throw new BizException(400, "教师查询需指定课程ID");
            }
            // 校验教师是否教授该课程
            long count = courseUserService.count(new LambdaQueryWrapper<CourseUser>()
                    .eq(CourseUser::getUserId, userId)
                    .eq(CourseUser::getCourseId, courseId)
                    .eq(CourseUser::getRole, 2));

            if (count == 0) {
                throw new BizException(403, "无权访问该课程的资料");
            }

            list = this.list(new LambdaQueryWrapper<KnowledgeFile>()
                    .eq(KnowledgeFile::getCourseId, courseId)
                    .orderByDesc(KnowledgeFile::getCreateTime));
        } else {
            throw new BizException(403, "当前角色无权访问知识库");
        }

        // 2. 核心修复：同步 Dify 状态
        // 只有当列表中包含"索引中(1)"状态的文件时，才调用 Dify API
        syncDifyStatus(list);

        return list;
    }

    /**
     * 同步 Dify 文档状态
     * 针对处于 status=1 (索引中) 的文件进行状态检查
     */
    private void syncDifyStatus(List<KnowledgeFile> list) {
        if (list == null || list.isEmpty()) return;

        // 筛选出需要同步的文件 (状态为1且有Dify ID)
        List<KnowledgeFile> processingFiles = list.stream()
                .filter(f -> f.getStatus() == 1 && f.getDifyDocumentId() != null)
                .toList();

        if (processingFiles.isEmpty()) return;

        try {
            String apiKey = difyClient.getKnowledgeKey();
            String datasetId = difyClient.getGlobalDatasetId();

            // 获取最新文档列表 (假设最近上传的文件在前20条内)
            Map result = difyClient.getDocumentList(apiKey, datasetId, 1, 50);

            if (result != null && result.containsKey("data")) {
                List<Map> difyDocs = (List<Map>) result.get("data");
                boolean hasUpdates = false;

                for (KnowledgeFile file : processingFiles) {
                    // 在 Dify 返回列表中查找对应 ID
                    difyDocs.stream()
                            .filter(doc -> file.getDifyDocumentId().equals(doc.get("id")))
                            .findFirst()
                            .ifPresent(doc -> {
                                String status = (String) doc.get("indexing_status");
                                // Dify状态: indexing, completed, error, waiting, parsing
                                if ("completed".equals(status)) {
                                    file.setStatus((byte) 2); // 可用
                                    file.setUpdateTime(LocalDateTime.now());
                                } else if ("error".equals(status)) {
                                    file.setStatus((byte) 3); // 失败
                                    file.setUpdateTime(LocalDateTime.now());
                                }
                            });
                }

                // 批量更新数据库 (仅更新状态发生变化的文件)
                List<KnowledgeFile> updatedFiles = processingFiles.stream()
                        .filter(f -> f.getStatus() != 1)
                        .collect(Collectors.toList());

                if (!updatedFiles.isEmpty()) {
                    this.updateBatchById(updatedFiles);
                }
            }
        } catch (Exception e) {
            log.warn("同步 Dify 状态失败，本次忽略: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(Long id) {
        KnowledgeFile file = this.getById(id);
        if (file == null) {
            throw new BizException(404, "文件不存在");
        }

        Long userId = getCurrentUserId();
        Integer role = getCurrentUserRole();

        // 1. 权限校验
        if (role != 3) {
            long count = courseUserService.count(new LambdaQueryWrapper<CourseUser>()
                    .eq(CourseUser::getUserId, userId)
                    .eq(CourseUser::getCourseId, file.getCourseId())
                    .eq(CourseUser::getRole, 2));
            if (count == 0) {
                throw new BizException(403, "无权删除此文件");
            }
        }

        // 2. 调用 Dify 删除
        String apiKey = difyClient.getKnowledgeKey();
        String datasetId = difyClient.getGlobalDatasetId();

        if (file.getDifyDocumentId() != null) {
            try {
                difyClient.deleteDocument(apiKey, datasetId, file.getDifyDocumentId());
            } catch (Exception e) {
                log.warn("Dify文件删除失败，仅删除本地记录: {}", e.getMessage());
            }
        }

        // 3. 数据库逻辑删除
        file.setUpdateBy(userId);
        file.setUpdateTime(LocalDateTime.now());
        this.removeById(id);

        // 4. 删除本地文件
        if (file.getFileUrl() != null && file.getFileUrl().startsWith("/uploads/")) {
            try {
                String localPath = UPLOAD_DIR + file.getFileUrl().substring("/uploads/".length());
                FileUtil.del(localPath);
            } catch (Exception e) {
                log.warn("本地文件删除失败: {}", e.getMessage());
            }
        }
    }

    // 获取当前用户ID
    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    // 获取当前用户角色
    private Integer getCurrentUserRole() {
        return (Integer) SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .findFirst().get().getAuthority().replace("ROLE_", "").transform(Integer::parseInt);
    }
}