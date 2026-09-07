package com.holocockpit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 流量来源
 */
@Data
@TableName("traffic_source")
public class TrafficSource implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 一级分类（如 线上渠道/线下渠道/搜索引擎）
     */
    private String category;

    /**
     * 二级来源名称
     */
    private String sourceName;

    /**
     * 访问量
     */
    private Long visits;

    /**
     * 占比（%）
     */
    private BigDecimal ratio;
}
