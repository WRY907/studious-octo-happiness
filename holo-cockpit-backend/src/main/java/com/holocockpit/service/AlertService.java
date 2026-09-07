package com.holocockpit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.holocockpit.entity.AlertRecord;
import com.holocockpit.entity.CitySales;
import com.holocockpit.entity.RegionSales;
import com.holocockpit.mapper.AlertRecordMapper;
import com.holocockpit.mapper.CitySalesMapper;
import com.holocockpit.mapper.RegionSalesMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能预警服务：
 * 对比最新两天的省份/城市销售额，跌幅>25% 生成 SALES_DROP，涨幅>40% 生成 SALES_SURGE，
 * 并调用 Ark 生成 AI 应对建议（失败时使用模板）
 */
@Service
public class AlertService {

    /**
     * 下跌预警阈值：25%
     */
    private static final BigDecimal DROP_THRESHOLD = new BigDecimal("25");

    /**
     * 上涨预警阈值：40%
     */
    private static final BigDecimal SURGE_THRESHOLD = new BigDecimal("40");

    @Resource
    private RegionSalesMapper regionSalesMapper;

    @Resource
    private CitySalesMapper citySalesMapper;

    @Resource
    private AlertRecordMapper alertRecordMapper;

    @Resource
    private ArkClient arkClient;

    @Resource
    private JdbcTemplate jdbcTemplate;

    /**
     * 执行预警检测，返回新生成的预警数量
     */
    public int checkAlerts() {
        int count = 0;
        count += checkRegionAlerts();
        count += checkCityAlerts();
        return count;
    }

    /**
     * 省份维度预警：跌>25% → SALES_DROP(level2)；涨>40% → SALES_SURGE(level1)
     */
    private int checkRegionAlerts() {
        LocalDate latest = maxDate("region_sales");
        LocalDate prev = prevDate("region_sales", latest);
        if (latest == null || prev == null) {
            return 0;
        }
        Map<String, BigDecimal> prevMap = new HashMap<>();
        for (RegionSales r : regionSalesMapper.selectList(
                new LambdaQueryWrapper<RegionSales>().eq(RegionSales::getStatDate, prev))) {
            prevMap.put(r.getProvince(), r.getSales());
        }
        int count = 0;
        for (RegionSales r : regionSalesMapper.selectList(
                new LambdaQueryWrapper<RegionSales>().eq(RegionSales::getStatDate, latest))) {
            BigDecimal prevSales = prevMap.get(r.getProvince());
            if (prevSales == null || prevSales.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal cur = r.getSales() != null ? r.getSales() : BigDecimal.ZERO;
            BigDecimal changePct = cur.subtract(prevSales)
                    .multiply(new BigDecimal("100"))
                    .divide(prevSales, 2, RoundingMode.HALF_UP);
            if (changePct.compareTo(DROP_THRESHOLD.negate()) < 0) {
                count += createAlert("SALES_DROP", 2, r.getProvince(), "省份", prev, latest, prevSales, cur, changePct);
            } else if (changePct.compareTo(SURGE_THRESHOLD) > 0) {
                count += createAlert("SALES_SURGE", 1, r.getProvince(), "省份", prev, latest, prevSales, cur, changePct);
            }
        }
        return count;
    }

    /**
     * 城市维度预警：跌>25% → SALES_DROP(level1)；涨>40% → SALES_SURGE(level1)
     */
    private int checkCityAlerts() {
        LocalDate latest = maxDate("city_sales");
        LocalDate prev = prevDate("city_sales", latest);
        if (latest == null || prev == null) {
            return 0;
        }
        Map<String, BigDecimal> prevMap = new HashMap<>();
        for (CitySales c : citySalesMapper.selectList(
                new LambdaQueryWrapper<CitySales>().eq(CitySales::getStatDate, prev))) {
            prevMap.put(c.getCity(), c.getSales());
        }
        int count = 0;
        for (CitySales c : citySalesMapper.selectList(
                new LambdaQueryWrapper<CitySales>().eq(CitySales::getStatDate, latest))) {
            BigDecimal prevSales = prevMap.get(c.getCity());
            if (prevSales == null || prevSales.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal cur = c.getSales() != null ? c.getSales() : BigDecimal.ZERO;
            BigDecimal changePct = cur.subtract(prevSales)
                    .multiply(new BigDecimal("100"))
                    .divide(prevSales, 2, RoundingMode.HALF_UP);
            if (changePct.compareTo(DROP_THRESHOLD.negate()) < 0) {
                count += createAlert("SALES_DROP", 1, c.getCity(), "城市", prev, latest, prevSales, cur, changePct);
            } else if (changePct.compareTo(SURGE_THRESHOLD) > 0) {
                count += createAlert("SALES_SURGE", 1, c.getCity(), "城市", prev, latest, prevSales, cur, changePct);
            }
        }
        return count;
    }

    /**
     * 创建并保存一条预警记录（同日同标题去重），返回 1 已创建 / 0 跳过
     */
    private int createAlert(String alertType, int level, String name, String dim,
                            LocalDate prevDate, LocalDate latestDate,
                            BigDecimal prevSales, BigDecimal curSales, BigDecimal changePct) {
        String absPct = changePct.abs().toPlainString();
        String title = name + "销售额环比" + (changePct.compareTo(BigDecimal.ZERO) > 0 ? "上涨" : "下降") + absPct + "%（" + latestDate + "）";
        // 当天已生成相同预警则跳过
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        Long exists = alertRecordMapper.selectCount(new LambdaQueryWrapper<AlertRecord>()
                .eq(AlertRecord::getTitle, title)
                .ge(AlertRecord::getCreateTime, todayStart));
        if (exists != null && exists > 0) {
            return 0;
        }

        String content = dim + "「" + name + "」销售额从 " + prevDate + " 的 " + prevSales.toPlainString()
                + " 元变为 " + latestDate + " 的 " + curSales.toPlainString()
                + " 元，环比" + (changePct.compareTo(BigDecimal.ZERO) > 0 ? "上涨" : "下降") + absPct + "%，"
                + (changePct.compareTo(BigDecimal.ZERO) > 0 ? "超过40%激增预警线" : "超过25%下滑预警线") + "。";

        AlertRecord alert = new AlertRecord();
        alert.setAlertType(alertType);
        alert.setTitle(title);
        alert.setContent(content);
        alert.setLevel(level);
        alert.setAiAdvice(generateAdvice(name, dim, content, changePct.compareTo(BigDecimal.ZERO) > 0));
        alertRecordMapper.insert(alert);
        return 1;
    }

    /**
     * 调用 Ark 生成 AI 应对建议，失败时降级模板
     */
    private String generateAdvice(String name, String dim, String content, boolean surge) {
        try {
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> sys = new HashMap<>();
            sys.put("role", "system");
            sys.put("content", "你是华为手机数据驾驶舱AI分析师，请用简洁专业的中文回答。");
            Map<String, String> user = new HashMap<>();
            user.put("role", "user");
            user.put("content", "华为手机销售数据预警：" + content
                    + " 请给出1-2条简短的" + dim + "级应对建议（80字以内，直接输出建议内容）。");
            messages.add(sys);
            messages.add(user);
            String advice = arkClient.chat(messages);
            if (advice != null && !advice.trim().isEmpty()) {
                return advice.trim();
            }
        } catch (Exception ignore) {
            // Ark 不可用，使用模板
        }
        return surge
                ? "建议关注" + name + "市场需求激增原因，及时补货并加大营销投放，承接流量红利。"
                : "建议核查" + name + "销售下滑原因（库存/渠道/竞品），必要时调整促销策略并关注库存水位。";
    }

    /**
     * 查询最新预警（最近20条）
     */
    public List<AlertRecord> getAlerts() {
        return alertRecordMapper.selectList(
                new LambdaQueryWrapper<AlertRecord>()
                        .orderByDesc(AlertRecord::getCreateTime)
                        .last("LIMIT 20"));
    }

    /**
     * 指定表的最新统计日期
     */
    private LocalDate maxDate(String table) {
        return jdbcTemplate.queryForObject("SELECT MAX(stat_date) FROM " + table, LocalDate.class);
    }

    /**
     * 指定表早于 before 的最大统计日期
     */
    private LocalDate prevDate(String table, LocalDate before) {
        return jdbcTemplate.queryForObject(
                "SELECT MAX(stat_date) FROM " + table + " WHERE stat_date < ?", LocalDate.class, before);
    }
}
