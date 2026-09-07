package com.holocockpit.controller;

import com.holocockpit.common.Result;
import com.holocockpit.entity.AlertRecord;
import com.holocockpit.service.AiService;
import com.holocockpit.service.AlertService;
import com.holocockpit.service.ReportService;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 智能接口：流式对话、智能查询、智能预警、AI 报告
 */
@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private AiService aiService;

    @Resource
    private AlertService alertService;

    @Resource
    private ReportService reportService;

    /**
     * 对话请求体
     */
    @Data
    public static class ChatRequest {
        private String message;
        private List<Map<String, String>> history;
    }

    /**
     * 智能查询请求体
     */
    @Data
    public static class QueryRequest {
        private String question;
    }

    /**
     * AI 流式对话（SSE）：data 为 JSON 字符串文本片段，[DONE] 表示结束
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody ChatRequest body) {
        if (body.getMessage() == null || body.getMessage().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "消息不能为空");
        }
        return aiService.chatStream(body.getMessage().trim(), body.getHistory());
    }

    /**
     * 智能查询：自然语言 → SQL → 执行 → 中文解读，返回 {sql, columns, rows, reply}
     */
    @PostMapping("/query")
    public Result<Map<String, Object>> query(@RequestBody QueryRequest body) {
        if (body.getQuestion() == null || body.getQuestion().trim().isEmpty()) {
            return Result.error(400, "问题不能为空");
        }
        return Result.success(aiService.query(body.getQuestion().trim()));
    }

    /**
     * 查询最新预警（最近20条）
     */
    @GetMapping("/alerts")
    public Result<List<AlertRecord>> getAlerts() {
        return Result.success(alertService.getAlerts());
    }

    /**
     * 触发预警检测，返回 {count: 新增预警数}
     */
    @PostMapping("/alerts/check")
    public Result<Map<String, Object>> checkAlerts() {
        int count = alertService.checkAlerts();
        Map<String, Object> data = new HashMap<>();
        data.put("count", count);
        return Result.success("检测完成", data);
    }

    /**
     * 生成 AI 数据报告（HTML）
     */
    @PostMapping("/report")
    public Result<Map<String, Object>> report() {
        String html = reportService.generate();
        Map<String, Object> data = new HashMap<>();
        data.put("html", html);
        return Result.success(data);
    }
}
