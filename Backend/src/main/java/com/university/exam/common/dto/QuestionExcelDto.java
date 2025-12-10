package com.university.exam.common.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 题目导入 Excel DTO
 *
 * @author MySQL数据库架构师
 * @version 1.0.0
 * @since 2025-12-10
 */
@Data
public class QuestionExcelDto {

    @ExcelProperty("题干内容")
    private String content;

    @ExcelProperty("题目类型(1-单选,2-多选,3-判断,4-简答)")
    private Integer type;

    @ExcelProperty("难度(1-简单,2-中等,3-困难)")
    private Integer difficulty;

    @ExcelProperty("选项(JSON格式)")
    private String options;

    @ExcelProperty("答案")
    private String answer;

    @ExcelProperty("解析")
    private String analysis;

    @ExcelProperty("知识点/标签(JSON格式)")
    private String tags;
}