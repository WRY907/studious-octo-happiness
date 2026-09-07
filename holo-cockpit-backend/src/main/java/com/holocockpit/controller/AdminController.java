package com.holocockpit.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import com.holocockpit.entity.UserProfile;
import com.holocockpit.mapper.AlertRecordMapper;
import com.holocockpit.mapper.HotProductMapper;
import com.holocockpit.mapper.PhoneModelMapper;
import com.holocockpit.mapper.RealtimeOrderMapper;
import com.holocockpit.mapper.SalesTrendMapper;
import com.holocockpit.mapper.SysUserMapper;
import com.holocockpit.mapper.TrafficSourceMapper;
import com.holocockpit.mapper.UserProfileMapper;
import com.holocockpit.service.CockpitService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
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
    private UserProfileMapper userProfileMapper;

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

    /**
     * 用户画像分析数据：按角色差异化
     * 公开：性别/年龄分布、年龄×性别交叉、典型画像、分龄活跃时钟、分龄兴趣雷达
     * ADMIN 附加消费能力分层与年龄价值矩阵（金额类）
     */
    @GetMapping("/profile")
    public Result<Map<String, Object>> profile(HttpServletRequest request) {
        List<UserProfile> list = userProfileMapper.selectList(null);
        boolean admin = isAdmin(request);
        Map<String, Object> data = new LinkedHashMap<>();

        List<UserProfile> genders = new ArrayList<>();
        List<UserProfile> ages = new ArrayList<>();
        for (UserProfile u : list) {
            if ("gender".equals(u.getProfileType())) {
                genders.add(u);
            } else if ("age".equals(u.getProfileType())) {
                ages.add(u);
            }
        }
        ages.sort((a, b) -> Integer.compare(ageOrder(a.getProfileName()), ageOrder(b.getProfileName())));
        data.put("genders", genders);
        data.put("ages", ages);

        long total = 0;
        for (UserProfile g : genders) {
            total += g.getUserCount() == null ? 0 : g.getUserCount();
        }

        // ===== 年龄×性别交叉（公开，按年龄递增的男性占比微扰推导） =====
        double[] maleBias = {0.52, 0.54, 0.58, 0.60, 0.62, 0.64};
        List<Map<String, Object>> ageGender = new ArrayList<>();
        for (int i = 0; i < ages.size(); i++) {
            UserProfile a = ages.get(i);
            double malePct = maleBias[Math.min(i, maleBias.length - 1)];
            long users = a.getUserCount() == null ? 0 : a.getUserCount();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("ageRange", a.getProfileName());
            row.put("users", users);
            row.put("maleCount", Math.round(users * malePct));
            row.put("femaleCount", Math.round(users * (1 - malePct)));
            row.put("malePct", Math.round(malePct * 1000) / 10.0);
            row.put("femalePct", Math.round((1 - malePct) * 1000) / 10.0);
            ageGender.add(row);
        }
        data.put("ageGender", ageGender);

        // ===== 典型用户画像（公开，主力人群自动推导） =====
        UserProfile mainAge = ages.stream().max((x, y) -> x.getRatio().compareTo(y.getRatio())).orElse(null);
        UserProfile mainGender = genders.stream().max((x, y) -> x.getRatio().compareTo(y.getRatio())).orElse(null);
        if (mainAge != null && mainGender != null) {
            Map<String, Object> persona = new LinkedHashMap<>();
            persona.put("ageRange", mainAge.getProfileName());
            persona.put("gender", mainGender.getProfileName());
            persona.put("sharePct", mainAge.getRatio());
            persona.put("users", mainAge.getUserCount());
            // 主力人群特征标签（按主力年龄段推导）
            String ar = mainAge.getProfileName();
            List<String> tags = new ArrayList<>();
            if (ar.contains("25-34") || ar.contains("35-44")) {
                tags.add("商务精英"); tags.add("科技尝鲜"); tags.add("影像创作"); tags.add("换机主力");
            } else if (ar.contains("18")) {
                tags.add("潮流先锋"); tags.add("游戏娱乐"); tags.add("社交达人"); tags.add("性价比敏感");
            } else {
                tags.add("稳健务实"); tags.add("健康关注"); tags.add("品牌忠诚"); tags.add("家庭决策");
            }
            persona.put("tags", tags);
            persona.put("activeHours", ar.contains("18") ? "20:00-次日02:00" : "12:00-14:00 / 20:00-23:00");
            persona.put("devicePref", ar.contains("25") || ar.contains("35") ? "Mate / Pura 旗舰系列" : "nova / 畅享系列");
            persona.put("priceBand", ar.contains("25") || ar.contains("35") ? "¥5,000+ 高端价位" : "¥2,000-4,000 主流价位");
            data.put("persona", persona);
        }

        // ===== 分龄活跃时钟（公开：6 段人群 × 24h 活跃权重） =====
        double[][] shapes = {
                {0.2, 0.15, 0.1, 0.05, 0.05, 0.1, 0.3, 0.5, 0.4, 0.3, 0.3, 0.4, 0.6, 0.5, 0.4, 0.5, 0.8, 0.9, 1.0, 1.0, 1.0, 0.9, 0.7, 0.4},   // 18以下：放学后高峰
                {0.4, 0.3, 0.2, 0.1, 0.05, 0.05, 0.15, 0.3, 0.4, 0.3, 0.3, 0.5, 0.6, 0.5, 0.4, 0.5, 0.7, 0.8, 0.9, 1.0, 1.0, 1.0, 0.9, 0.6},   // 18-24：夜猫子
                {0.15, 0.1, 0.05, 0.05, 0.05, 0.15, 0.5, 0.8, 0.7, 0.6, 0.6, 0.8, 1.0, 0.9, 0.6, 0.6, 0.7, 0.8, 0.9, 1.0, 1.0, 0.9, 0.6, 0.3},  // 25-34：午晚双峰
                {0.1, 0.05, 0.05, 0.05, 0.1, 0.3, 0.7, 1.0, 0.8, 0.6, 0.6, 0.7, 0.9, 0.7, 0.5, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0, 0.8, 0.5, 0.2},   // 35-44：早+晚
                {0.05, 0.05, 0.05, 0.05, 0.2, 0.5, 0.9, 1.0, 0.7, 0.5, 0.5, 0.7, 0.9, 0.7, 0.5, 0.4, 0.5, 0.6, 0.7, 0.9, 1.0, 0.7, 0.4, 0.1},  // 45-54：晨型
                {0.05, 0.05, 0.05, 0.1, 0.3, 0.7, 1.0, 0.9, 0.6, 0.5, 0.5, 0.7, 0.9, 0.8, 0.6, 0.5, 0.4, 0.4, 0.5, 0.6, 0.7, 0.5, 0.2, 0.1}    // 55+：清晨+午间
        };
        List<Map<String, Object>> ageHourly = new ArrayList<>();
        for (int i = 0; i < ages.size(); i++) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("ageRange", ages.get(i).getProfileName());
            List<Integer> hours = new ArrayList<>();
            double[] shape = shapes[Math.min(i, shapes.length - 1)];
            for (int h = 0; h < 24; h++) {
                hours.add((int) Math.round(shape[h] * 100));
            }
            row.put("hours", hours);
            ageHourly.add(row);
        }
        data.put("ageHourly", ageHourly);

        // ===== 分龄兴趣雷达（公开：商家运营参考） =====
        String[] dims = {"科技尝鲜", "商务办公", "影像创作", "游戏娱乐", "性价比", "健康运动"};
        double[][] interestShapes = {
                {45, 20, 35, 92, 78, 55},   // 18以下
                {80, 40, 62, 90, 70, 58},   // 18-24
                {88, 78, 80, 60, 52, 65},   // 25-34
                {70, 90, 72, 42, 50, 72},   // 35-44
                {48, 62, 45, 25, 78, 80},   // 45-54
                {30, 25, 22, 12, 85, 88}    // 55+
        };
        List<Map<String, Object>> ageInterests = new ArrayList<>();
        for (int i = 0; i < ages.size(); i++) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("ageRange", ages.get(i).getProfileName());
            List<Integer> scores = new ArrayList<>();
            double[] is = interestShapes[Math.min(i, interestShapes.length - 1)];
            for (double v : is) {
                scores.add((int) v);
            }
            row.put("scores", scores);
            ageInterests.add(row);
        }
        data.put("interestDims", java.util.Arrays.asList(dims));
        data.put("ageInterests", ageInterests);

        if (admin) {
            // ===== 消费能力分层（仅管理员） =====
            String[] tiers = {"钻石用户", "铂金用户", "黄金用户", "白银用户", "普通用户"};
            double[] tierRatio = {0.03, 0.07, 0.15, 0.25, 0.50};
            double[] tierArpu = {15800, 8800, 4600, 2100, 680};
            double[] tierRepurchase = {72, 58, 45, 31, 14};
            List<Map<String, Object>> consumption = new ArrayList<>();
            for (int i = 0; i < tiers.length; i++) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("tier", tiers[i]);
                row.put("users", Math.round(total * tierRatio[i]));
                row.put("ratio", Math.round(tierRatio[i] * 1000) / 10.0);
                row.put("arpu", tierArpu[i]);
                row.put("repurchase", tierRepurchase[i]);
                consumption.add(row);
            }
            data.put("consumptionTiers", consumption);

            // ===== 年龄价值矩阵（仅管理员：客单价/转化/GMV 贡献） =====
            double[] convRate = {3.8, 6.2, 9.6, 8.8, 6.5, 3.9};
            double[] aovArr = {2199, 3299, 5899, 6599, 4999, 2799};
            List<Map<String, Object>> ageValue = new ArrayList<>();
            for (int i = 0; i < ages.size(); i++) {
                long users = ages.get(i).getUserCount() == null ? 0 : ages.get(i).getUserCount();
                double gmv = users * (convRate[Math.min(i, convRate.length - 1)] / 100.0) * aovArr[Math.min(i, aovArr.length - 1)];
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("ageRange", ages.get(i).getProfileName());
                row.put("users", users);
                row.put("conversionRate", convRate[Math.min(i, convRate.length - 1)]);
                row.put("aov", aovArr[Math.min(i, aovArr.length - 1)]);
                row.put("gmv", Math.round(gmv));
                ageValue.add(row);
            }
            data.put("ageValue", ageValue);
        }
        return Result.success(data);
    }
    /** 年龄段语义排序权重：18以下→18-24→25-34→35-44→45-54→55+（其余按首数字兜底） */
    private int ageOrder(String name) {
        if (name == null) return 99;
        if (name.contains("以下") && !name.contains("以上")) return 0;
        if (name.contains("以上")) return 60;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(\\d+)").matcher(name);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return 99;
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

    // ==================== 商户审批 ====================

    /**
     * 审批列表：tab=pending 待审核（按申请时间正序）/ tab=handled 已处理（按审批时间倒序）
     */
    @GetMapping("/audit/list")
    public Result<Map<String, Object>> auditList(@RequestParam(defaultValue = "pending") String tab,
                                                 @RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "10") long size,
                                                 HttpServletRequest request) {
        if (!isAdmin(request)) {
            return Result.error(403, "无权限，仅管理员可审批");
        }
        LambdaQueryWrapper<SysUser> qw = new LambdaQueryWrapper<>();
        if ("handled".equals(tab)) {
            qw.ne(SysUser::getStatus, "PENDING").eq(SysUser::getRole, "MERCHANT")
                    .orderByDesc(SysUser::getAuditTime);
        } else {
            qw.eq(SysUser::getStatus, "PENDING").eq(SysUser::getRole, "MERCHANT")
                    .orderByAsc(SysUser::getCreateTime);
        }
        Page<SysUser> result = sysUserMapper.selectPage(new Page<>(page, size), qw);
        List<SysUser> records = result.getRecords();
        for (SysUser u : records) {
            u.setPassword(null);
        }
        return Result.success(pageOf(records, result.getTotal()));
    }

    /**
     * 审批统计：待审/通过/拒绝/停用 数量（侧边栏角标与统计卡共用）
     */
    @GetMapping("/audit/stats")
    public Result<Map<String, Object>> auditStats(HttpServletRequest request) {
        if (!isAdmin(request)) {
            return Result.error(403, "无权限，仅管理员可查看");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("pendingCount", countMerchantByStatus("PENDING"));
        data.put("approvedCount", countMerchantByStatus("APPROVED"));
        data.put("rejectedCount", countMerchantByStatus("REJECTED"));
        data.put("disabledCount", countMerchantByStatus("DISABLED"));
        return Result.success(data);
    }

    /**
     * 通过审批：PENDING → APPROVED（文案随 data 返回；显式清空旧拒绝理由）
     */
    @PutMapping("/audit/{id}/approve")
    public Result<String> approve(@PathVariable Long id, HttpServletRequest request) {
        if (!isAdmin(request)) {
            return Result.error(403, "无权限，仅管理员可审批");
        }
        SysUser user = sysUserMapper.selectById(id);
        if (user == null || !"MERCHANT".equals(user.getRole())) {
            return Result.error(404, "申请记录不存在");
        }
        if (!"PENDING".equals(user.getStatus())) {
            return Result.error(400, "该申请不在待审核状态");
        }
        sysUserMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, user.getId())
                .set(SysUser::getStatus, "APPROVED")
                .set(SysUser::getRejectReason, null)
                .set(SysUser::getAuditTime, LocalDateTime.now()));
        return Result.success("已通过「" + user.getMerchantName() + "」的入驻申请");
    }

    /**
     * 一键通过全部待审申请（批量审批，单条 UPDATE 显式清空拒绝理由）
     */
    @PutMapping("/audit/approve-all")
    public Result<String> approveAll(HttpServletRequest request) {
        if (!isAdmin(request)) {
            return Result.error(403, "无权限，仅管理员可审批");
        }
        List<SysUser> pending = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getStatus, "PENDING").eq(SysUser::getRole, "MERCHANT"));
        if (!pending.isEmpty()) {
            sysUserMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                    .eq(SysUser::getStatus, "PENDING").eq(SysUser::getRole, "MERCHANT")
                    .set(SysUser::getStatus, "APPROVED")
                    .set(SysUser::getRejectReason, null)
                    .set(SysUser::getAuditTime, LocalDateTime.now()));
        }
        return Result.success("已一键通过 " + pending.size() + " 条入驻申请");
    }

    /**
     * 拒绝审批：PENDING → REJECTED（记录理由，商家可修改资料重新申请；文案随 data 返回）
     */
    @PutMapping("/audit/{id}/reject")
    public Result<String> reject(@PathVariable Long id, @RequestBody Map<String, String> body,
                                 HttpServletRequest request) {
        if (!isAdmin(request)) {
            return Result.error(403, "无权限，仅管理员可审批");
        }
        SysUser user = sysUserMapper.selectById(id);
        if (user == null || !"MERCHANT".equals(user.getRole())) {
            return Result.error(404, "申请记录不存在");
        }
        if (!"PENDING".equals(user.getStatus())) {
            return Result.error(400, "该申请不在待审核状态");
        }
        String reason = body.get("reason");
        sysUserMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, user.getId())
                .set(SysUser::getStatus, "REJECTED")
                .set(SysUser::getRejectReason, reason == null ? null : reason.trim())
                .set(SysUser::getAuditTime, LocalDateTime.now()));
        return Result.success("已拒绝「" + user.getMerchantName() + "」的入驻申请");
    }

    /**
     * 停用商家：APPROVED → DISABLED（该账号立即无法登录；文案随 data 返回）
     */
    @PutMapping("/audit/{id}/disable")
    public Result<String> disable(@PathVariable Long id, HttpServletRequest request) {
        if (!isAdmin(request)) {
            return Result.error(403, "无权限，仅管理员可操作");
        }
        SysUser user = sysUserMapper.selectById(id);
        if (user == null || !"MERCHANT".equals(user.getRole())) {
            return Result.error(404, "商家账号不存在");
        }
        if (!"APPROVED".equals(user.getStatus())) {
            return Result.error(400, "仅已通过的商家可停用");
        }
        user.setStatus("DISABLED");
        user.setAuditTime(LocalDateTime.now());
        sysUserMapper.updateById(user);
        return Result.success("已停用商家「" + user.getMerchantName() + "」，该账号将无法登录");
    }

    /**
     * 启用商家：DISABLED → APPROVED（恢复登录；文案随 data 返回）
     */
    @PutMapping("/audit/{id}/enable")
    public Result<String> enable(@PathVariable Long id, HttpServletRequest request) {
        if (!isAdmin(request)) {
            return Result.error(403, "无权限，仅管理员可操作");
        }
        SysUser user = sysUserMapper.selectById(id);
        if (user == null || !"MERCHANT".equals(user.getRole())) {
            return Result.error(404, "商家账号不存在");
        }
        if (!"DISABLED".equals(user.getStatus())) {
            return Result.error(400, "仅已停用的商家可重新启用");
        }
        user.setStatus("APPROVED");
        user.setAuditTime(LocalDateTime.now());
        sysUserMapper.updateById(user);
        return Result.success("已启用商家「" + user.getMerchantName() + "」，该账号恢复登录");
    }

    /**
     * 按状态统计商家数量（MERCHANT 角色）
     */
    private long countMerchantByStatus(String status) {
        return sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getStatus, status).eq(SysUser::getRole, "MERCHANT"));
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
