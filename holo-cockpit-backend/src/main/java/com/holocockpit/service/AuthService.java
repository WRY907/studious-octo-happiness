package com.holocockpit.service;

import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.holocockpit.config.JwtUtil;
import com.holocockpit.entity.SysUser;
import com.holocockpit.mapper.SysUserMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证服务：登录校验（MD5）并签发 JWT；商家注册（需管理员审批）
 */
@Service
public class AuthService {

    @Resource
    private SysUserMapper sysUserMapper;

    /**
     * 登录：校验用户名 + MD5 密码，成功返回令牌与用户信息
     * 审批状态拦截：待审/被拒/停用 的账号不能进入系统（类群邀请：群主未同意则不能进群）
     */
    public Map<String, Object> login(String username, String password) {
        if (username == null || username.trim().isEmpty()
                || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("用户名和密码不能为空");
        }
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username.trim()));
        // MD5 小写 hex 比对
        if (user == null || !SecureUtil.md5(password).equals(user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        // 审批状态拦截
        String status = user.getStatus() == null ? "APPROVED" : user.getStatus();
        if ("PENDING".equals(status)) {
            throw new IllegalArgumentException("账号正在等待管理员审核，通过后即可登录系统");
        }
        if ("REJECTED".equals(status)) {
            String reason = user.getRejectReason();
            throw new IllegalArgumentException("注册审核未通过"
                    + (reason != null && !reason.trim().isEmpty() ? "：" + reason.trim() : "")
                    + "，可修改资料后重新提交注册");
        }
        if ("DISABLED".equals(status)) {
            throw new IllegalArgumentException("账号已被管理员停用，如有疑问请联系管理员");
        }
        String token = JwtUtil.createToken(user.getId(), user.getUsername(), user.getRole());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("role", user.getRole());
        data.put("username", user.getUsername());
        data.put("merchantName", user.getMerchantName());
        return data;
    }

    /**
     * 商家注册：提交后进入 PENDING 待审状态，管理员通过后方可登录
     * 同名账号仅在 REJECTED（被拒）状态时允许重新提交，覆盖旧申请回到待审
     */
    public void register(String username, String password, String merchantName) {
        if (username == null || username.trim().length() < 3 || username.trim().length() > 32) {
            throw new IllegalArgumentException("用户名需为 3-32 个字符");
        }
        if (!username.trim().matches("[A-Za-z0-9_\\-\\u4e00-\\u9fa5]+")) {
            throw new IllegalArgumentException("用户名仅支持中文、英文、数字、下划线和横线");
        }
        if (password == null || password.length() < 6 || password.length() > 64) {
            throw new IllegalArgumentException("密码需为 6-64 个字符");
        }
        if (merchantName == null || merchantName.trim().isEmpty()) {
            throw new IllegalArgumentException("商户名称不能为空");
        }
        if (merchantName.trim().length() > 50) {
            throw new IllegalArgumentException("商户名称不能超过 50 个字符");
        }
        String uname = username.trim();
        SysUser exist = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, uname));
        LocalDateTime now = LocalDateTime.now();
        if (exist == null) {
            SysUser user = new SysUser();
            user.setUsername(uname);
            user.setPassword(SecureUtil.md5(password));
            user.setRole("MERCHANT");
            user.setMerchantName(merchantName.trim());
            user.setStatus("PENDING");
            user.setCreateTime(now);
            sysUserMapper.insert(user);
            return;
        }
        // 被拒的申请允许重新提交：覆盖资料并回到待审状态（显式清空旧拒绝理由）
        if ("REJECTED".equals(exist.getStatus())) {
            sysUserMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                    .eq(SysUser::getId, exist.getId())
                    .set(SysUser::getPassword, SecureUtil.md5(password))
                    .set(SysUser::getMerchantName, merchantName.trim())
                    .set(SysUser::getStatus, "PENDING")
                    .set(SysUser::getRejectReason, null)
                    .set(SysUser::getAuditTime, null)
                    .set(SysUser::getCreateTime, now));
            return;
        }
        throw new IllegalArgumentException("用户名已被占用，请更换用户名");
    }
}
