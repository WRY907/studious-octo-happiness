package com.holocockpit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 总览统计数据
 */
@Data
@TableName("overview_stats")
public class OverviewStats implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 累计销售额（元）
     */
    private BigDecimal totalSales;

    /**
     * 累计订单数
     */
    private Integer totalOrders;

    /**
     * 累计用户数
     */
    private Integer totalUsers;

    /**
     * 今日销售额（元）
     */
    private BigDecimal todaySales;

    /**
     * 今日订单数
     */
    private Integer todayOrders;

    /**
     * 今日新增用户
     */
    private Integer todayNewUsers;

    /**
     * 今日访问量
     */
    private Integer todayVisits;

    /**
     * 销售额增长率（%）
     */
    private BigDecimal salesGrowth;

    /**
     * 订单数增长率（%）
     */
    private BigDecimal ordersGrowth;

    /**
     * 用户数增长率（%）
     */
    private BigDecimal usersGrowth;

    /**
     * 访问量增长率（%）
     */
    private BigDecimal visitsGrowth;

    /**
     * 转化率（%）
     */
    private BigDecimal conversionRate;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
