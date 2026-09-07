package com.holocockpit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 实时订单数据
 */
@Data
@TableName("realtime_order")
public class RealtimeOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 机型名称
     */
    private String modelName;

    /**
     * 订单金额（元）
     */
    private BigDecimal amount;

    /**
     * 省份
     */
    private String province;

    /**
     * 城市
     */
    private String city;

    /**
     * 订单状态：1-待付款 2-已付款 3-已发货 4-已完成
     */
    private Integer status;

    /**
     * 下单时间
     */
    private LocalDateTime createTime;

    /**
     * 金额是否已脱敏（MERCHANT 角色查询时金额置空，masked=true）
     */
    @TableField(exist = false)
    private Boolean masked;
}
