package com.holocockpit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 热销机型排名
 */
@Data
@TableName("hot_product")
public class HotProduct implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 排名
     */
    private Integer rankNo;

    /**
     * 机型名称
     */
    private String modelName;

    /**
     * 销量（台）
     */
    private Integer salesCount;

    /**
     * 销售额（元）
     */
    private BigDecimal salesAmount;

    /**
     * 增长率（%）
     */
    private BigDecimal growth;
}
