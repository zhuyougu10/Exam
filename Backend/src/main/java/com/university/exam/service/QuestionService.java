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
     * 随机抽取指定数量和类型的题目
     *
     * @param courseId 课程ID
     * @param type     题目类型
     * @param count    数量
     * @return 题目列表
     */
    List<Question> getRandomQuestions(Long courseId, Integer type, Integer count);
}