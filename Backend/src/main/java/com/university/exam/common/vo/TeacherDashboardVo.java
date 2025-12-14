package com.university.exam.common.vo;

import lombok.Data;
import java.util.List;

@Data
public class TeacherDashboardVo {
    // 概览数据
    private Long paperCount;      // 试卷总数
    private Long questionCount;   // 题目总数
    private Long examineeCount;   // 累计参考人次

    // 待办/通知列表
    private List<TodoItem> todoList;

    @Data
    public static class TodoItem {
        private Long id;
        private String title;
        private String time;
        private Boolean urgent; // 是否紧急
        private String type;    // 任务类型：review(阅卷), publish(发布), import(导入)
    }
}