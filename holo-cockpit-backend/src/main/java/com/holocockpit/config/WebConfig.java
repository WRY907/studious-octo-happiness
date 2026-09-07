package com.holocockpit.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Web 配置：注册 JwtFilter、JdbcTemplate
 */
@Configuration
public class WebConfig {

    /**
     * 注册 JWT 过滤器，仅拦截 /admin/* 路径
     */
    @Bean
    public FilterRegistrationBean<JwtFilter> jwtFilter() {
        FilterRegistrationBean<JwtFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new JwtFilter());
        registration.addUrlPatterns("/admin/*");
        registration.setName("jwtFilter");
        registration.setOrder(1);
        return registration;
    }

    /**
     * JdbcTemplate：用于 AI 智能查询动态执行 SQL
     */
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
