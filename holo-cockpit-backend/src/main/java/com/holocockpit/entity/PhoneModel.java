package com.holocockpit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 手机机型信息
 */
@Data
@TableName("phone_model")
public class PhoneModel implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 机型名称
     */
    private String modelName;

    /**
     * 系列（如 Mate / nova / P 系列）
     */
    private String series;

    /**
     * 价格（元）
     */
    private BigDecimal price;

    /**
     * 评分（1-5）
     */
    private BigDecimal rating;

    /**
     * 图片地址
     */
    private String imageUrl;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
