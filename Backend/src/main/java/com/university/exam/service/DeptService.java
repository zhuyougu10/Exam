package com.university.exam.service;

import com.university.exam.entity.Dept;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 部门/班级表 服务类
 * </p>
 *
 * @author MySQL数据库架构师
 * @since 2025-12-09
 */
public interface DeptService extends IService<Dept> {

    /**
     * 获取部门树形结构
     *
     * @return 部门树列表
     */
    List<Dept> getDeptTree();
}