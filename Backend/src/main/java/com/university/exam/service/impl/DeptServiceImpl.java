package com.university.exam.service.impl;

import com.university.exam.entity.Dept;
import com.university.exam.mapper.DeptMapper;
import com.university.exam.service.DeptService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 部门/班级表 服务实现类
 * </p>
 *
 * @author MySQL数据库架构师
 * @since 2025-12-09
 */
@Service
public class DeptServiceImpl extends ServiceImpl<DeptMapper, Dept> implements DeptService {

    /**
     * 获取部门树形结构
     *
     * @return 部门树列表
     */
    @Override
    public List<Dept> getDeptTree() {
        // 1. 查询所有未删除的部门，按排序字段排序
        List<Dept> allDepts = this.list(new LambdaQueryWrapper<Dept>()
                .orderByAsc(Dept::getSortOrder));

        // 2. 使用流处理构建树形结构
        // 先找到所有根节点（ParentId为0）
        List<Dept> roots = allDepts.stream()
                .filter(dept -> dept.getParentId() == 0)
                .collect(Collectors.toList());

        // 3. 递归查找子节点
        for (Dept root : roots) {
            setChildren(root, allDepts);
        }

        return roots;
    }

    /**
     * 递归设置子节点
     *
     * @param parent 父节点
     * @param allDepts 所有节点列表
     */
    private void setChildren(Dept parent, List<Dept> allDepts) {
        List<Dept> children = allDepts.stream()
                .filter(dept -> dept.getParentId().equals(parent.getId()))
                .collect(Collectors.toList());

        if (!children.isEmpty()) {
            parent.setChildren(children);
            // 继续递归
            for (Dept child : children) {
                setChildren(child, allDepts);
            }
        } else {
            parent.setChildren(new ArrayList<>());
        }
    }
}