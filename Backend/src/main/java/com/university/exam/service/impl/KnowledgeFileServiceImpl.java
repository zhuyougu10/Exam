package com.university.exam.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.university.exam.common.exception.BizException;
import com.university.exam.common.utils.DifyClient;
import com.university.exam.entity.CourseUser;
import com.university.exam.entity.KnowledgeFile;
import com.university.exam.mapper.KnowledgeFileMapper;
import com.university.exam.service.CourseUserService;
import com.university.exam.service.KnowledgeFileService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    // 本地文件存储路径 (实际生产环境建议配置在 application.yml 或使用 OSS)
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

        // 2. 保存文件到本地 (模拟 OSS)
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

        String fileUrl = "/uploads/" + fileName; // 模拟访问路径

        // 3. 调用 Dify API 上传
        String difyDocumentId;
        try {
            Map result = difyClient.uploadDocument(apiKey, datasetId, file);
            // 解析返回结果，获取 document.id
            // Dify 响应结构: { "document": { "id": "...", ... }, ... }
            if (result != null && result.containsKey("document")) {
                Map document = (Map) result.get("document");
                difyDocumentId = (String) document.get("id");
            } else {
                throw new BizException(500, "Dify 响应异常，未获取到文档ID");
            }
        } catch (Exception e) {
            // 如果 Dify 上传失败，删除本地文件（可选）
            FileUtil.del(dest);
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

        // 1. 权限校验
        if (role == 3) { // 管理员
            // 管理员可以查看所有，如果指定了 courseId 则筛选
            LambdaQueryWrapper<KnowledgeFile> query = new LambdaQueryWrapper<>();
            if (courseId != null) {
                query.eq(KnowledgeFile::getCourseId, courseId);
            }
            query.orderByDesc(KnowledgeFile::getCreateTime);
            return this.list(query);
        } else if (role == 2) { // 教师
            if (courseId == null) {
                throw new BizException(400, "教师查询需指定课程ID");
            }
            // 校验教师是否教授该课程
            long count = courseUserService.count(new LambdaQueryWrapper<CourseUser>()
                    .eq(CourseUser::getUserId, userId)
                    .eq(CourseUser::getCourseId, courseId)
                    .eq(CourseUser::getRole, 2)); // 角色2为教师关联

            if (count == 0) {
                throw new BizException(403, "无权访问该课程的资料");
            }

            return this.list(new LambdaQueryWrapper<KnowledgeFile>()
                    .eq(KnowledgeFile::getCourseId, courseId)
                    .orderByDesc(KnowledgeFile::getCreateTime));
        } else {
            // 学生或其他角色暂未开放
            throw new BizException(403, "当前角色无权访问知识库");
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

        // 1. 权限校验 (管理员可删任意，教师只能删自己课程的)
        if (role != 3) {
            // 教师：检查是否是该课程的授课教师
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

        // 只有当存在 Dify ID 时才调用远程删除
        if (file.getDifyDocumentId() != null) {
            try {
                difyClient.deleteDocument(apiKey, datasetId, file.getDifyDocumentId());
            } catch (Exception e) {
                log.warn("Dify文件删除失败，仅删除本地记录: {}", e.getMessage());
                // 这里选择吞掉异常，确保本地数据能被删除，防止数据不一致死锁
            }
        }

        // 3. 数据库逻辑删除
        file.setUpdateBy(userId);
        file.setUpdateTime(LocalDateTime.now());
        this.removeById(id); // MyBatis-Plus 默认逻辑删除

        // 4. 删除本地文件 (可选)
        if (file.getFileUrl() != null && file.getFileUrl().startsWith("/uploads/")) {
            String localPath = UPLOAD_DIR + file.getFileUrl().substring("/uploads/".length());
            FileUtil.del(localPath);
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