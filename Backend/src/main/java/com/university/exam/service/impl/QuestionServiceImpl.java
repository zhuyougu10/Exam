package com.university.exam.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.university.exam.common.dto.QuestionExcelDto;
import com.university.exam.common.exception.BizException;
import com.university.exam.common.utils.QuestionExcelListener;
import com.university.exam.entity.Question;
import com.university.exam.mapper.QuestionMapper;
import com.university.exam.service.QuestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * <p>
 * 题库表 服务实现类
 * </p>
 *
 * @author MySQL数据库架构师
 * @since 2025-12-09
 */
@Slf4j
@Service
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements QuestionService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importQuestions(Long courseId, MultipartFile file, Long userId) {
        if (file.isEmpty()) {
            throw new BizException(400, "文件不能为空");
        }
        try {
            EasyExcel.read(file.getInputStream(), QuestionExcelDto.class, 
                    new QuestionExcelListener(this, courseId, userId)).sheet().doRead();
        } catch (IOException e) {
            log.error("Excel导入失败", e);
            throw new BizException(500, "Excel导入失败: " + e.getMessage());
        }
    }

    @Override
    public boolean checkDuplicate(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }
        String hash = DigestUtil.md5Hex(content.trim());
        long count = this.count(new LambdaQueryWrapper<Question>()
                .eq(Question::getContentHash, hash));
        return count > 0;
    }

    @Override
    public List<Question> getRandomQuestions(Long courseId, Integer type, Integer count) {
        // 使用 MySQL 的 ORDER BY RAND() 进行随机抽取
        // 注意：在大数据量下可能有性能问题，但在校级考试系统规模下通常可以接受
        return this.list(new QueryWrapper<Question>()
                .eq("course_id", courseId)
                .eq("type", type)
                .last("ORDER BY RAND() LIMIT " + count));
    }
}