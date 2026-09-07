package com.holocockpit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统用户（ADMIN-管理员 / MERCHANT-商户）
 */
@Data
@TableName("sys_user")
public class SysUser implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码（MD5 小写 hex）
     */
    private String password;

    /**
     * 角色：ADMIN-管理员 / MERCHANT-商户
     */
    private String role;

    /**
     * 商户名称（MERCHANT 角色使用）
     */
    private String merchantName;

    /**
     * 审批状态：PENDING-待审核 / APPROVED-已通过 / REJECTED-已拒绝 / DISABLED-已停用
     */
    private String status;

    /**
     * 拒绝理由（REJECTED 时使用）
     */
    private String rejectReason;

    /**
     * 最近一次审批时间
     */
    private LocalDateTime auditTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
