package com.holocockpit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 城市销售数据（用于城市榜/地图下钻）
 */
@Data
@TableName("city_sales")
public class CitySales implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 城市名称
     */
    private String city;

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
