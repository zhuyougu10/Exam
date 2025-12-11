package com.university.exam.common.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

/**
 * 题目导入 Excel DTO
 *
 * @author MySQL数据库架构师
 * @version 1.6.0 (增加填空题题干说明)
 * @since 2025-12-10
 */
@Data
public class QuestionExcelDto {

    @ColumnWidth(50) // 设置列宽，方便阅读
    @ExcelProperty("题干内容 (必填，填空题请用____表示空)")
    private String content;

    @ColumnWidth(20)
    @ExcelProperty("题目类型 (1-单选,2-多选,3-判断,4-简答,5-填空)")
    private Integer type;

    @ColumnWidth(15)
    @ExcelProperty("难度 (1-简单,2-中等,3-困难)")
    private Integer difficulty;

    @ColumnWidth(40)
    @ExcelProperty("选项 (单/多选用竖线 | 分隔，如: A选项 | B选项)")
    private String options;

    @ColumnWidth(30)
    @ExcelProperty("答案 (单/多选填字母; 判断填正确/错误; 填空题多空用 | 分隔)")
    private String answer;

    @ColumnWidth(40)
    @ExcelProperty("解析 (选填)")
    private String analysis;

    @ColumnWidth(25)
    @ExcelProperty("知识点/标签 (用逗号分隔，如: 第一章,重点)")
    private String tags;
}