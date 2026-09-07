package com.holocockpit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 销售趋势数据（按天）
 */
@Data
@TableName("sales_trend")
public class SalesTrend implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 统计日期
     */
    private LocalDate statDate;

    /**
     * 销售额（元）
     */
    private BigDecimal sales;

    /**
     * 订单数
     */
    private Integer orders;

    /**
     * 访问量
     */
    private Integer visits;

    /**
     * 客单价（元）
     */
    private BigDecimal avgOrderValue;
}
