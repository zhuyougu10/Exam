package com.university.exam.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.university.exam.common.dto.review.ReviewListResp;
import com.university.exam.common.dto.review.ReviewSubmitReq;
import com.university.exam.common.exception.BizException;
import com.university.exam.common.result.Result;
import com.university.exam.entity.*;
import com.university.exam.entity.Record;
import com.university.exam.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 阅卷/复核管理控制器
 *
 * @author MySQL数据库架构师
 * @version 1.0.0
 * @since 2025-12-14
 */
@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole(2, 3)") // 仅教师和管理员
public class ReviewController {

    private final RecordService recordService;
    private final RecordDetailService recordDetailService;
    private final QuestionService questionService;
    private final UserService userService;
    private final PaperService paperService;
    private final CourseUserService courseUserService;
    private final DeptService deptService;

    /**
     * 获取待批改/已批改列表
     * GET /api/review/list
     */
    @GetMapping("/list")
    public Result<?> getList(@RequestParam(defaultValue = "1") Integer page,
                             @RequestParam(defaultValue = "10") Integer size,
                             @RequestParam(required = false) Long courseId,
                             @RequestParam(required = false) String studentName,
                             @RequestParam(required = false) Integer status) { // 2-待批改, 3-已完成
        Long currentUserId = getCurrentUserId();
        Integer role = getCurrentUserRole();

        // 1. 获取该教师负责的课程 ID 列表 (管理员忽略)
        List<Long> allowedCourseIds = null;
        if (role == 2) {
            allowedCourseIds = courseUserService.list(new LambdaQueryWrapper<CourseUser>()
                            .eq(CourseUser::getUserId, currentUserId)
                            .eq(CourseUser::getRole, 2))
                    .stream().map(CourseUser::getCourseId).collect(Collectors.toList());
            
            if (allowedCourseIds.isEmpty()) {
                return Result.success(new Page<ReviewListResp>());
            }
        }

        // 2. 联表逻辑较复杂，这里采用分步查询优化 (先查 Paper，再查 Record)
        // 查找符合条件的试卷 ID
        LambdaQueryWrapper<Paper> paperQuery = new LambdaQueryWrapper<>();
        if (courseId != null) {
            paperQuery.eq(Paper::getCourseId, courseId);
        } else if (role == 2) {
            paperQuery.in(Paper::getCourseId, allowedCourseIds);
        }
        List<Paper> papers = paperService.list(paperQuery);
        if (papers.isEmpty()) {
            return Result.success(new Page<ReviewListResp>());
        }
        List<Long> paperIds = papers.stream().map(Paper::getId).collect(Collectors.toList());
        Map<Long, Paper> paperMap = papers.stream().collect(Collectors.toMap(Paper::getId, Function.identity()));

        // 3. 查询考试记录
        LambdaQueryWrapper<Record> recordQuery = new LambdaQueryWrapper<>();
        recordQuery.in(Record::getPaperId, paperIds);
        // 只查询已交卷(2)或已批改(3)
        if (status != null) {
            recordQuery.eq(Record::getStatus, status);
        } else {
            recordQuery.in(Record::getStatus, 2, 3);
        }
        
        // 姓名搜索需要先查用户ID (性能优化点: 大数据量应走ES，这里数据库量小直接查)
        if (StringUtils.hasText(studentName)) {
            List<Long> userIds = userService.list(new LambdaQueryWrapper<User>()
                    .like(User::getRealName, studentName))
                    .stream().map(User::getId).collect(Collectors.toList());
            if (userIds.isEmpty()) {
                return Result.success(new Page<ReviewListResp>());
            }
            recordQuery.in(Record::getUserId, userIds);
        }

        recordQuery.orderByDesc(Record::getSubmitTime);

        IPage<Record> recordPage = recordService.page(new Page<>(page, size), recordQuery);

        // 4. 组装 VO
        List<ReviewListResp> voList = new ArrayList<>();
        if (!recordPage.getRecords().isEmpty()) {
            List<Long> userIds = recordPage.getRecords().stream().map(Record::getUserId).distinct().toList();
            Map<Long, User> userMap = userService.listByIds(userIds).stream()
                    .collect(Collectors.toMap(User::getId, Function.identity()));
            
            // 部门信息
            List<Long> deptIds = userMap.values().stream().map(User::getDeptId).distinct().toList();
            Map<Long, Dept> deptMap = new HashMap<>();
            if (!deptIds.isEmpty()) {
                deptMap = deptService.listByIds(deptIds).stream().collect(Collectors.toMap(Dept::getId, Function.identity()));
            }

            for (Record record : recordPage.getRecords()) {
                ReviewListResp vo = new ReviewListResp();
                BeanUtil.copyProperties(record, vo);
                
                Paper p = paperMap.get(record.getPaperId());
                if (p != null) vo.setPaperTitle(p.getTitle());

                User u = userMap.get(record.getUserId());
                if (u != null) {
                    vo.setStudentName(u.getRealName());
                    Dept d = deptMap.get(u.getDeptId());
                    vo.setDeptName(d != null ? d.getDeptName() : "-");
                }

                // 状态描述
                vo.setReviewStatusDesc(record.getStatus() == 3 ? "已完成" : "待复核");
                voList.add(vo);
            }
        }

        // 构造返回分页对象
        Page<ReviewListResp> resultPage = new Page<>(page, size);
        resultPage.setTotal(recordPage.getTotal());
        resultPage.setRecords(voList);

        return Result.success(resultPage);
    }

    /**
     * 获取阅卷详情 (含题目、学生答案、AI评分)
     * GET /api/review/detail/{recordId}
     */
    @GetMapping("/detail/{recordId}")
    public Result<?> getReviewDetail(@PathVariable Long recordId) {
        Record record = recordService.getById(recordId);
        if (record == null) throw new BizException(404, "记录不存在");

        // 权限检查
        checkPermission(record);

        // 获取明细
        List<RecordDetail> details = recordDetailService.list(new LambdaQueryWrapper<RecordDetail>()
                .eq(RecordDetail::getRecordId, recordId));
        
        // 获取题目信息
        List<Long> qIds = details.stream().map(RecordDetail::getQuestionId).collect(Collectors.toList());
        Map<Long, Question> qMap = questionService.listByIds(qIds).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));

        // 组装详情
        List<Map<String, Object>> questionList = new ArrayList<>();
        for (RecordDetail detail : details) {
            Question q = qMap.get(detail.getQuestionId());
            if (q == null) continue;

            Map<String, Object> item = new HashMap<>();
            item.put("detailId", detail.getId()); // 用于提交修改
            item.put("questionId", q.getId());
            item.put("type", q.getType());
            item.put("content", q.getContent());
            item.put("options", q.getOptions());
            item.put("standardAnswer", q.getAnswer());
            item.put("analysis", q.getAnalysis());
            item.put("maxScore", detail.getMaxScore());
            
            item.put("studentAnswer", detail.getStudentAnswer());
            item.put("score", detail.getScore());
            item.put("aiComment", detail.getAiComment());
            item.put("isMarked", detail.getIsMarked());

            questionList.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("record", record);
        result.put("questions", questionList);

        return Result.success(result);
    }

    /**
     * 提交人工复核结果 (修改分数)
     * POST /api/review/submit
     */
    @PostMapping("/submit")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> submitReview(@Valid @RequestBody ReviewSubmitReq req) {
        Record record = recordService.getById(req.getRecordId());
        if (record == null) throw new BizException(404, "记录不存在");
        
        checkPermission(record);

        RecordDetail detail = recordDetailService.getOne(new LambdaQueryWrapper<RecordDetail>()
                .eq(RecordDetail::getRecordId, req.getRecordId())
                .eq(RecordDetail::getQuestionId, req.getQuestionId()));
        
        if (detail == null) throw new BizException(404, "题目记录不存在");

        // 校验分数
        if (req.getScore().compareTo(detail.getMaxScore()) > 0) {
            throw new BizException(400, "打分不能超过该题满分: " + detail.getMaxScore());
        }

        // 更新明细
        detail.setScore(req.getScore());
        if (StringUtils.hasText(req.getComment())) {
            detail.setAiComment(req.getComment()); // 复用字段存人工评语，或前缀区分
        }
        detail.setIsMarked((byte) 1);
        detail.setIsCorrect(req.getScore().compareTo(detail.getMaxScore()) == 0 ? (byte)1 : (byte)0);
        detail.setUpdateBy(getCurrentUserId());
        detail.setUpdateTime(LocalDateTime.now());
        recordDetailService.updateById(detail);

        // 重新计算总分
        List<RecordDetail> allDetails = recordDetailService.list(new LambdaQueryWrapper<RecordDetail>()
                .eq(RecordDetail::getRecordId, req.getRecordId()));
        
        BigDecimal newTotal = allDetails.stream()
                .map(RecordDetail::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        record.setTotalScore(newTotal);
        
        // 检查是否全部批改完
        boolean allMarked = allDetails.stream().allMatch(d -> d.getIsMarked() == 1);
        if (allMarked) {
            record.setStatus((byte) 3); // 已完成
        }
        
        recordService.updateById(record);

        return Result.success(newTotal, "分数已更新");
    }

    private void checkPermission(Record record) {
        Long userId = getCurrentUserId();
        Integer role = getCurrentUserRole();
        if (role == 3) return; // 管理员通行

        Paper paper = paperService.getById(record.getPaperId());
        long count = courseUserService.count(new LambdaQueryWrapper<CourseUser>()
                .eq(CourseUser::getUserId, userId)
                .eq(CourseUser::getCourseId, paper.getCourseId())
                .eq(CourseUser::getRole, 2));
        
        if (count == 0 && !paper.getCreateBy().equals(userId)) {
            throw new BizException(403, "无权操作非本人负责课程的试卷");
        }
    }

    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private Integer getCurrentUserRole() {
        return (Integer) SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .findFirst().get().getAuthority().replace("ROLE_", "").transform(Integer::parseInt);
    }
}