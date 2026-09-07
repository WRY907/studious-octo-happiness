package com.holocockpit.controller;

import com.holocockpit.common.Result;
import com.holocockpit.service.AuthService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 认证接口
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Resource
    private AuthService authService;

    /**
     * 登录：返回 {token, role, username, merchantName}
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        try {
            return Result.success(authService.login(body.get("username"), body.get("password")));
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "登录失败：" + e.getMessage());
        }
    }

    /**
     * 商家注册：提交入驻申请，进入待审状态，管理员审核通过后方可登录
     */
    @PostMapping("/register")
    public Result<Void> register(@RequestBody Map<String, String> body) {
        try {
            authService.register(body.get("username"), body.get("password"), body.get("merchantName"));
            return Result.success("注册申请已提交，请等待管理员审核", null);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "注册失败：" + e.getMessage());
        }
    }
}
