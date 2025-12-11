package com.university.exam.service;

import com.university.exam.entity.Publish;
import com.baomidou.mybatisplus.extension.service.IService;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 考试发布/场次表 服务类
 * </p>
 *
 * @author MySQL数据库架构师
 * @since 2025-12-09
 */
public interface PublishService extends IService<Publish> {

    /**
     * 发布考试
     *
     * @param request 发布请求
     * @param userId  操作人
     * @return 发布记录ID
     */
    Long publishExam(PublishRequest request, Long userId);

    /**
     * 检查试卷是否已发布
     */
    boolean checkIsPublished(Long paperId);

    @Data
    class PublishRequest {
        private Long paperId;
        private String title;
        private List<Long> targetDeptIds; // 目标班级/部门
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Integer limitCount; // 限制次数
        private String password;    // 考试密码(可选)
    }
}