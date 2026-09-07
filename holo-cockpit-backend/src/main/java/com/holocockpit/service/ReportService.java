package com.holocockpit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.holocockpit.entity.CitySales;
import com.holocockpit.entity.HotProduct;
import com.holocockpit.entity.OverviewStats;
import com.holocockpit.entity.RegionSales;
import com.holocockpit.entity.SalesTrend;
import com.holocockpit.mapper.CitySalesMapper;
import com.holocockpit.mapper.HotProductMapper;
import com.holocockpit.mapper.RegionSalesMapper;
import com.holocockpit.mapper.SalesTrendMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据报告服务：汇总30天+今日+TOP数据 → AI 摘要建议 → 深色科技风 HTML 报告
 */
@Service
public class ReportService {

    @Resource
    private CockpitService cockpitService;

    @Resource
    private SalesTrendMapper salesTrendMapper;

    @Resource
    private RegionSalesMapper regionSalesMapper;

    @Resource
    private CitySalesMapper citySalesMapper;

    @Resource
    private HotProductMapper hotProductMapper;

    @Resource
    private ArkClient arkClient;

    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * 生成完整 HTML 报告字符串
     */
    public String generate() {
        // ===== 汇总数据 =====
        OverviewStats overview = cockpitService.getOverview();
        LocalDate latestDate = cockpitService.maxDate("region_sales");
        List<SalesTrend> trends = cockpitService.getSalesTrend30();

        BigDecimal total30Sales = BigDecimal.ZERO;
        long total30Orders = 0;
        long total30Visits = 0;
        for (SalesTrend t : trends) {
            if (t.getSales() != null) {
                total30Sales = total30Sales.add(t.getSales());
            }
            if (t.getOrders() != null) {
                total30Orders += t.getOrders();
            }
            if (t.getVisits() != null) {
                total30Visits += t.getVisits();
            }
        }

        List<RegionSales> topRegions = regionSalesMapper.selectList(
                new LambdaQueryWrapper<RegionSales>()
                        .eq(RegionSales::getStatDate, latestDate)
                        .orderByDesc(RegionSales::getSales)
                        .last("LIMIT 3"));
        List<CitySales> topCities = citySalesMapper.selectList(
                new LambdaQueryWrapper<CitySales>()
                        .eq(CitySales::getStatDate, latestDate)
                        .orderByDesc(CitySales::getSales)
                        .last("LIMIT 3"));
        List<HotProduct> topProducts = hotProductMapper.selectList(
                new LambdaQueryWrapper<HotProduct>()
                        .orderByAsc(HotProduct::getRankNo)
                        .last("LIMIT 3"));

        // ===== AI 摘要建议（失败降级模板） =====
        String summary = generateSummary(overview, latestDate, trends, total30Sales, total30Orders, topRegions, topCities, topProducts);

        // ===== 拼装 HTML =====
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html lang=\"zh-CN\">\n<head>\n<meta charset=\"UTF-8\">\n")
            .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
            .append("<title>华为手机全息数据驾驶舱 · 数据报告</title>\n<style>\n")
            .append("body{background:#070f1e;color:#d6e6ff;font-family:'Microsoft YaHei','PingFang SC',sans-serif;margin:0;padding:48px 24px;}\n")
            .append(".report{max-width:1080px;margin:0 auto;}\n")
            .append("h1{font-size:28px;color:#4db8ff;letter-spacing:2px;margin:0 0 6px;}\n")
            .append(".sub{color:#5f7fae;font-size:13px;margin-bottom:32px;}\n")
            .append(".cards{display:flex;flex-wrap:wrap;gap:16px;margin-bottom:32px;}\n")
            .append(".card{flex:1;min-width:180px;background:linear-gradient(135deg,#0d1e38,#0a1628);border:1px solid #1e3a5f;border-radius:10px;padding:18px 22px;box-shadow:0 0 18px rgba(30,90,160,.15);}\n")
            .append(".card .label{color:#7fa8d9;font-size:13px;margin-bottom:8px;}\n")
            .append(".card .value{color:#4db8ff;font-size:24px;font-weight:bold;}\n")
            .append("h2{font-size:18px;color:#9fd0ff;border-left:3px solid #4db8ff;padding-left:10px;margin:32px 0 14px;}\n")
            .append("table{width:100%;border-collapse:collapse;font-size:13px;}\n")
            .append("th{background:#0d1e38;color:#9fd0ff;padding:10px;text-align:left;border-bottom:1px solid #1e3a5f;}\n")
            .append("td{padding:9px 10px;border-bottom:1px solid #142946;color:#c4d8f5;}\n")
            .append("tr:hover td{background:#0f2240;}\n")
            .append(".rank{display:flex;flex-wrap:wrap;gap:16px;}\n")
            .append(".rank .card{min-width:260px;}\n")
            .append(".rank .item{padding:6px 0;color:#c4d8f5;font-size:14px;border-bottom:1px dashed #1e3a5f;}\n")
            .append(".rank .item:last-child{border-bottom:none;}\n")
            .append(".rank .item b{color:#4db8ff;}\n")
            .append(".ai{background:linear-gradient(135deg,#0d1e38,#0a1628);border:1px solid #1e3a5f;border-left:3px solid #4db8ff;border-radius:10px;padding:20px 24px;color:#c4d8f5;line-height:1.9;font-size:14px;white-space:pre-wrap;}\n")
            .append(".footer{margin-top:40px;color:#3d5a80;font-size:12px;text-align:center;}\n")
            .append(".num{color:#4db8ff;}\n")
            .append("</style>\n</head>\n<body>\n<div class=\"report\">\n");

        html.append("<h1>华为手机全息数据驾驶舱 · 数据报告</h1>\n")
            .append("<div class=\"sub\">报告生成时间：").append(LocalDateTime.now().format(DATETIME_FMT))
            .append(" ｜ 数据日期：").append(latestDate != null ? latestDate.toString() : "无")
            .append(" ｜ 统计范围：近30天</div>\n");

        // KPI 卡片
        html.append("<div class=\"cards\">");
        card(html, "近30天销售额（元）", fmt(total30Sales));
        card(html, "近30天订单数（单）", String.valueOf(total30Orders));
        card(html, "近30天访问量", String.valueOf(total30Visits));
        card(html, "今日销售额（元）", nvl(overview.getTodaySales()));
        card(html, "今日订单数（单）", nvl(overview.getTodayOrders()));
        card(html, "转化率", nvl(overview.getConversionRate()) + "%");
        html.append("</div>\n");

        // TOP3 榜单
        html.append("<h2>TOP3 榜单</h2>\n<div class=\"rank\">");
        html.append("<div class=\"card\"><div class=\"label\">销售额 TOP3 省份</div>");
        for (RegionSales r : topRegions) {
            html.append("<div class=\"item\">").append(esc(r.getProvince()))
                .append("：<b>").append(fmt(r.getSales())).append("</b> 元</div>");
        }
        html.append("</div>");
        html.append("<div class=\"card\"><div class=\"label\">销售额 TOP3 城市</div>");
        for (CitySales c : topCities) {
            html.append("<div class=\"item\">").append(esc(c.getCity()))
                .append("：<b>").append(fmt(c.getSales())).append("</b> 元</div>");
        }
        html.append("</div>");
        html.append("<div class=\"card\"><div class=\"label\">热销 TOP3 机型</div>");
        for (HotProduct p : topProducts) {
            html.append("<div class=\"item\">").append(esc(p.getModelName()))
                .append("：销量 <b>").append(nvl(p.getSalesCount())).append("</b> 台，销售额 <b>")
                .append(fmt(p.getSalesAmount())).append("</b> 元</div>");
        }
        html.append("</div></div>\n");

        // 近30天趋势表
        html.append("<h2>近30天销售趋势</h2>\n<table>\n<tr><th>日期</th><th>销售额（元）</th><th>订单数</th><th>访问量</th><th>客单价（元）</th></tr>\n");
        for (SalesTrend t : trends) {
            html.append("<tr><td>").append(t.getStatDate() != null ? t.getStatDate().toString() : "")
                .append("</td><td class=\"num\">").append(fmt(t.getSales()))
                .append("</td><td>").append(nvl(t.getOrders()))
                .append("</td><td>").append(nvl(t.getVisits()))
                .append("</td><td class=\"num\">").append(fmt(t.getAvgOrderValue()))
                .append("</td></tr>\n");
        }
        html.append("</table>\n");

        // AI 分析建议
        html.append("<h2>AI 分析建议</h2>\n<div class=\"ai\">").append(esc(summary)).append("</div>\n");

        html.append("<div class=\"footer\">华为手机全息数据驾驶舱 · AI 智能生成报告</div>\n</div>\n</body>\n</html>");
        return html.toString();
    }

    /**
     * 调用 Ark 生成摘要与建议，失败时使用本地模板
     */
    private String generateSummary(OverviewStats overview, LocalDate latestDate, List<SalesTrend> trends,
                                   BigDecimal total30Sales, long total30Orders,
                                   List<RegionSales> topRegions, List<CitySales> topCities, List<HotProduct> topProducts) {
        StringBuilder data = new StringBuilder();
        data.append("数据日期：").append(latestDate).append("\n");
        data.append("近30天累计销售额：").append(fmt(total30Sales)).append("元，累计订单：").append(total30Orders).append("单；\n");
        data.append("今日销售额：").append(nvl(overview.getTodaySales())).append("元，今日订单：").append(nvl(overview.getTodayOrders()))
            .append("单，转化率：").append(nvl(overview.getConversionRate())).append("%；\n");
        if (!trends.isEmpty()) {
            SalesTrend last = trends.get(trends.size() - 1);
            SalesTrend first = trends.get(0);
            data.append("30天前日销售额：").append(fmt(first.getSales())).append("元，最新日销售额：").append(fmt(last.getSales())).append("元；\n");
        }
        data.append("销售额TOP3省份：");
        for (RegionSales r : topRegions) {
            data.append(r.getProvince()).append("(").append(fmt(r.getSales())).append("元) ");
        }
        data.append("\n销售额TOP3城市：");
        for (CitySales c : topCities) {
            data.append(c.getCity()).append("(").append(fmt(c.getSales())).append("元) ");
        }
        data.append("\n热销TOP3机型：");
        for (HotProduct p : topProducts) {
            data.append(p.getModelName()).append("(销量").append(nvl(p.getSalesCount())).append("台) ");
        }

        try {
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(message("system", "你是华为手机数据驾驶舱AI分析师，请用简洁专业的中文回答。"));
            messages.add(message("user", "以下是华为手机销售数据摘要：\n" + data
                    + "\n请生成一段150字以内的中文经营总结，并给出3条可执行建议（分条列出）。"));
            String reply = arkClient.chat(messages);
            if (reply != null && !reply.trim().isEmpty()) {
                return reply.trim();
            }
        } catch (Exception ignore) {
            // Ark 不可用，使用模板
        }
        return "近30天累计销售额 " + fmt(total30Sales) + " 元，累计订单 " + total30Orders + " 单。\n"
                + "建议：\n1. 重点关注 TOP 省份/城市的市场表现，加大资源倾斜；\n"
                + "2. 对热销机型保障库存与供货，避免断货影响转化；\n"
                + "3. 针对销售下滑区域及时核查渠道与库存，调整促销策略。";
    }

    private void card(StringBuilder html, String label, String value) {
        html.append("<div class=\"card\"><div class=\"label\">").append(label)
            .append("</div><div class=\"value\">").append(value).append("</div></div>");
    }

    /**
     * 金额千分位格式化
     */
    private String fmt(BigDecimal value) {
        return value == null ? "0" : String.format("%,.2f", value);
    }

    private String nvl(Object value) {
        return value != null ? String.valueOf(value) : "0";
    }

    /**
     * HTML 转义
     */
    private String esc(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private Map<String, String> message(String role, String content) {
        Map<String, String> m = new HashMap<>();
        m.put("role", role);
        m.put("content", content);
        return m;
    }
}
