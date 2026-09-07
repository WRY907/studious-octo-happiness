package com.holocockpit.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.holocockpit.entity.*;
import com.holocockpit.mapper.CitySalesMapper;
import com.holocockpit.mapper.HotProductMapper;
import com.holocockpit.mapper.RegionSalesMapper;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 服务：驾驶舱智能对话（流式）与自然语言转 SQL 智能查询
 */
@Service
public class AiService {

    @Resource
    private ArkClient arkClient;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private CockpitService cockpitService;

    @Resource
    private RegionSalesMapper regionSalesMapper;

    @Resource
    private CitySalesMapper citySalesMapper;

    @Resource
    private HotProductMapper hotProductMapper;

    /**
     * 对话流式转发线程池
     */
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * 允许查询的表白名单（8张业务表）
     */
    private static final Set<String> ALLOWED_TABLES = new HashSet<>(Arrays.asList(
            "phone_model", "region_sales", "city_sales", "realtime_order",
            "hot_product", "traffic_source", "user_profile", "sales_trend"));

    /**
     * 禁止的关键字（大小写不敏感，词边界匹配）
     */
    private static final Pattern FORBIDDEN_PATTERN = Pattern.compile(
            "(?i)\\b(insert|update|delete|drop|alter|truncate|create|grant|into)\\b");

    /**
     * 提取 FROM / JOIN 后的表名
     */
    private static final Pattern TABLE_PATTERN = Pattern.compile(
            "(?i)\\b(from|join)\\s+[`]?([a-z_][a-z0-9_]*)[`]?");

    /**
     * 8张业务表结构说明（用于生成SQL的提示词）
     */
    private static final String TABLE_DDL = ""
            + "phone_model(id BIGINT 主键, model_name VARCHAR 机型名称, series VARCHAR 系列, price DECIMAL 价格, rating DECIMAL 评分, image_url VARCHAR 图片, create_time DATETIME, update_time DATETIME)\n"
            + "region_sales(id BIGINT 主键, province VARCHAR 省份, sales DECIMAL 销售额, orders INT 订单数, users INT 用户数, stat_date DATE 统计日期)\n"
            + "city_sales(id BIGINT 主键, city VARCHAR 城市, sales DECIMAL 销售额, orders INT 订单数, users INT 用户数, stat_date DATE 统计日期)\n"
            + "realtime_order(id BIGINT 主键, order_no VARCHAR 订单号, user_name VARCHAR 用户名, model_name VARCHAR 机型名称, amount DECIMAL 金额, province VARCHAR 省份, city VARCHAR 城市, status TINYINT 状态1待付款2已付款3已发货4已完成, create_time DATETIME 下单时间)\n"
            + "hot_product(id BIGINT 主键, rank_no INT 排名, model_name VARCHAR 机型名称, sales_count INT 销量, sales_amount DECIMAL 销售额, growth DECIMAL 增长率)\n"
            + "traffic_source(id BIGINT 主键, category VARCHAR 一级分类, source_name VARCHAR 二级来源, visits BIGINT 访问量, ratio DECIMAL 占比)\n"
            + "user_profile(id BIGINT 主键, profile_type VARCHAR 类型gender性别age年龄, profile_name VARCHAR 名称, user_count INT 用户数, ratio DECIMAL 占比)\n"
            + "sales_trend(id BIGINT 主键, stat_date DATE 日期, sales DECIMAL 销售额, orders INT 订单数, visits INT 访问量, avg_order_value DECIMAL 客单价)";

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }

    /**
     * 构建数据上下文系统提示词（今日总览 + TOP3省份/城市/机型）
     */
    public String buildContext() {
        StringBuilder sb = new StringBuilder();
        sb.append("你是华为手机数据驾驶舱AI分析师，请用简洁、专业的中文回答。\n");

        OverviewStats overview = cockpitService.getOverview();
        LocalDate latestDate = cockpitService.maxDate("region_sales");
        sb.append("当前数据概况（数据日期：").append(latestDate != null ? latestDate.toString() : "无").append("）：\n");
        if (overview.getId() != null) {
            sb.append("- 累计销售额 ").append(nvl(overview.getTotalSales())).append(" 元，累计订单 ")
                    .append(nvl(overview.getTotalOrders())).append(" 单，累计用户 ")
                    .append(nvl(overview.getTotalUsers())).append(" 人；\n");
            sb.append("- 今日销售额 ").append(nvl(overview.getTodaySales())).append(" 元，今日订单 ")
                    .append(nvl(overview.getTodayOrders())).append(" 单，新增用户 ")
                    .append(nvl(overview.getTodayNewUsers())).append(" 人，访问量 ")
                    .append(nvl(overview.getTodayVisits())).append("，转化率 ")
                    .append(nvl(overview.getConversionRate())).append("%；\n");
            sb.append("- 销售额环比 ").append(nvl(overview.getSalesGrowth())).append("%，订单环比 ")
                    .append(nvl(overview.getOrdersGrowth())).append("%。\n");
        }

        // TOP3 省份
        List<RegionSales> regions = regionSalesMapper.selectList(
                new LambdaQueryWrapper<RegionSales>()
                        .eq(RegionSales::getStatDate, latestDate)
                        .orderByDesc(RegionSales::getSales)
                        .last("LIMIT 3"));
        if (!regions.isEmpty()) {
            sb.append("- 销售额TOP3省份：");
            for (RegionSales r : regions) {
                sb.append(r.getProvince()).append("(").append(nvl(r.getSales())).append("元) ");
            }
            sb.append("\n");
        }

        // TOP3 城市
        List<CitySales> cities = citySalesMapper.selectList(
                new LambdaQueryWrapper<CitySales>()
                        .eq(CitySales::getStatDate, latestDate)
                        .orderByDesc(CitySales::getSales)
                        .last("LIMIT 3"));
        if (!cities.isEmpty()) {
            sb.append("- 销售额TOP3城市：");
            for (CitySales c : cities) {
                sb.append(c.getCity()).append("(").append(nvl(c.getSales())).append("元) ");
            }
            sb.append("\n");
        }

        // TOP3 机型
        List<HotProduct> hots = hotProductMapper.selectList(
                new LambdaQueryWrapper<HotProduct>()
                        .orderByAsc(HotProduct::getRankNo)
                        .last("LIMIT 3"));
        if (!hots.isEmpty()) {
            sb.append("- 热销TOP3机型：");
            for (HotProduct h : hots) {
                sb.append(h.getModelName()).append("(销量").append(nvl(h.getSalesCount()))
                        .append("台，").append(nvl(h.getSalesAmount())).append("元) ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * 智能对话（流式）：Ark 失败时降级本地模板回复，完成后发送 [DONE]
     */
    public SseEmitter chatStream(String message, List<Map<String, String>> history) {
        SseEmitter emitter = new SseEmitter(120_000L);
        emitter.onTimeout(emitter::complete);

        executor.execute(() -> {
            try {
                List<Map<String, String>> messages = new ArrayList<>();
                messages.add(system(buildContext()));
                // 携带最近的历史对话（最多12条）
                if (history != null) {
                    int from = Math.max(0, history.size() - 12);
                    for (Map<String, String> h : history.subList(from, history.size())) {
                        String role = h.get("role");
                        String content = h.get("content");
                        if (role != null && content != null && !content.trim().isEmpty()) {
                            Map<String, String> m = new HashMap<>();
                            m.put("role", role);
                            m.put("content", content);
                            messages.add(m);
                        }
                    }
                }
                messages.add(user(message));
                arkClient.chatStream(messages, emitter);
            } catch (Exception e) {
                // Ark 失败：降级本地模板回复
                try {
                    emitter.send(SseEmitter.event().data(ArkClient.toJsonString(localReply(message))));
                } catch (Exception ignore) {
                    // 前端已断开
                }
            }
            try {
                emitter.send("[DONE]");
                emitter.complete();
            } catch (Exception ignore) {
                // 前端已断开
            }
        });
        return emitter;
    }

    /**
     * 本地降级模板回复（Ark 不可用时）
     */
    private String localReply(String message) {
        OverviewStats overview = cockpitService.getOverview();
        return "⚠️ AI 大模型服务暂不可用（可能是 API Key 未开通模型或网络不通），当前展示本地数据概况：\n"
                + "- 累计销售额：" + nvl(overview.getTotalSales()) + " 元，累计订单：" + nvl(overview.getTotalOrders()) + " 单\n"
                + "- 今日销售额：" + nvl(overview.getTodaySales()) + " 元，今日订单：" + nvl(overview.getTodayOrders()) + " 单\n"
                + "- 今日访问量：" + nvl(overview.getTodayVisits()) + "，转化率：" + nvl(overview.getConversionRate()) + "%\n"
                + "恢复后我将详细分析您的问题「" + message + "」。";
    }

    /**
     * 智能查询：自然语言 → SQL（校验）→ 执行（最多50行）→ 中文解读
     */
    public Map<String, Object> query(String question) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sql", "");
        result.put("columns", new ArrayList<>());
        result.put("rows", new ArrayList<>());
        String sql = null;
        try {
            // ① 调 Ark 生成 SQL
            List<Map<String, String>> genMessages = new ArrayList<>();
            genMessages.add(system("你是SQL生成助手。以下是数据库表结构：\n" + TABLE_DDL
                    + "\n规则：只生成 SELECT 查询语句；只允许查询以上这些表；直接输出SQL本身，不要解释，不要使用markdown代码块。"));
            genMessages.add(user(question));
            sql = cleanSql(arkClient.chat(genMessages));
            validateSql(sql);
            result.put("sql", sql);

            // ② 执行查询（最多50行）
            List<String> columns = new ArrayList<>();
            List<Map<String, Object>> rows = executeQuery(sql, columns);
            result.put("columns", columns);
            result.put("rows", rows);

            // ③ 结果回传 Ark 生成中文解读
            List<Map<String, String>> explainMessages = new ArrayList<>();
            explainMessages.add(system(buildContext()));
            explainMessages.add(user("用户问题：" + question
                    + "\n执行的SQL：" + sql
                    + "\n查询结果（JSON，最多50行）：" + JSONUtil.toJsonStr(rows)
                    + "\n请用简洁专业的中文解读查询结果：先直接回答问题，再给出1-2条分析建议。"));
            String reply = arkClient.chat(explainMessages);
            result.put("reply", reply != null ? reply : "查询完成。");
        } catch (IllegalArgumentException e) {
            result.put("reply", "查询失败：" + e.getMessage());
        } catch (Exception e) {
            result.put("sql", sql != null ? sql : "");
            result.put("reply", "查询失败：AI 服务或数据库暂时不可用，请稍后重试");
        }
        return result;
    }

    /**
     * 清理 AI 生成的 SQL（去除 markdown 代码块、首尾空白、结尾分号）
     */
    private String cleanSql(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new IllegalArgumentException("未能生成有效的SQL");
        }
        String sql = raw.trim();
        // 去除 ```sql ... ``` 代码块标记
        if (sql.startsWith("```")) {
            int first = sql.indexOf('\n');
            int last = sql.lastIndexOf("```");
            if (first >= 0 && last > first) {
                sql = sql.substring(first + 1, last).trim();
            }
        }
        while (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1).trim();
        }
        return sql;
    }

    /**
     * SQL 安全校验：必须 SELECT 开头、禁止写操作关键字/多语句/注释、表白名单
     */
    void validateSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL 不能为空");
        }
        String s = sql.trim();
        if (!s.toLowerCase(Locale.ROOT).startsWith("select")) {
            throw new IllegalArgumentException("仅支持 SELECT 查询");
        }
        if (s.contains(";")) {
            throw new IllegalArgumentException("不支持多语句执行");
        }
        if (s.contains("--") || s.contains("/*") || s.contains("*/") || s.contains("#")) {
            throw new IllegalArgumentException("SQL 中不允许包含注释");
        }
        Matcher forbidden = FORBIDDEN_PATTERN.matcher(s);
        if (forbidden.find()) {
            throw new IllegalArgumentException("SQL 中包含不允许的关键字：" + forbidden.group().toUpperCase());
        }
        // 表名白名单校验
        Matcher tables = TABLE_PATTERN.matcher(s);
        while (tables.find()) {
            String table = tables.group(2).toLowerCase(Locale.ROOT);
            if (!ALLOWED_TABLES.contains(table)) {
                throw new IllegalArgumentException("不允许查询表：" + table);
            }
        }
    }

    /**
     * 执行查询：限制最多返回 50 行
     */
    private List<Map<String, Object>> executeQuery(String sql, List<String> columns) {
        List<Map<String, Object>> rows = new ArrayList<>();
        jdbcTemplate.query(con -> {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setMaxRows(50);
            ps.setQueryTimeout(10);
            return ps;
        }, rs -> {
            int columnCount = rs.getMetaData().getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                columns.add(rs.getMetaData().getColumnLabel(i));
            }
            ColumnMapRowMapper rowMapper = new ColumnMapRowMapper();
            while (rs.next() && rows.size() < 50) {
                rows.add(rowMapper.mapRow(rs, rows.size()));
            }
            return null;
        });
        return rows;
    }

    /**
     * 空值兜底显示
     */
    private String nvl(Object value) {
        return value != null ? String.valueOf(value) : "0";
    }

    private Map<String, String> system(String content) {
        return message("system", content);
    }

    private Map<String, String> user(String content) {
        return message("user", content);
    }

    private Map<String, String> message(String role, String content) {
        Map<String, String> m = new HashMap<>();
        m.put("role", role);
        m.put("content", content);
        return m;
    }
}
