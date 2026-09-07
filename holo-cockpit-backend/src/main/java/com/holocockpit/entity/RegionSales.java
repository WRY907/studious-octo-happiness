package com.holocockpit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 省份销售数据（用于地图）
 */
@Data
@TableName("region_sales")
public class RegionSales implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 省份名称
     */
    private String province;

    /**
     * 销售额（元）
     */
    private BigDecimal sales;

    /**
     * 订单数
     */
    private Integer orders;

    /**
     * 用户数
     */
    private Integer users;

    /**
     * 统计日期
     */
    private LocalDate statDate;
}
