package com.xuetupt.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis Mapper 扫描配置
 * <p>
 * 扫描 com.xuetupt.mapper 包下的所有 Mapper 接口，
 * 使其可以被 Spring 容器管理。
 */
@Configuration
@MapperScan("com.xuetupt.mapper")
public class MapperScanConfig {
}
