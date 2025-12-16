package com.university.exam.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 用户导入Excel数据传输对象
 */
@Data
public class UserImportDTO {

    @ExcelProperty("用户名")
    private String username;

    @ExcelProperty("真实姓名")
    private String realName;

    @ExcelProperty("密码")
    private String password;

    @ExcelProperty("角色")
    private String roleName;

    @ExcelProperty("部门/班级")
    private String deptName;

    @ExcelProperty("手机号")
    private String phone;

    @ExcelProperty("邮箱")
    private String email;
}
