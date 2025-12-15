package com.university.exam.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.university.exam.common.dto.review.ReviewListResp;
import com.university.exam.common.dto.review.ReviewPaperResp;
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
 * @version 1.2.0
 * @since 2025-12-14
 */
@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('2', '3')") // 仅教师和管理员
public class ReviewController {

    private final RecordService recordService;
    private final RecordDetailService recordDetailService;
    private final QuestionService questionService;
    private final UserService userService;
    private final PaperService paperService;
    private final CourseUserService courseUserService;
    private final DeptService deptService;
    private final CourseService courseService;

    /**
     * 获取待阅试卷列表 (含统计信息)
     * GET /api/review/papers
     */
    @GetMapping("/papers")
    public Result<?> getPaperList() {
        Long currentUserId = getCurrentUserId();
        Integer role = getCurrentUserRole();

        // 1. 获取教师负责的课程
        List<Long> courseIds = new ArrayList<>();
        if (role == 2) {
            courseIds = courseUserService.list(new LambdaQueryWrapper<CourseUser>()
                            .eq(CourseUser::getUserId, currentUserId)
                            .eq(CourseUser::getRole, 2))
                    .stream().map(CourseUser::getCourseId).collect(Collectors.toList());
            
            if (courseIds.isEmpty()) {
                return Result.success(new ArrayList<>());
            }
        }

        // 2. 查询相关试卷
        LambdaQueryWrapper<Paper> paperQuery = new LambdaQueryWrapper<>();
        if (!courseIds.isEmpty()) {
            paperQuery.in(Paper::getCourseId, courseIds);
        }
        paperQuery.orderByDesc(Paper::getCreateTime);
        List<Paper> papers = paperService.list(paperQuery);

        if (papers.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        // 3. 准备统计数据 (查询所有相关记录进行内存聚合，避免N+1查询)
        List<Long> paperIds = papers.stream().map(Paper::getId).collect(Collectors.toList());
        List<Record> records = recordService.list(new LambdaQueryWrapper<Record>()
                .in(Record::getPaperId, paperIds)
                .in(Record::getStatus, 2, 3)); // 只关注已交卷(2)和已完成(3)

        // 分组统计：Map<PaperId, Map<Status, Count>>
        Map<Long, Map<Byte, Long>> statsMap = records.stream()
                .collect(Collectors.groupingBy(
                        Record::getPaperId,
                        Collectors.groupingBy(Record::getStatus, Collectors.counting())
                ));

        // 缓存课程名称
        List<Long> allCourseIds = papers.stream().map(Paper::getCourseId).distinct().toList();
        Map<Long, String> courseNameMap = courseService.listByIds(allCourseIds).stream()
                .collect(Collectors.toMap(Course::getId, Course::getCourseName));

        // 4. 组装结果
        List<ReviewPaperResp> result = new ArrayList<>();
        for (Paper paper : papers) {
            Map<Byte, Long> paperStats = statsMap.getOrDefault(paper.getId(), new HashMap<>());
            long pending = paperStats.getOrDefault((byte) 2, 0L);
            long reviewed = paperStats.getOrDefault((byte) 3, 0L);

            ReviewPaperResp vo = new ReviewPaperResp();
            vo.setId(paper.getId());
            vo.setTitle(paper.getTitle());
            vo.setCourseId(paper.getCourseId());
            vo.setCourseName(courseNameMap.getOrDefault(paper.getCourseId(), "未知课程"));
            vo.setStatus(paper.getStatus().intValue());
            
            vo.setPendingCount(pending);
            vo.setReviewedCount(reviewed);
            vo.setTotalCount(pending + reviewed);

            result.add(vo);
        }

        return Result.success(result);
    }

    /**
     * 获取考生列表
     * GET /api/review/list
     */
    @GetMapping("/list")
    public Result<?> getList(@RequestParam(defaultValue = "1") Integer page,
                             @RequestParam(defaultValue = "10") Integer size,
                             @RequestParam(required = false) Long paperId,
                             @RequestParam(required = false) Long courseId,
                             @RequestParam(required = false) String studentName,
                             @RequestParam(required = false) Integer status) {
        
        // 构建查询条件
        LambdaQueryWrapper<Record> recordQuery = new LambdaQueryWrapper<>();
        
        // 1. 试卷/课程筛选
        if (paperId != null) {
            recordQuery.eq(Record::getPaperId, paperId);
        } else if (courseId != null) {
            List<Paper> coursePapers = paperService.list(new LambdaQueryWrapper<Paper>().eq(Paper::getCourseId, courseId));
            if (coursePapers.isEmpty()) return Result.success(new Page<ReviewListResp>());
            recordQuery.in(Record::getPaperId, coursePapers.stream().map(Paper::getId).toList());
        } else {
            if (getCurrentUserRole() == 2) {
                Long currentUserId = getCurrentUserId();
                List<Long> myCourseIds = courseUserService.list(new LambdaQueryWrapper<CourseUser>()
                        .eq(CourseUser::getUserId, currentUserId)
                        .eq(CourseUser::getRole, 2))
                        .stream().map(CourseUser::getCourseId).toList();
                
                if (myCourseIds.isEmpty()) return Result.success(new Page<ReviewListResp>());
                
                List<Paper> myPapers = paperService.list(new LambdaQueryWrapper<Paper>().in(Paper::getCourseId, myCourseIds));
                if (myPapers.isEmpty()) return Result.success(new Page<ReviewListResp>());
                
                recordQuery.in(Record::getPaperId, myPapers.stream().map(Paper::getId).toList());
            }
        }

        // 2. 状态筛选
        if (status != null) {
            recordQuery.eq(Record::getStatus, status);
        } else {
            recordQuery.in(Record::getStatus, 2, 3);
        }
        
        // 3. 姓名搜索
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

        // 4. 执行分页查询
        IPage<Record> recordPage = recordService.page(new Page<>(page, size), recordQuery);

        // 5. 组装 VO
        List<ReviewListResp> voList = new ArrayList<>();
        if (!recordPage.getRecords().isEmpty()) {
            List<Long> userIds = recordPage.getRecords().stream().map(Record::getUserId).distinct().toList();
            List<Long> pIds = recordPage.getRecords().stream().map(Record::getPaperId).distinct().toList();
            
            Map<Long, User> userMap = userService.listByIds(userIds).stream()
                    .collect(Collectors.toMap(User::getId, Function.identity()));
            Map<Long, Paper> paperMap = paperService.listByIds(pIds).stream()
                    .collect(Collectors.toMap(Paper::getId, Function.identity()));
            
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

                vo.setReviewStatusDesc(record.getStatus() == 3 ? "已完成" : "待复核");
                voList.add(vo);
            }
        }

        Page<ReviewListResp> resultPage = new Page<>(page, size);
        resultPage.setTotal(recordPage.getTotal());
        resultPage.setRecords(voList);

        return Result.success(resultPage);
    }

    /**
     * 获取阅卷详情
     * GET /api/review/detail/{recordId}
     */
    @GetMapping("/detail/{recordId}")
    public Result<?> getReviewDetail(@PathVariable Long recordId) {
        Record record = recordService.getById(recordId);
        if (record == null) throw new BizException(404, "记录不存在");

        checkPermission(record);

        List<RecordDetail> details = recordDetailService.list(new LambdaQueryWrapper<RecordDetail>()
                .eq(RecordDetail::getRecordId, recordId));
        
        List<Long> qIds = details.stream().map(RecordDetail::getQuestionId).collect(Collectors.toList());
        Map<Long, Question> qMap = questionService.listByIds(qIds).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));

        List<Map<String, Object>> questionList = new ArrayList<>();
        for (RecordDetail detail : details) {
            Question q = qMap.get(detail.getQuestionId());
            if (q == null) continue;

            Map<String, Object> item = new HashMap<>();
            item.put("detailId", detail.getId());
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
     * 提交单题人工复核结果
     * POST /api/review/submit
     */
    @PostMapping("/submit")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> submitReview(@Valid @RequestBody ReviewSubmitReq req) {
        // 调用封装的批量逻辑，将单个请求包装成List
        List<ReviewSubmitReq> list = new ArrayList<>();
        list.add(req);
        return submitReviewBatch(list);
    }

    /**
     * 批量提交人工复核结果 (核心修复逻辑)
     * POST /api/review/submit-batch
     */
    @PostMapping("/submit-batch")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> submitReviewBatch(@Valid @RequestBody List<ReviewSubmitReq> reqList) {
        if (CollUtil.isEmpty(reqList)) {
            return Result.error(400,"提交列表不能为空");
        }

        // 取出 recordId (同一批次属于同一个 record)
        Long recordId = reqList.get(0).getRecordId();
        Record record = recordService.getById(recordId);
        if (record == null) throw new BizException(404, "记录不存在");
        
        checkPermission(record);

        // 1. 获取当前数据库中的所有明细，构建 Map 方便查找
        List<RecordDetail> dbDetails = recordDetailService.list(new LambdaQueryWrapper<RecordDetail>()
                .eq(RecordDetail::getRecordId, recordId));
        Map<Long, RecordDetail> detailMap = dbDetails.stream()
                .collect(Collectors.toMap(RecordDetail::getQuestionId, Function.identity()));

        List<RecordDetail> updates = new ArrayList<>();

        // 2. 遍历请求，更新内存对象并收集需要更新的记录
        for (ReviewSubmitReq req : reqList) {
            if (!recordId.equals(req.getRecordId())) {
                throw new BizException(400, "批量提交只能包含同一份试卷的记录");
            }

            RecordDetail detail = detailMap.get(req.getQuestionId());
            if (detail == null) {
                // 如果题目不存在，可能是脏数据，选择跳过或报错。这里选择跳过。
                continue;
            }

            // 校验分数
            if (req.getScore().compareTo(detail.getMaxScore()) > 0) {
                throw new BizException(400, "题目ID " + req.getQuestionId() + " 打分不能超过满分: " + detail.getMaxScore());
            }

            // 更新字段
            detail.setScore(req.getScore());
            if (StringUtils.hasText(req.getComment())) {
                detail.setAiComment(req.getComment());
            }
            detail.setIsMarked((byte) 1); // 标记为已批改
            // 简单逻辑：满分即正确
            detail.setIsCorrect(req.getScore().compareTo(detail.getMaxScore()) == 0 ? (byte)1 : (byte)0);
            detail.setUpdateBy(getCurrentUserId());
            detail.setUpdateTime(LocalDateTime.now());
            
            updates.add(detail);
        }

        // 3. 批量更新到数据库
        if (!updates.isEmpty()) {
            recordDetailService.updateBatchById(updates);
        }

        // 4. 计算新的总分 (基于内存中已更新的 dbDetails)
        BigDecimal newTotal = dbDetails.stream()
                .map(d -> d.getScore() == null ? BigDecimal.ZERO : d.getScore())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        record.setTotalScore(newTotal);

        // 5. 检查状态：是否所有题目都已批改
        boolean allMarked = dbDetails.stream().allMatch(d -> d.getIsMarked() == 1);
        if (allMarked) {
            record.setStatus((byte) 3); // 已完成
        } else {
            // 如果之前是完成状态，现在可能有题目被回退（虽然目前业务没这逻辑），还是保持严谨
            record.setStatus((byte) 2); 
        }
        
        // 6. 更新试卷记录
        recordService.updateById(record);

        return Result.success(newTotal, "批量提交成功");
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