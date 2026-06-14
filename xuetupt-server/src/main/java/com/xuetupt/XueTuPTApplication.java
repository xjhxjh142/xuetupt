package com.xuetupt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 学途PT - 主启动类
 * <p>
 * 基于 Spring Boot 的在线教育平台后端服务。
 * 提供课程管理、资料下载、自习室、秒杀等功能。
 */
@SpringBootApplication
public class XueTuPTApplication {
    public static void main(String[] args) {
        SpringApplication.run(XueTuPTApplication.class, args);
    }
}
