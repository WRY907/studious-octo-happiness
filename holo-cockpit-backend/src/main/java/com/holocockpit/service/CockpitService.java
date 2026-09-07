package com.holocockpit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.holocockpit.entity.*;
import com.holocockpit.mapper.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 驾驶舱数据服务：一次查询返回大屏全部数据
 */
@Service
public class CockpitService {

    @Resource
    private OverviewStatsMapper overviewStatsMapper;

    @Resource
    private SalesTrendMapper salesTrendMapper;

    @Resource
    private RegionSalesMapper regionSalesMapper;

    @Resource
    private CitySalesMapper citySalesMapper;

    @Resource
    private RealtimeOrderMapper realtimeOrderMapper;

    @Resource
    private HotProductMapper hotProductMapper;

    @Resource
    private TrafficSourceMapper trafficSourceMapper;

    @Resource
    private UserProfileMapper userProfileMapper;

    @Resource
    private JdbcTemplate jdbcTemplate;

    /**
     * 获取全部驾驶舱数据（大屏一次性加载）
     */
    public Map<String, Object> getAll() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("overview", getOverview());
        data.put("salesTrend", getSalesTrend30());
        data.put("regionSales", getRegionSales(null));
        data.put("citySales", getCitySales(null));
        data.put("hotProducts", getHotProducts());
        data.put("trafficSources", getTrafficSources());
        data.put("userProfiles", getUserProfiles());
        data.put("realtimeOrders", getRealtimeOrders());
        return data;
    }

    /**
     * 总览统计（最新一条）
     */
    public OverviewStats getOverview() {
        OverviewStats stats = overviewStatsMapper.selectOne(
                new LambdaQueryWrapper<OverviewStats>()
                        .orderByDesc(OverviewStats::getUpdateTime)
                        .last("LIMIT 1"));
        return stats != null ? stats : new OverviewStats();
    }

    /**
     * 销售趋势：最近30天，按日期升序
     */
    public List<SalesTrend> getSalesTrend30() {
        List<SalesTrend> list = salesTrendMapper.selectList(
                new LambdaQueryWrapper<SalesTrend>()
                        .orderByDesc(SalesTrend::getStatDate)
                        .last("LIMIT 30"));
        Collections.reverse(list);
        return list;
    }

    /**
     * 省份销售数据：默认最新统计日期（"今日"=MAX(stat_date)）的全部省份，支持指定日期
     */
    public List<RegionSales> getRegionSales(LocalDate date) {
        LocalDate target = date != null ? date : maxDate("region_sales");
        if (target == null) {
            return Collections.emptyList();
        }
        return regionSalesMapper.selectList(
                new LambdaQueryWrapper<RegionSales>()
                        .eq(RegionSales::getStatDate, target)
                        .orderByDesc(RegionSales::getSales));
    }

    /**
     * 城市销售数据：默认最新统计日期的TOP10城市（按销售额倒序），支持指定日期
     */
    public List<CitySales> getCitySales(LocalDate date) {
        LocalDate target = date != null ? date : maxDate("city_sales");
        if (target == null) {
            return Collections.emptyList();
        }
        return citySalesMapper.selectList(
                new LambdaQueryWrapper<CitySales>()
                        .eq(CitySales::getStatDate, target)
                        .orderByDesc(CitySales::getSales)
                        .last("LIMIT 10"));
    }

    /**
     * 热销机型排名（rank_no 升序）
     */
    public List<HotProduct> getHotProducts() {
        return hotProductMapper.selectList(
                new LambdaQueryWrapper<HotProduct>().orderByAsc(HotProduct::getRankNo));
    }

    /**
     * 流量来源（按访问量倒序）
     */
    public List<TrafficSource> getTrafficSources() {
        return trafficSourceMapper.selectList(
                new LambdaQueryWrapper<TrafficSource>().orderByDesc(TrafficSource::getVisits));
    }

    /**
     * 用户画像分布（性别/年龄）
     */
    public List<UserProfile> getUserProfiles() {
        return userProfileMapper.selectList(null);
    }

    /**
     * 实时订单：最近20条（按时间倒序）
     */
    public List<RealtimeOrder> getRealtimeOrders() {
        return realtimeOrderMapper.selectList(
                new LambdaQueryWrapper<RealtimeOrder>()
                        .orderByDesc(RealtimeOrder::getCreateTime)
                        .last("LIMIT 20"));
    }

    /**
     * 查询指定表的最新统计日期
     */
    public LocalDate maxDate(String table) {
        return jdbcTemplate.queryForObject(
                "SELECT MAX(stat_date) FROM " + table, LocalDate.class);
    }
}
