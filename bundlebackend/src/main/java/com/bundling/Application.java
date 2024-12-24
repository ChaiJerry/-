package com.bundling;

import bundle_system.io.sql.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;

import java.sql.*;

/**
 * Spring Boot 应用程序的主类。
 * 排除掉特定的自动配置类
 * ，以避免不必要的数据库连接干扰程序启动。
 */
@SpringBootApplication(exclude = {
        MongoAutoConfiguration.class,
        MongoDataAutoConfiguration.class,
        DataSourceAutoConfiguration.class
})
public class Application {

    /**
     * SQL 工具实例，用于处理数据库操作。
     */
    public static SQLUtils sqlUtils = null;

    /**
     * 主方法，应用程序的入口点。
     *
     * @param args 命令行参数数组。
     * @throws SQLException 如果发生 SQL 相关的异常。
     * @throws ClassNotFoundException 如果找不到 SQL 驱动类。
     */
    public static void main(String[] args) throws SQLException, ClassNotFoundException {
        // 解析命令行参数
        if (!ArgParser.parse(args)) return;
        // 启动 Spring Boot 应用程序
        SpringApplication.run(com.bundling.Application.class, args);
    }
}



