package com.university.exam.service;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.university.exam.common.dto.student.StudentExamDto;
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

    /**
     * 获取学生可参加的考试列表
     *
     * @param userId 学生ID
     * @param deptId 学生所属部门ID
     * @return 考试列表DTO
     */
    List<StudentExamDto> listStudentExams(Long userId, Long deptId);

    @Data
    class PublishRequest {
        private Long paperId;
        private String title;
        private List<Long> targetDeptIds; // 目标班级/部门

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime startTime;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime endTime;

        private Integer limitCount; // 限制次数
        private String password;    // 考试密码(可选)
    }
}