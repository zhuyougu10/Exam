package com.university.exam.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.university.exam.common.result.Result;
import com.university.exam.entity.MistakeBook;
import com.university.exam.entity.Question;
import com.university.exam.service.MistakeBookService;
import com.university.exam.service.QuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/mistake-book")
@RequiredArgsConstructor
public class MistakeBookController {

    private final MistakeBookService mistakeBookService;
    private final QuestionService questionService;

    /**
     * 获取当前用户ID (替代 JwtUtils.getUserId)
     */
    private Long getCurrentUserId() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            return Long.valueOf(principal.toString());
        } catch (Exception e) {
            log.error("获取当前用户ID失败", e);
            return 0L;
        }
    }

    /**
     * 分页查询错题本列表，并附带题目详细信息
     */
    @GetMapping("/list")
    public Result<IPage<Map<String, Object>>> getMistakeList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long questionType) {
        
        Long userId = getCurrentUserId();

        // 1. 构建分页与查询条件
        Page<MistakeBook> page = new Page<>(current, size);
        QueryWrapper<MistakeBook> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.orderByDesc("create_time");

        // 2. 查询错题记录
        IPage<MistakeBook> mistakePage = mistakeBookService.page(page, queryWrapper);
        List<MistakeBook> records = mistakePage.getRecords();

        if (records.isEmpty()) {
            IPage<Map<String, Object>> emptyPage = new Page<>(current, size, 0);
            emptyPage.setRecords(Collections.emptyList());
            return Result.success(emptyPage);
        }

        // 3. 提取题目ID并批量查询题目详情
        Set<Long> questionIds = records.stream()
                .map(MistakeBook::getQuestionId)
                .collect(Collectors.toSet());
        
        List<Question> questions = questionService.listByIds(questionIds);
        Map<Long, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        // 4. 组装数据，将错题记录与题目详情合并
        List<Map<String, Object>> voList = new ArrayList<>();
        
        for (MistakeBook record : records) {
            Question q = questionMap.get(record.getQuestionId());
            
            // 如果传入了类型过滤，且题目类型不匹配，则跳过
            if (questionType != null && q != null && !Objects.equals(q.getType(), questionType.intValue())) {
                continue;
            }

            Map<String, Object> vo = new HashMap<>();
            vo.put("id", record.getId()); // 错题本ID
            vo.put("questionId", record.getQuestionId());
            vo.put("createTime", record.getCreateTime());
            
            if (q != null) {
                vo.put("questionContent", q.getContent());
                vo.put("questionType", q.getType());
                vo.put("options", q.getOptions());
                vo.put("analysis", q.getAnalysis()); 
                vo.put("difficulty", q.getDifficulty());
                vo.put("correctAnswer", q.getAnswer());
            }
            voList.add(vo);
        }

        IPage<Map<String, Object>> resultPage = new Page<>(current, size, mistakePage.getTotal());
        resultPage.setRecords(voList);

        return Result.success(resultPage);
    }

    /**
     * 移除错题
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> removeMistake(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        
        // 校验该错题是否属于当前用户
        MistakeBook mistake = mistakeBookService.getById(id);
        if (mistake == null) {
            // 修正: 补充错误码 500
            return Result.error(500, "记录不存在");
        }
        if (!mistake.getUserId().equals(userId)) {
            return Result.error(500, "无权操作");
        }

        boolean success = mistakeBookService.removeById(id);
        return Result.success(success);
    }
}