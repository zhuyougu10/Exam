package com.university.exam.common.utils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.university.exam.common.exception.BizException;
import com.university.exam.entity.Config;
import com.university.exam.service.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
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
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Dify AI API 客户端封装
 * 基于 Spring 6 RestClient 实现，支持动态 Key 和日志脱敏
 *
 * @author MySQL数据库架构师
 * @version 1.1.0
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

    /**
     * 获取 Dify Base URL
     */
    public String getBaseUrl() {
        return getConfigValue(KEY_BASE_URL);
    }

    /**
     * 获取知识库专用 API Key
     */
    public String getKnowledgeKey() {
        return getConfigValue(KEY_KNOWLEDGE_API_KEY);
    }

    /**
     * 获取出题应用 API Key
     */
    public String getGenerationKey() {
        return getConfigValue(KEY_GENERATION_API_KEY);
    }

    /**
     * 获取阅卷应用 API Key
     */
    public String getGradingKey() {
        return getConfigValue(KEY_GRADING_API_KEY);
    }

    /**
     * 获取全局知识库 Dataset ID
     */
    public String getGlobalDatasetId() {
        return getConfigValue(KEY_GLOBAL_DATASET_ID);
    }

    /**
     * 通用配置获取逻辑
     */
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
     * URL: POST {base_url}/workflows/run
     *
     * @param apiKey 工作流应用的 API Key (可通过 getGenerationKey() 等方法获取)
     * @param inputs 输入参数 Map
     * @param userId 用户标识
     * @return 响应结果 Map (通常包含 data.outputs)
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
        } catch (Exception e) {
            log.error("Dify 工作流执行失败", e);
            throw new BizException(500, "AI 服务调用失败: " + e.getMessage());
        }
    }

    /**
     * 上传文档到知识库
     * URL: POST {base_url}/datasets/{dataset_id}/document/create_by_file
     *
     * @param apiKey    知识库应用的 API Key (可通过 getKnowledgeKey() 获取)
     * @param datasetId 知识库 ID
     * @param file      上传的文件
     * @return 响应结果 Map (包含 document 信息)
     */
    public Map uploadDocument(String apiKey, String datasetId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new BizException(400, "上传文件不能为空");
        }

        try {
            // 构建 Multipart 表单数据
            MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
            
            // 封装文件资源，必须重写 getFilename 以便 RestClient 识别
            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
            parts.add("file", fileResource);
            
            // 构建 data 参数 (JSON 字符串)
            // 默认使用高质量索引和自动分段规则
            String dataJson = "{\"indexing_technique\":\"high_quality\",\"process_rule\":{\"mode\":\"automatic\"}}";
            parts.add("data", dataJson);

            return getClient(apiKey).post()
                    .uri("/datasets/{dataset_id}/document/create_by_file", datasetId)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(parts)
                    .retrieve()
                    .body(Map.class);

        } catch (IOException e) {
            throw new BizException(500, "文件处理失败");
        } catch (Exception e) {
            log.error("Dify 文档上传失败", e);
            throw new BizException(500, "知识库上传失败: " + e.getMessage());
        }
    }

    /**
     * 删除知识库文档
     * URL: DELETE {base_url}/datasets/{dataset_id}/documents/{document_id}
     *
     * @param apiKey     知识库应用的 API Key
     * @param datasetId  知识库 ID
     * @param documentId 文档 ID
     */
    public void deleteDocument(String apiKey, String datasetId, String documentId) {
        try {
            getClient(apiKey).delete()
                    .uri("/datasets/{dataset_id}/documents/{document_id}", datasetId, documentId)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Dify 文档删除成功: datasetId={}, docId={}", datasetId, documentId);
        } catch (Exception e) {
            log.error("Dify 文档删除失败", e);
            throw new BizException(500, "知识库文档删除失败: " + e.getMessage());
        }
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

            // 核心脱敏逻辑：替换 Authorization 头
            if (headers.containsKey(HttpHeaders.AUTHORIZATION)) {
                headers.set(HttpHeaders.AUTHORIZATION, "Bearer sk-******");
            }

            String bodyStr = new String(body, StandardCharsets.UTF_8);
            // 对于文件上传，body可能是乱码或太长，简化显示
            if (request.getHeaders().getContentType() != null && 
                request.getHeaders().getContentType().toString().contains(MediaType.MULTIPART_FORM_DATA_VALUE)) {
                bodyStr = "[Multipart Data]";
            }

            log.info(">>> Dify Req: Method={}, URI={}, Headers={}, Body={}",
                    request.getMethod(), request.getURI(), headers, bodyStr);
        }

        private void logResponse(ClientHttpResponse response) throws IOException {
            log.info("<<< Dify Res: Status={}", response.getStatusCode());
        }
    }
}