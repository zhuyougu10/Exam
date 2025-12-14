package com.university.exam.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.university.exam.common.result.Result;
import com.university.exam.common.vo.TeacherDashboardVo;
import com.university.exam.entity.Paper;
import com.university.exam.entity.Publish;
import com.university.exam.entity.Question;
import com.university.exam.entity.Record;
import com.university.exam.service.PaperService;
import com.university.exam.service.PublishService;
import com.university.exam.service.QuestionService;
import com.university.exam.service.RecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teacher/dashboard")
public class TeacherDashboardController {

    @Autowired
    private PaperService paperService;
    @Autowired
    private QuestionService questionService;
    @Autowired
    private RecordService recordService;
    @Autowired
    private PublishService publishService;

    @GetMapping("/stats")
    public Result<TeacherDashboardVo> getDashboardStats() {
        TeacherDashboardVo vo = new TeacherDashboardVo();

        // 1. 基础数据统计
        vo.setPaperCount(paperService.count());
        vo.setQuestionCount(questionService.count());
        // 统计已提交的考试记录作为参考人次
        vo.setExamineeCount(recordService.count(new LambdaQueryWrapper<Record>().ne(Record::getStatus, 0)));

        // 2. 构建待办事项 (模拟智能生成)
        List<TeacherDashboardVo.TodoItem> todoList = new ArrayList<>();

        // 待办1: 查询有多少份试卷含有待批改的记录 (Status=2)
        // 这里简化逻辑，查询待批改记录总数
        long pendingReviewCount = recordService.count(new LambdaQueryWrapper<Record>().eq(Record::getStatus, 2));
        if (pendingReviewCount > 0) {
            TeacherDashboardVo.TodoItem item = new TeacherDashboardVo.TodoItem();
            item.setId(1L);
            item.setTitle("目前有 " + pendingReviewCount + " 份考卷等待阅卷复核");
            item.setTime("实时");
            item.setUrgent(true);
            item.setType("review");
            todoList.add(item);
        }

        // 待办2: 查询最近发布的考试
        List<Publish> recentPublishes = publishService.list(new LambdaQueryWrapper<Publish>()
                .orderByDesc(Publish::getCreateTime)
                .last("LIMIT 1"));
        if (!recentPublishes.isEmpty()) {
            Publish p = recentPublishes.get(0);
            TeacherDashboardVo.TodoItem item = new TeacherDashboardVo.TodoItem();
            item.setId(2L);
            item.setTitle("最新发布的考试《" + p.getTitle() + "》正在进行中");
            item.setTime(p.getCreateTime().format(DateTimeFormatter.ofPattern("MM-dd HH:mm")));
            item.setUrgent(false);
            item.setType("publish");
            todoList.add(item);
        }

        // 待办3: 题库统计提示
        TeacherDashboardVo.TodoItem item3 = new TeacherDashboardVo.TodoItem();
        item3.setId(3L);
        item3.setTitle("当前题库共有 " + vo.getQuestionCount() + " 道题目，建议补充");
        item3.setTime("系统提示");
        item3.setUrgent(false);
        item3.setType("import");
        todoList.add(item3);

        vo.setTodoList(todoList);

        return Result.success(vo);
    }
}