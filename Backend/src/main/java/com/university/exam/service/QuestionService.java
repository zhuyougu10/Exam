package com.university.exam.service;

import com.university.exam.entity.Question;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * <p>
 * 题库表 服务类
 * </p>
 *
 * @author MySQL数据库架构师
 * @since 2025-12-09
 */
public interface QuestionService extends IService<Question> {

    /**
     * 批量导入题目
     *
     * @param courseId 课程ID
     * @param file     Excel文件
     * @param userId   操作人ID
     */
    void importQuestions(Long courseId, MultipartFile file, Long userId);

    /**
     * 检查题目是否重复
     *
     * @param content 题目内容
     * @return true-重复, false-不重复
     */
    boolean checkDuplicate(String content);
    
    /**
     * 根据权限获取题目列表查询条件
     * @param userId 用户ID
     * @param role 用户角色
     * @param courseId 课程ID (可选筛选)
     * @return 题目列表
     */
    // 注意：如果是分页查询，通常在Controller层组装Wrapper，
    // 但为了权限封装，可以在这里提供一个辅助方法或直接在Controller处理。
    // 这里保持简单，逻辑主要写在Controller或通过Wrapper传递。
}