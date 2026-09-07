package com.holocockpit.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.holocockpit.common.Result;
import com.holocockpit.entity.AlertRecord;
import com.holocockpit.entity.OverviewStats;
import com.holocockpit.entity.PhoneModel;
import com.holocockpit.entity.RealtimeOrder;
import com.holocockpit.entity.SalesTrend;
import com.holocockpit.entity.SysUser;
import com.holocockpit.mapper.AlertRecordMapper;
import com.holocockpit.mapper.PhoneModelMapper;
import com.holocockpit.mapper.RealtimeOrderMapper;
import com.holocockpit.mapper.SalesTrendMapper;
import com.holocockpit.mapper.SysUserMapper;
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
