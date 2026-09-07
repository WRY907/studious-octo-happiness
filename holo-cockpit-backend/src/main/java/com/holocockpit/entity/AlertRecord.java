package com.holocockpit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 智能预警记录
 */
@Data
@TableName("alert_record")
public class AlertRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 预警类型：SALES_DROP-销售下滑 / SALES_SURGE-销售激增
     */
    private String alertType;

    /**
     * 预警标题
     */
    private String title;

    /**
     * 预警内容
     */
    private String content;

    /**
     * AI 应对建议
     */
    private String aiAdvice;

    /**
     * 预警级别：1-提示 2-警告 3-严重
     */
    private Integer level;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
