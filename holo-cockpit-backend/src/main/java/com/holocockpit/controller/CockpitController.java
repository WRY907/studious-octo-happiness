package com.holocockpit.controller;

import com.holocockpit.common.Result;
import com.holocockpit.config.JwtUtil;
import com.holocockpit.entity.CitySales;
import com.holocockpit.entity.HotProduct;
import com.holocockpit.entity.OverviewStats;
import com.holocockpit.entity.RealtimeOrder;
import com.holocockpit.entity.RegionSales;
import com.holocockpit.service.CockpitService;
import io.jsonwebtoken.Claims;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 全息数据驾驶舱数据接口（大屏）
 * 角色感知：携带商家(MERCHANT)令牌访问时，金额类数据脱敏（置null + masked=true）
 */
@RestController
@RequestMapping("/cockpit")
public class CockpitController {

    @Resource
    private CockpitService cockpitService;

    /**
     * 获取全部驾驶舱数据（一次性返回）
     * 商家角色：金额类字段脱敏（销售额/热销金额/订单金额），保留非金额指标与图表结构
     */
    @GetMapping("/all")
    public Result<Map<String, Object>> getAll(HttpServletRequest request) {
        Map<String, Object> data = cockpitService.getAll();
        if (isMerchant(request)) {
            desensitize(data);
        }
        return Result.success(data);
    }

    /**
     * 省份销售数据（默认最新日期，可选 date=yyyy-MM-dd）
     * 商家角色：销售额脱敏
     */
    @GetMapping("/region")
    public Result<List<RegionSales>> getRegionSales(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            HttpServletRequest request) {
        List<RegionSales> list = cockpitService.getRegionSales(date);
        if (isMerchant(request)) {
            list.forEach(r -> r.setSales(null));
        }
        return Result.success(list);
    }

    /**
     * 城市销售数据（默认最新日期TOP10，可选 date=yyyy-MM-dd）
     * 商家角色：销售额脱敏
     */
    @GetMapping("/city")
    public Result<List<CitySales>> getCitySales(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            HttpServletRequest request) {
        List<CitySales> list = cockpitService.getCitySales(date);
        if (isMerchant(request)) {
            list.forEach(c -> c.setSales(null));
        }
        return Result.success(list);
    }

    // ==================== 商家脱敏 ====================

    /**
     * 解析可选的 Bearer 令牌，判断是否商家角色（无令牌/解析失败均视为非商家）
     */
    private boolean isMerchant(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return false;
        }
        try {
            Claims claims = JwtUtil.parseToken(auth.substring(7).trim());
            return "MERCHANT".equals(claims.get("role", String.class));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 大屏数据脱敏：
     * - overview：今日/累计销售额置空（增长率保留）
     * - hotProducts：销售金额置空（销量台数保留）
     * - realtimeOrders：订单金额置空
     * - region/city：数值保留（地图相对热力需要），前端 tooltip 层脱敏显示
     * - masked=true：告知前端进入脱敏模式
     */
    @SuppressWarnings("unchecked")
    private void desensitize(Map<String, Object> data) {
        data.put("masked", true);

        Object ov = data.get("overview");
        if (ov instanceof OverviewStats) {
            OverviewStats o = (OverviewStats) ov;
            o.setTodaySales(null);
            o.setTotalSales(null);
        }

        Object hots = data.get("hotProducts");
        if (hots instanceof List) {
            ((List<Object>) hots).stream()
                    .filter(HotProduct.class::isInstance)
                    .map(HotProduct.class::cast)
                    .forEach(h -> h.setSalesAmount(null));
        }

        Object orders = data.get("realtimeOrders");
        if (orders instanceof List) {
            ((List<Object>) orders).stream()
                    .filter(RealtimeOrder.class::isInstance)
                    .map(RealtimeOrder.class::cast)
                    .forEach(o -> o.setAmount(null));
        }
    }
}
