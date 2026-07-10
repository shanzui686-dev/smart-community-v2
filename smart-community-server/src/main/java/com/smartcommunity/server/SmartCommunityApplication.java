package com.smartcommunity.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.smartcommunity.server.mapper")
@EnableAsync
@EnableScheduling
public class SmartCommunityApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartCommunityApplication.class, args);
        System.out.println("========================================");
        System.out.println("  智慧小区管理系统服务端启动成功！");
        System.out.println("  API文档: http://localhost:8085/doc.html");
        System.out.println("========================================");
    }
}
