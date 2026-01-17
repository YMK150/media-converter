package com.mediaconverter.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * SQLite 数据源配置
 * 针对 SQLite 数据库的并发访问问题进行优化
 */
@Configuration
@RequiredArgsConstructor
public class DataSourceConfig {

    private final DataSource dataSource;

    /**
     * 配置事务管理器
     * 使用适当的事务超时设置
     */
    @Bean
    public PlatformTransactionManager transactionManager() {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setDataSource(dataSource);
        // 设置默认事务超时时间为 30 秒
        transactionManager.setDefaultTimeout(30);
        return transactionManager;
    }
}
