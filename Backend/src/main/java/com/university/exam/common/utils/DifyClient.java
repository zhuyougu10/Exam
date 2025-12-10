package com.university.exam.common.utils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.university.exam.common.exception.BizException;
import com.university.exam.entity.Config;
import com.university.exam.service.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Dify AI API 客户端封装
 * 基于 Spring 6 RestClient 实现，支持动态 Key 和日志脱敏
 *
 * @author MySQL数据库架构师
 * @version 1.4.0 (修复doc_form参数)
 * @since 2025-12-10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DifyClient {

    private final ConfigService configService;

    // ================== 系统配置键常量 (对应 sys_config 表) ==================
    public static final String KEY_BASE_URL = "dify_base_url";
    public static final String KEY_KNOWLEDGE_API_KEY = "dify_key_knowledge";
    public static final String KEY_GENERATION_API_KEY = "dify_key_generation";
    public static final String KEY_GRADING_API_KEY = "dify_key_grading";
    public static final String KEY_GLOBAL_DATASET_ID = "dify_global_dataset_id";

    /**
     * 获取配置好的 RestClient 实例
     */
    private RestClient getClient(String apiKey) {
        return RestClient.builder()
                .baseUrl(getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .requestInterceptor(new DifyRequestLogger())
                .build();
    }

    // ================== 配置获取辅助方法 ==================

    public String getBaseUrl() {
        return getConfigValue(KEY_BASE_URL);
    }

    public String getKnowledgeKey() {
        return getConfigValue(KEY_KNOWLEDGE_API_KEY);
    }

    public String getGenerationKey() {
        return getConfigValue(KEY_GENERATION_API_KEY);
    }

    public String getGradingKey() {
        return getConfigValue(KEY_GRADING_API_KEY);
    }

    public String getGlobalDatasetId() {
        return getConfigValue(KEY_GLOBAL_DATASET_ID);
    }

    private String getConfigValue(String key) {
        Config config = configService.getOne(new LambdaQueryWrapper<Config>()
                .eq(Config::getConfigKey, key));

        if (config == null || !StringUtils.hasText(config.getConfigValue())) {
            throw new BizException(500, "系统未配置参数: " + key);
        }
        return config.getConfigValue();
    }

    // ================== 核心通信方法 ==================

    /**
     * 执行工作流 (Workflow)
     */
    public Map runWorkflow(String apiKey, Map<String, Object> inputs, String userId) {
        Map<String, Object> body = new HashMap<>();
        body.put("inputs", inputs);
        body.put("response_mode", "blocking");
        body.put("user", userId);

        try {
            return getClient(apiKey).post()
                    .uri("/workflows/run")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
        } catch (HttpClientErrorException e) {
            handleHttpClientError(e);
            return null;
        } catch (Exception e) {
            log.error("Dify 工作流执行失败", e);
            throw new BizException(500, "AI 服务调用失败: " + e.getMessage());
        }
    }

    /**
     * 上传文档到知识库
     * URL: POST {base_url}/datasets/{dataset_id}/document/create_by_file
     */
    public Map uploadDocument(String apiKey, String datasetId, Resource fileResource) {
        if (fileResource == null) {
            throw new BizException(400, "上传文件资源不能为空");
        }

        try {
            MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
            parts.add("file", fileResource);

            // 修复: 移除 "doc_form":"text_model"
            // 原因: Dify 知识库已有预设类型(如文本或QA)，强制指定不匹配的类型会导致 400 错误。
            // 移除后，Dify 会自动使用该知识库本身的类型配置。
            String dataJson = "{\"indexing_technique\":\"high_quality\",\"process_rule\":{\"mode\":\"automatic\"}}";
            parts.add("data", dataJson);

            return getClient(apiKey).post()
                    .uri("/datasets/{dataset_id}/document/create_by_file", datasetId)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(parts)
                    .retrieve()
                    .body(Map.class);

        } catch (HttpClientErrorException e) {
            handleHttpClientError(e);
            return null;
        } catch (Exception e) {
            log.error("Dify 文档上传失败", e);
            throw new BizException(500, "知识库上传失败: " + e.getMessage());
        }
    }

    /**
     * 删除知识库文档
     */
    public void deleteDocument(String apiKey, String datasetId, String documentId) {
        try {
            getClient(apiKey).delete()
                    .uri("/datasets/{dataset_id}/documents/{document_id}", datasetId, documentId)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Dify 文档删除成功: datasetId={}, docId={}", datasetId, documentId);
        } catch (HttpClientErrorException e) {
            log.warn("Dify 文档删除异常: {}", e.getMessage());
            // 删除时如果报 404，可以忽略，视为已删除
            if (e.getStatusCode().value() != 404) {
                handleHttpClientError(e);
            }
        } catch (Exception e) {
            log.error("Dify 文档删除失败", e);
            throw new BizException(500, "知识库文档删除失败: " + e.getMessage());
        }
    }

    /**
     * 统一处理 Dify API 错误响应
     */
    private void handleHttpClientError(HttpClientErrorException e) {
        log.error("Dify API Error: Status={}, Body={}", e.getStatusCode(), e.getResponseBodyAsString());
        if (e.getStatusCode().value() == 401) {
            throw new BizException(401, "Dify API Key 无效或过期，请检查系统设置");
        } else if (e.getStatusCode().value() == 403) {
            throw new BizException(403, "Dify API 权限不足，请检查应用权限");
        } else if (e.getStatusCode().value() == 404) {
            throw new BizException(404, "Dify 资源不存在 (Dataset ID 可能错误)");
        } else if (e.getStatusCode().value() == 400) {
            throw new BizException(400, "Dify 参数错误: " + e.getResponseBodyAsString());
        }
        throw new BizException(e.getStatusCode().value(), "Dify 服务错误: " + e.getStatusText());
    }

    /**
     * 自定义请求日志拦截器 (实现日志脱敏)
     */
    private static class DifyRequestLogger implements ClientHttpRequestInterceptor {
        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
            logRequest(request, body);
            ClientHttpResponse response = execution.execute(request, body);
            logResponse(response);
            return response;
        }

        private void logRequest(HttpRequest request, byte[] body) {
            HttpHeaders headers = new HttpHeaders();
            headers.putAll(request.getHeaders());

            if (headers.containsKey(HttpHeaders.AUTHORIZATION)) {
                headers.set(HttpHeaders.AUTHORIZATION, "Bearer sk-******");
            }

            String bodyStr = new String(body, StandardCharsets.UTF_8);
            MediaType contentType = request.getHeaders().getContentType();
            if (contentType != null && contentType.isCompatibleWith(MediaType.MULTIPART_FORM_DATA)) {
                bodyStr = "[Multipart Data (Binary Omitted)]";
            }

            log.info(">>> Dify Req: Method={}, URI={}, Headers={}, Body={}",
                    request.getMethod(), request.getURI(), headers, bodyStr);
        }

        private void logResponse(ClientHttpResponse response) throws IOException {
            log.info("<<< Dify Res: Status={}", response.getStatusCode());
        }
    }
}