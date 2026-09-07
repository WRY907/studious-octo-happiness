package com.holocockpit;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 全息数据驾驶舱启动类
 */
@SpringBootApplication
@MapperScan("com.holocockpit.mapper")
@EnableScheduling
public class HoloCockpitApplication {

    public static void main(String[] args) {
        SpringApplication.run(HoloCockpitApplication.class, args);
        System.out.println("=======================================");
        System.out.println("  全息数据驾驶舱后端服务启动成功！");
        System.out.println("  接口地址: http://localhost:8080/api");
        System.out.println("=======================================");
    }
}
