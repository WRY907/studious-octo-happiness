package com.holocockpit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 用户画像分布（性别/年龄）
 */
@Data
@TableName("user_profile")
public class UserProfile implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 维度类型：gender-性别 / age-年龄
     */
    private String profileType;

    /**
     * 维度名称（如 男性 / 18-24岁）
     */
    private String profileName;

    /**
     * 用户数
     */
    private Integer userCount;

    /**
     * 占比（%）
     */
    private BigDecimal ratio;
}
