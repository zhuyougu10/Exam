package com.university.exam.codegen;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;

import java.util.Collections;

/**
 * MyBatis-Plus 代码生成器
 * 用于生成后端代码结构，包括Entity、Mapper、Service等
 *
 * @author MySQL数据库架构师
 * @version 1.0.0
 * @since 2025-12-09
 */
public class CodeGenerator {

    /**
     * 代码生成入口方法
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // 数据库连接配置
        String url = "jdbc:mysql://localhost:3306/exam_system?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf-8&allowPublicKeyRetrieval=true";
        String username = "root";
        String password = "root";
        
        // 代码生成配置
        FastAutoGenerator.create(url, username, password)
                // 全局配置
                .globalConfig(builder -> {
                    builder.author("MySQL数据库架构师") // 设置作者
                            .outputDir(System.getProperty("user.dir") + "/src/main/java") // 设置输出目录
                            .commentDate("yyyy-MM-dd") // 设置注释日期格式
                            .disableOpenDir(); // 禁用打开输出目录
                })
                // 包配置
                .packageConfig(builder -> {
                    builder.parent("com.university.exam") // 设置父包名
                            .entity("entity") // 设置实体类包名
                            .mapper("mapper") // 设置Mapper接口包名
                            .service("service") // 设置Service接口包名
                            .serviceImpl("service.impl") // 设置Service实现类包名
                            .controller("controller") // 设置Controller包名
                            .pathInfo(Collections.singletonMap(OutputFile.xml, System.getProperty("user.dir") + "/src/main/resources/mapper")); // 设置Mapper XML文件路径
                })
                // 策略配置
                .strategyConfig(builder -> {
                    builder.addInclude(
                            "sys_user", "sys_dept", "sys_course", "sys_course_user", "sys_config",
                            "sys_knowledge_file", "exam_question", "sys_ai_task",
                            "exam_paper", "exam_paper_question", "exam_publish",
                            "exam_record", "exam_record_detail", "exam_proctor_log",
                            "exam_mistake_book", "sys_notice", "sys_user_notice"
                    ) // 设置需要生成的表名
                    .addTablePrefix("sys_", "exam_") // 设置表前缀，生成的实体类将去掉前缀
                    // 实体类策略配置
                    .entityBuilder()
                    .enableLombok() // 开启Lombok注解
                    .enableTableFieldAnnotation() // 开启表字段注解
                    .logicDeleteColumnName("is_deleted") // 设置逻辑删除字段
                    // Service策略配置
                    .serviceBuilder()
                    .formatServiceFileName("%sService") // Service接口命名格式
                    .formatServiceImplFileName("%sServiceImpl") // Service实现类命名格式
                    // Controller策略配置
                    .controllerBuilder()
                    .enableRestStyle(); // 开启Rest风格
                })
                // 模板引擎配置
                .templateEngine(new FreemarkerTemplateEngine())
                // 执行代码生成
                .execute();
    }
}