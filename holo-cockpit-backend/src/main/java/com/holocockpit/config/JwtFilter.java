package com.holocockpit.config;

import cn.hutool.json.JSONUtil;
import com.holocockpit.common.Result;
import io.jsonwebtoken.Claims;
import org.springframework.http.HttpMethod;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * JWT 认证过滤器：仅拦截 /admin/* 路径
 * 解析 Authorization: Bearer xxx，将 userId/username/role 放入 request attribute，
 * 无效令牌直接返回 401 Result JSON
 */
public class JwtFilter extends OncePerRequestFilter {

    /**
     * 仅拦截 /admin/ 开头的业务路径，放行 CORS 预检请求
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        return !path.startsWith("/admin/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            writeUnauthorized(response, "未登录或缺少访问令牌");
            return;
        }
        try {
            Claims claims = JwtUtil.parseToken(authorization.substring(7).trim());
            request.setAttribute("userId", claims.get("userId", Long.class));
            request.setAttribute("username", claims.get("username", String.class));
            request.setAttribute("role", claims.get("role", String.class));
            chain.doFilter(request, response);
        } catch (Exception e) {
            writeUnauthorized(response, "令牌无效或已过期，请重新登录");
        }
    }

    /**
     * 返回 401 的 Result JSON
     */
    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getOutputStream().write(JSONUtil.toJsonStr(Result.error(401, message)).getBytes(StandardCharsets.UTF_8));
    }
}
