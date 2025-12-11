package com.university.exam.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.university.exam.common.exception.BizException;
import com.university.exam.common.utils.DifyClient;
import com.university.exam.entity.*;
import com.university.exam.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 智能出题服务实现类
 *
 * @author MySQL数据库架构师
 * @version 1.7.0 (同步题目到查重库)
 * @since 2025-12-10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionGenerationServiceImpl implements QuestionGenerationService {

    private final DifyClient difyClient;
    private final AiTaskService aiTaskService;
    private final QuestionService questionService;
    private final NoticeService noticeService;
    private final UserNoticeService userNoticeService;
    private final CourseService courseService;

    @Autowired
    @Lazy
    private QuestionGenerationService self;

    private static final int BATCH_SIZE = 10;

    @Override
    public Long startGenerationTask(Long courseId, String topic, int totalCount, String difficulty, List<Integer> types, Long userId, String token) {
        String apiKey = difyClient.getGenerationKey();
        if (!StringUtils.hasText(apiKey) || apiKey.contains("******")) {
            throw new BizException(500, "系统未配置有效的 AI 出题 API Key");
        }

        AiTask task = new AiTask();
        String typeNameDesc = getTypeNameDesc(types);
        task.setTaskName(String.format("智能出题-%s-%s", typeNameDesc, topic));
        task.setType((byte) 1);
        task.setTotalCount(totalCount);
        task.setCurrentCount(0);
        task.setStatus((byte) 1);
        task.setCreateBy(userId);
        task.setUpdateBy(userId);
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());

        aiTaskService.save(task);

        self.processGenerationAsync(
                task.getId(), courseId, topic, totalCount, difficulty, types, userId, apiKey, token
        );

        return task.getId();
    }

    @Override
    @Async("aiTaskExecutor")
    public void processGenerationAsync(Long taskId, Long courseId, String topic, int totalCount, String difficulty, List<Integer> types, Long userId, String apiKey, String token) {
        log.info(">>> 开始执行异步出题: taskId={}, topic={}", taskId, topic);

        Course course = courseService.getById(courseId);
        String courseName = (course != null) ? course.getCourseName() : "";
        String questionTypeParam = getTypeIdsString(types);

        int generatedCount = 0;
        int failedAttempts = 0;

        // 获取查重库配置 (注意：我们复用 KnowledgeKey 来操作查重库，或者你需要单独配置一个 Key)
        // 假设这里复用 KnowledgeKey，因为它有 Dataset 操作权限
        String knowledgeApiKey = difyClient.getKnowledgeKey();
        String historyDatasetId = difyClient.getHistoryDatasetId(); // 需要先在配置表添加 dify_history_dataset_id

        try {
            while (generatedCount < totalCount && failedAttempts < 3) {
                int currentBatchNeed = Math.min(BATCH_SIZE, totalCount - generatedCount);

                Map<String, Object> inputs = new HashMap<>();
                inputs.put("topic", topic);
                inputs.put("count", currentBatchNeed);
                inputs.put("difficulty", difficulty);
                inputs.put("course_name", courseName);
                inputs.put("types", questionTypeParam);
                if (StringUtils.hasText(token)) inputs.put("user_token", token);

                Map<String, Object> result = null;
                try {
                    result = difyClient.runWorkflow(apiKey, inputs, userId.toString());
                } catch (Exception e) {
                    log.error("Dify 调用异常", e);
                    failedAttempts++;
                    Thread.sleep(2000);
                    continue;
                }

                boolean batchSuccess = false;
                if (result != null && result.containsKey("data")) {
                    Map data = (Map) result.get("data");
                    Map outputs = (Map) data.get("outputs");

                    if (outputs != null) {
                        Object outputObj = outputs.get("result");
                        if (outputObj == null) outputObj = outputs.get("text");

                        if (outputObj != null) {
                            String jsonStr = outputObj instanceof String ? (String) outputObj : JSONUtil.toJsonStr(outputObj);
                            jsonStr = cleanJsonString(jsonStr);

                            try {
                                JSONArray questionsJson = JSONUtil.parseArray(jsonStr);
                                List<Question> validQuestions = new ArrayList<>();
                                StringBuilder syncText = new StringBuilder(); // 用于同步到查重库的文本

                                for (Object q : questionsJson) {
                                    JSONObject qJson = (JSONObject) q;
                                    String content = qJson.getStr("content");
                                    if (StrUtil.isBlank(content)) continue;

                                    String contentHash = DigestUtil.md5Hex(content);
                                    long exists = questionService.count(new LambdaQueryWrapper<Question>()
                                            .eq(Question::getContentHash, contentHash));

                                    if (exists > 0) continue;

                                    Question question = new Question();
                                    question.setCourseId(courseId);
                                    question.setType(parseType(qJson.getStr("type")));
                                    question.setContent(content);
                                    question.setContentHash(contentHash);
                                    question.setOptions(JSONUtil.toJsonStr(qJson.getJSONArray("options")));
                                    question.setAnswer(qJson.getStr("answer"));
                                    question.setAnalysis(qJson.getStr("analysis"));
                                    question.setDifficulty(parseDifficulty(difficulty));
                                    question.setTags(JSONUtil.toJsonStr(List.of(topic, "AI生成")));
                                    question.setCreateBy(userId);
                                    question.setUpdateBy(userId);
                                    question.setCreateTime(LocalDateTime.now());
                                    question.setUpdateTime(LocalDateTime.now());

                                    validQuestions.add(question);
                                    
                                    // 收集新题干，准备同步
                                    syncText.append(content).append("\n\n");
                                }

                                if (!validQuestions.isEmpty()) {
                                    questionService.saveBatch(validQuestions);
                                    generatedCount += validQuestions.size();
                                    batchSuccess = true;
                                    updateTaskProgress(taskId, generatedCount, totalCount, (byte) 1, null);

                                    // 核心修改：同步到 Dify 查重库
                                    if (StringUtils.hasText(historyDatasetId) && syncText.length() > 0) {
                                        difyClient.createDocumentByText(
                                                knowledgeApiKey, // 使用有知识库写权限的 Key
                                                historyDatasetId,
                                                syncText.toString(),
                                                "自动入库_" + taskId + "_" + System.currentTimeMillis()
                                        );
                                    }
                                }
                            } catch (Exception e) {
                                log.error("解析 JSON 失败", e);
                                failedAttempts++;
                            }
                        }
                    }
                }

                if (!batchSuccess) failedAttempts++; else failedAttempts = 0;

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            if (generatedCount == 0) {
                updateTaskProgress(taskId, 0, totalCount, (byte) 3, "未能生成有效题目");
                sendNotification(userId, "AI 出题失败", "任务 [" + topic + "] 执行失败。");
            } else {
                updateTaskProgress(taskId, generatedCount, totalCount, (byte) 2, null);
                sendNotification(userId, "AI 出题完成", "任务 [" + topic + "] 已完成，共生成 " + generatedCount + " 题。");
            }

        } catch (Exception e) {
            log.error("AI 出题任务异常", e);
            updateTaskProgress(taskId, generatedCount, totalCount, (byte) 3, e.getMessage());
        }
    }

    // ... (其他辅助方法保持不变)
    private void updateTaskProgress(Long taskId, int current, int total, byte status, String errorMsg) {
        AiTask update = new AiTask();
        update.setId(taskId);
        update.setCurrentCount(current);
        update.setStatus(status);
        if (errorMsg != null) update.setErrorMsg(errorMsg);
        update.setUpdateTime(LocalDateTime.now());
        aiTaskService.updateById(update);
    }

    private void sendNotification(Long userId, String title, String content) {
        try {
            Notice notice = new Notice();
            notice.setTitle(title);
            notice.setContent(content);
            notice.setType((byte) 1);
            notice.setTargetType((byte) 3);
            notice.setSenderId(0L);
            notice.setCreateTime(LocalDateTime.now());
            notice.setUpdateTime(LocalDateTime.now());
            noticeService.save(notice);

            UserNotice userNotice = new UserNotice();
            userNotice.setNoticeId(notice.getId());
            userNotice.setUserId(userId);
            userNotice.setIsRead((byte) 0);
            userNotice.setCreateTime(LocalDateTime.now());
            userNotice.setUpdateTime(LocalDateTime.now());
            userNoticeService.save(userNotice);
        } catch (Exception e) {
            log.error("发送通知失败", e);
        }
    }

    private String cleanJsonString(String jsonStr) {
        if (StrUtil.isBlank(jsonStr)) return "";
        jsonStr = jsonStr.trim();
        if (jsonStr.startsWith("```json")) jsonStr = jsonStr.substring(7);
        if (jsonStr.startsWith("```")) jsonStr = jsonStr.substring(3);
        if (jsonStr.endsWith("```")) jsonStr = jsonStr.substring(0, jsonStr.length() - 3);
        return jsonStr.trim();
    }

    private String getTypeIdsString(List<Integer> types) {
        if (types == null || types.isEmpty()) return "";
        return types.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private String getTypeNameDesc(List<Integer> types) {
        if (types == null || types.isEmpty()) return "混合题型";
        return types.stream().map(this::getTypeName).collect(Collectors.joining("、"));
    }

    private String getTypeName(Integer type) {
        switch (type) {
            case 1: return "单选";
            case 2: return "多选";
            case 3: return "判断";
            case 4: return "简答";
            case 5: return "填空";
            default: return "未知";
        }
    }

    private Byte parseType(String typeStr) {
        if (typeStr == null) return 1;
        try {
            int typeInt = Integer.parseInt(typeStr);
            if (typeInt >= 1 && typeInt <= 5) return (byte) typeInt;
        } catch (NumberFormatException e) {
            // ignore
        }
        if (typeStr.contains("填空") || "gap_filling".equalsIgnoreCase(typeStr)) return 5;
        if (typeStr.contains("单选") || "choice".equalsIgnoreCase(typeStr)) return 1;
        if (typeStr.contains("多选") || "multiple".equalsIgnoreCase(typeStr)) return 2;
        if (typeStr.contains("判断") || "boolean".equalsIgnoreCase(typeStr)) return 3;
        if (typeStr.contains("简答") || "essay".equalsIgnoreCase(typeStr)) return 4;
        return 1;
    }

    private Byte parseDifficulty(String diffStr) {
        if ("简单".equals(diffStr) || "1".equals(diffStr)) return 1;
        if ("困难".equals(diffStr) || "3".equals(diffStr)) return 3;
        return 2;
    }
}