package com.holocockpit.service;

import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.holocockpit.config.JwtUtil;
import com.holocockpit.entity.SysUser;
import com.holocockpit.mapper.SysUserMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证服务：登录校验（MD5）并签发 JWT
 */
@Service
public class AuthService {

    @Resource
    private SysUserMapper sysUserMapper;

    /**
     * 登录：校验用户名 + MD5 密码，成功返回令牌与用户信息
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
        String token = JwtUtil.createToken(user.getId(), user.getUsername(), user.getRole());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("role", user.getRole());
        data.put("username", user.getUsername());
        data.put("merchantName", user.getMerchantName());
        return data;
    }
}
