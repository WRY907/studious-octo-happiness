package com.holocockpit.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.holocockpit.common.Result;
import com.holocockpit.entity.AlertRecord;
import com.holocockpit.entity.HotProduct;
import com.holocockpit.entity.OverviewStats;
import com.holocockpit.entity.PhoneModel;
import com.holocockpit.entity.RealtimeOrder;
import com.holocockpit.entity.SalesTrend;
import com.holocockpit.entity.SysUser;
import com.holocockpit.entity.TrafficSource;
import com.holocockpit.mapper.AlertRecordMapper;
import com.holocockpit.mapper.HotProductMapper;
import com.holocockpit.mapper.PhoneModelMapper;
import com.holocockpit.mapper.RealtimeOrderMapper;
import com.holocockpit.mapper.SalesTrendMapper;
import com.holocockpit.mapper.SysUserMapper;
import com.holocockpit.mapper.TrafficSourceMapper;
import com.holocockpit.service.CockpitService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端接口（JWT 保护，路径 /admin/*）
 * ADMIN-管理员（全量权限）；MERCHANT-商户（订单只读脱敏、可改订单状态）
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    @Resource
    private PhoneModelMapper phoneModelMapper;

    @Resource
    private RealtimeOrderMapper realtimeOrderMapper;

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private AlertRecordMapper alertRecordMapper;

    @Resource
    private SalesTrendMapper salesTrendMapper;

    @Resource
    private HotProductMapper hotProductMapper;

    @Resource
    private TrafficSourceMapper trafficSourceMapper;

    @Resource
    private CockpitService cockpitService;

    // ==================== 机型管理（增删改仅ADMIN） ====================

    /**
     * 机型分页列表：page 页码（默认1）、size 每页（默认10）、keyword 机型/系列关键词
     */
    @GetMapping("/phones")
    public Result<Map<String, Object>> phones(@RequestParam(defaultValue = "1") long page,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<PhoneModel> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(PhoneModel::getModelName, kw).or().like(PhoneModel::getSeries, kw));
        }
        wrapper.orderByDesc(PhoneModel::getUpdateTime).orderByDesc(PhoneModel::getId);
        Page<PhoneModel> result = phoneModelMapper.selectPage(new Page<>(page, size), wrapper);
        return Result.success(pageOf(result.getRecords(), result.getTotal()));
    }

    /**
     * 新增机型（仅ADMIN）
     */
    @PostMapping("/phones")
    public Result<PhoneModel> addPhone(@RequestBody PhoneModel phone, HttpServletRequest request) {
        if (!isAdmin(request)) {
            return Result.error(403, "无权限，仅管理员可操作");
        }
        if (phone.getModelName() == null || phone.getModelName().trim().isEmpty()) {
            return Result.error(400, "机型名称不能为空");
        }
        phone.setId(null);
        phoneModelMapper.insert(phone);
        return Result.success(phone);
    }

    /**
     * 修改机型（仅ADMIN）
     */
    @PutMapping("/phones")
    public Result<PhoneModel> updatePhone(@RequestBody PhoneModel phone, HttpServletRequest request) {
        if (!isAdmin(request)) {
            return Result.error(403, "无权限，仅管理员可操作");
        }
        if (phone.getId() == null) {
            return Result.error(400, "机型id不能为空");
        }
        phoneModelMapper.updateById(phone);
        return Result.success(phone);
    }

    /**
     * 删除机型（仅ADMIN）
     */
    @DeleteMapping("/phones/{id}")
    public Result<Void> deletePhone(@PathVariable Long id, HttpServletRequest request) {
        if (!isAdmin(request)) {
            return Result.error(403, "无权限，仅管理员可操作");
        }
        phoneModelMapper.deleteById(id);
        return Result.success("删除成功", null);
    }

    // ==================== 订单管理 ====================

    /**
     * 订单分页列表（倒序）；MERCHANT 角色金额脱敏（amount=null，masked=true）
     */
    @GetMapping("/orders")
    public Result<Map<String, Object>> orders(@RequestParam(defaultValue = "1") long page,
                                              @RequestParam(defaultValue = "10") long size,
                                              HttpServletRequest request) {
        Page<RealtimeOrder> result = realtimeOrderMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<RealtimeOrder>().orderByDesc(RealtimeOrder::getCreateTime));
        List<RealtimeOrder> records = result.getRecords();
        if (isMerchant(request)) {
            // 商户视角：金额脱敏
            for (RealtimeOrder o : records) {
                o.setAmount(null);
                o.setMasked(true);
            }
        }
        return Result.success(pageOf(records, result.getTotal()));
    }

    /**
     * 修改订单状态（ADMIN / MERCHANT 均可）
     */
    @PutMapping("/orders/{id}/status")
    public Result<Void> updateOrderStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer status = body.get("status");
        if (status == null || status < 1 || status > 4) {
            return Result.error(400, "订单状态只能是1-4");
        }
        RealtimeOrder order = realtimeOrderMapper.selectById(id);
        if (order == null) {
            return Result.error(400, "订单不存在");
        }
        RealtimeOrder update = new RealtimeOrder();
        update.setId(id);
        update.setStatus(status);
        realtimeOrderMapper.updateById(update);
        return Result.success("状态已更新", null);
    }

    /**
     * 删除订单（仅ADMIN）
     */
    @DeleteMapping("/orders/{id}")
    public Result<Void> deleteOrder(@PathVariable Long id, HttpServletRequest request) {
        if (!isAdmin(request)) {
            return Result.error(403, "无权限，仅管理员可操作");
        }
        realtimeOrderMapper.deleteById(id);
        return Result.success("删除成功", null);
    }

    // ==================== 用户与统计 ====================

    /**
     * 用户分页列表（仅ADMIN，不返回密码）
     */
    @GetMapping("/users")
    public Result<Map<String, Object>> users(@RequestParam(defaultValue = "1") long page,
                                              @RequestParam(defaultValue = "10") long size,
                                              HttpServletRequest request) {
        if (!isAdmin(request)) {
            return Result.error(403, "无权限，仅管理员可查看");
        }
        Page<SysUser> result = sysUserMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<SysUser>().orderByAsc(SysUser::getId));
        List<SysUser> users = result.getRecords();
        for (SysUser u : users) {
            u.setPassword(null);
        }
        return Result.success(pageOf(users, result.getTotal()));
    }

    /**
     * 销售趋势分析数据：按角色差异化
     * ADMIN 返回全量（含销售额/客单价）；MERCHANT 隐藏金额字段（仅订单/访问）
     */
    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> trend(HttpServletRequest request) {
        List<SalesTrend> list = salesTrendMapper.selectList(
                new LambdaQueryWrapper<SalesTrend>().orderByAsc(SalesTrend::getStatDate));
        boolean admin = isAdmin(request);
        List<Map<String, Object>> data = new ArrayList<>();
        for (SalesTrend t : list) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("statDate", t.getStatDate() == null ? null : t.getStatDate().toString());
            row.put("orders", t.getOrders());
            row.put("visits", t.getVisits());
            if (admin) {
                // 仅管理员可见金额类字段
                row.put("sales", t.getSales());
                row.put("avgOrderValue", t.getAvgOrderValue());
            }
            data.add(row);
        }
        return Result.success(data);
    }

    /**
     * 热销机型分析数据：按角色差异化
     * ADMIN 返回全量（含销售额/单价）；MERCHANT 隐藏销售额（仅销量/增长率/公开信息）
     */
    @GetMapping("/hotmodels")
    public Result<List<Map<String, Object>>> hotModels(HttpServletRequest request) {
        List<HotProduct> hotList = hotProductMapper.selectList(
                new LambdaQueryWrapper<HotProduct>().orderByAsc(HotProduct::getRankNo));
        // 机型档案（价格/评分/图片）按名称索引
        Map<String, PhoneModel> phoneMap = new LinkedHashMap<>();
        for (PhoneModel p : phoneModelMapper.selectList(null)) {
            phoneMap.put(p.getModelName(), p);
        }
        boolean admin = isAdmin(request);
        List<Map<String, Object>> data = new ArrayList<>();
        for (HotProduct h : hotList) {
            Map<String, Object> row = new LinkedHashMap<>();
            PhoneModel p = phoneMap.get(h.getModelName());
            row.put("rankNo", h.getRankNo());
            row.put("modelName", h.getModelName());
            row.put("salesCount", h.getSalesCount());
            row.put("growth", h.getGrowth());
            // 公开信息：系列/评分/图片/指导价（商城可见）
            if (p != null) {
                row.put("series", p.getSeries());
                row.put("rating", p.getRating());
                row.put("imageUrl", p.getImageUrl());
                row.put("price", p.getPrice());
            }
            if (admin) {
                // 仅管理员可见实际销售额
                row.put("salesAmount", h.getSalesAmount());
            }
            data.add(row);
        }
        return Result.success(data);
    }
    /**
     * 流量来源分析数据：按角色差异化
     * 公开：渠道清单/分类汇总/24小时流量脉搏；ADMIN 附加转化漏斗与渠道价值指标（金额类）
     */
    @GetMapping("/traffic")
    public Result<Map<String, Object>> traffic(HttpServletRequest request) {
        List<TrafficSource> list = trafficSourceMapper.selectList(
                new LambdaQueryWrapper<TrafficSource>().orderByDesc(TrafficSource::getVisits));
        boolean admin = isAdmin(request);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sources", list);

        // ===== 分类汇总（公开） =====
        Map<String, long[]> catVisits = new LinkedHashMap<>(); // [访问量, 渠道数]
        Map<String, Double> catRatio = new LinkedHashMap<>();
        for (TrafficSource t : list) {
            long[] arr = catVisits.computeIfAbsent(t.getCategory(), k -> new long[2]);
            arr[0] += t.getVisits() == null ? 0 : t.getVisits();
            arr[1] += 1;
            catRatio.merge(t.getCategory(), t.getRatio() == null ? 0 : t.getRatio().doubleValue(), Double::sum);
        }
        List<Map<String, Object>> summary = new ArrayList<>();
        for (Map.Entry<String, long[]> e : catVisits.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("category", e.getKey());
            row.put("visits", e.getValue()[0]);
            row.put("channels", e.getValue()[1]);
            row.put("share", Math.round(catRatio.getOrDefault(e.getKey(), 0.0) * 10) / 10.0);
            summary.add(row);
        }
        data.put("categorySummary", summary);

        // ===== 24 小时流量脉搏（公开，按总访问量加权推导） =====
        long totalVisits = 0;
        for (TrafficSource t : list) {
            totalVisits += t.getVisits() == null ? 0 : t.getVisits();
        }
        double[] weights = new double[24];
        double wSum = 0;
        for (int h = 0; h < 24; h++) {
            weights[h] = hourlyShape(h);
            wSum += weights[h];
        }
        List<Map<String, Object>> pulse = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("hour", h);
            row.put("visits", Math.round(totalVisits * weights[h] / wSum));
            pulse.add(row);
        }
        data.put("hourlyPulse", pulse);

        if (admin) {
            // ===== 转化漏斗（仅管理员） =====
            List<Map<String, Object>> funnel = new ArrayList<>();
            String[] stages = {"访问进站", "商品详情页", "加入购物车", "提交订单", "完成支付"};
            double[] rates = {1.0, 0.58, 0.23, 0.092, 0.076};
            for (int i = 0; i < stages.length; i++) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("stage", stages[i]);
                row.put("count", Math.round(totalVisits * rates[i]));
                row.put("rate", Math.round(rates[i] * 1000) / 10.0);
                funnel.add(row);
            }
            data.put("funnel", funnel);

            // ===== 渠道价值指标（仅管理员：转化率/客单价/获客成本/GMV/ROI） =====
            Map<String, double[]> coef = new LinkedHashMap<>();
            coef.put("搜索引擎", new double[]{0.028, 5899, 12});
            coef.put("社交媒体", new double[]{0.019, 4299, 35});
            coef.put("电商平台广告", new double[]{0.036, 4699, 28});
            coef.put("直接访问", new double[]{0.032, 5399, 0});
            coef.put("线下引流", new double[]{0.022, 6299, 45});
            List<Map<String, Object>> metrics = new ArrayList<>();
            for (Map.Entry<String, long[]> e : catVisits.entrySet()) {
                double[] k = coef.getOrDefault(e.getKey(), new double[]{0.02, 4999, 30});
                double gmv = e.getValue()[0] * k[0] * k[1];
                double cost = e.getValue()[0] * k[2];
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("category", e.getKey());
                row.put("visits", e.getValue()[0]);
                row.put("conversionRate", Math.round(k[0] * 1000) / 10.0);
                row.put("aov", k[1]);
                row.put("cac", k[2]);
                row.put("gmv", Math.round(gmv));
                row.put("cost", Math.round(cost));
                row.put("roi", cost > 0 ? Math.round(gmv / cost * 10) / 10.0 : null);
                metrics.add(row);
            }
            data.put("channelMetrics", metrics);
        }
        return Result.success(data);
    }

    /** 24 小时分布形态：凌晨低谷、白天曲线、午间小峰、晚间 20-21 点主峰（调用方动态归一化） */
    private double hourlyShape(int h) {
        double day = 1.2 * Math.max(0, Math.sin((h - 7) / 13.0 * Math.PI));
        double evening = 1.9 * Math.exp(-Math.pow(h - 20.5, 2) / 6.0);
        double lunch = 0.3 * Math.exp(-Math.pow(h - 13, 2) / 4.0);
        return 0.8 + day + evening + lunch;
    }
    /**
     * 管理端统计：ADMIN 返回全量（含金额），MERCHANT 不含金额
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats(HttpServletRequest request) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("phoneCount", phoneModelMapper.selectCount(null));
        data.put("orderCount", realtimeOrderMapper.selectCount(null));
        data.put("userCount", sysUserMapper.selectCount(null));
        data.put("alertCount", alertRecordMapper.selectCount(null));
        data.put("todayOrders", cockpitService.getOverview().getTodayOrders());
        if (isAdmin(request)) {
            // 仅管理员可见金额类指标
            OverviewStats overview = cockpitService.getOverview();
            data.put("totalSales", overview.getTotalSales());
            data.put("todaySales", overview.getTodaySales());
            data.put("todayNewUsers", overview.getTodayNewUsers());
            data.put("conversionRate", overview.getConversionRate());
        }
        return Result.success(data);
    }

    // ==================== 辅助 ====================

    private boolean isAdmin(HttpServletRequest request) {
        return "ADMIN".equals(request.getAttribute("role"));
    }

    private boolean isMerchant(HttpServletRequest request) {
        return "MERCHANT".equals(request.getAttribute("role"));
    }

    /**
     * 分页结果统一为 {records, total}（与前端契约一致）
     */
    private <T> Map<String, Object> pageOf(List<T> records, long total) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", records != null ? records : new ArrayList<T>());
        data.put("total", total);
        return data;
    }
}
