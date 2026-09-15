package cn.ecom.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek 大模型客户端（OpenAI 兼容接口，仅用 Spring RestTemplate + fastjson，不新增依赖）。
 * 客服「规则优先 + 大模型兜底」中的兜底层：api-key 未配置或调用失败时返回 null，由调用方降级为转人工。
 */
@Component
public class DeepSeekClient {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekClient.class);

    @Value("${deepseek.api-key:}")
    private String apiKey;

    @Value("${deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${deepseek.model:deepseek-reasoner}")
    private String model;

    @Value("${deepseek.timeout-seconds:60}")
    private int timeoutSeconds;

    private volatile RestTemplate restTemplate;

    private RestTemplate restTemplate() {
        if (restTemplate == null) {
            synchronized (this) {
                if (restTemplate == null) {
                    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                    factory.setConnectTimeout(5000);
                    factory.setReadTimeout(timeoutSeconds * 1000);
                    restTemplate = new RestTemplate(factory);
                }
            }
        }
        return restTemplate;
    }

    /** 是否已配置密钥 */
    public boolean available() {
        return StringUtils.hasText(apiKey);
    }

    /**
     * 单轮问答，返回模型回答正文；未配置密钥 / 超时 / 异常 / 空回答时返回 null（调用方转人工）
     */
    public String chat(String systemPrompt, String question) {
        if (!available()) {
            return null;
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(message("system", systemPrompt));
            messages.add(message("user", question));
            body.put("messages", messages);
            body.put("stream", false);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            HttpEntity<String> entity = new HttpEntity<>(JSON.toJSONString(body), headers);

            ResponseEntity<String> response = restTemplate()
                    .postForEntity(baseUrl + "/chat/completions", entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || !StringUtils.hasText(response.getBody())) {
                log.warn("DeepSeek 调用失败：HTTP {}，降级转人工", response.getStatusCodeValue());
                return null;
            }

            JSONObject json = JSON.parseObject(response.getBody());
            JSONArray choices = json.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                return null;
            }
            JSONObject message = choices.getJSONObject(0).getJSONObject("message");
            String content = message == null ? null : message.getString("content");
            return StringUtils.hasText(content) ? content.trim() : null;
        } catch (Exception e) {
            log.warn("DeepSeek 调用异常，降级转人工：{}", e.getMessage());
            return null;
        }
    }

    private Map<String, String> message(String role, String content) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("role", role);
        m.put("content", content);
        return m;
    }
}
