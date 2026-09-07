package com.holocockpit.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 火山方舟（豆包大模型）客户端封装
 * OpenAI 兼容格式：POST {base-url}/chat/completions
 */
@Service
public class ArkClient {

    @Value("${ark.api-key}")
    private String apiKey;

    @Value("${ark.base-url}")
    private String baseUrl;

    @Value("${ark.model}")
    private String model;

    private RestTemplate restTemplate;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @PostConstruct
    public void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10 * 1000);
        factory.setReadTimeout(60 * 1000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * 非流式对话：返回首条回复内容，失败抛出异常
     */
    public String chat(List<Map<String, String>> messages) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("stream", false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        String response = restTemplate.postForObject(
                baseUrl + "/chat/completions", new HttpEntity<>(body, headers), String.class);
        JSONObject json = JSONUtil.parseObj(response);
        JSONArray choices = json.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            return "";
        }
        JSONObject message = choices.getJSONObject(0).getJSONObject("message");
        return message != null ? message.getStr("content", "") : "";
    }

    /**
     * 流式对话：逐行读取 SSE 响应，把 delta.content 以 JSON 字符串形式转发给前端 SseEmitter
     */
    public void chatStream(List<Map<String, String>> messages, SseEmitter emitter) throws IOException {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("stream", true);
        String jsonBody = JSONUtil.toJsonStr(body);

        restTemplate.execute(baseUrl + "/chat/completions", HttpMethod.POST,
                request -> {
                    HttpHeaders headers = request.getHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.setBearerAuth(apiKey);
                    request.getBody().write(jsonBody.getBytes(StandardCharsets.UTF_8));
                },
                response -> {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (!line.startsWith("data:")) {
                                continue;
                            }
                            String payload = line.substring(5).trim();
                            if (payload.isEmpty()) {
                                continue;
                            }
                            if ("[DONE]".equals(payload)) {
                                break;
                            }
                            String content = extractDeltaContent(payload);
                            if (content != null && !content.isEmpty()) {
                                // 以带引号的 JSON 字符串发送（换行转义），避免破坏 SSE 报文
                                emitter.send(SseEmitter.event().data(toJsonString(content)));
                            }
                        }
                    }
                    return null;
                });
    }

    /**
     * 字符串 → 带引号的 JSON 字符串（内部换行/引号会被转义）
     */
    public static String toJsonString(String s) {
        try {
            return MAPPER.writeValueAsString(s != null ? s : "");
        } catch (Exception e) {
            return "\"\"";
        }
    }

    /**
     * 从流式分片中提取 delta.content
     */
    private String extractDeltaContent(String payload) {
        try {
            JSONObject chunk = JSONUtil.parseObj(payload);
            JSONArray choices = chunk.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                return null;
            }
            JSONObject delta = choices.getJSONObject(0).getJSONObject("delta");
            return delta != null ? delta.getStr("content") : null;
        } catch (Exception e) {
            return null;
        }
    }
}
