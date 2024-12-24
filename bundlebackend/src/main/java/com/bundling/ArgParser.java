package com.bundling;

import bundle_system.io.sql.*;
import com.bundling.service.*;
import org.jetbrains.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

import static com.bundling.service.BundleService.*;

public class ArgParser {
    /*
     * 私有构造函数，
     * 防止外部实例化。
     */
    private ArgParser() {
    }
    // 初始化日志对象，用于记录日志信息。
    static Logger logger = LoggerFactory.getLogger(ArgParser.class);

    /**
     * 解析命令行参数并初始化相关配置。
     *
     * @param args 命令行参数数组。
     * @return 如果解析成功返回 true，否则返回 false。
     * @throws ClassNotFoundException 如果找不到 SQL 驱动类。
     * @throws SQLException 如果发生 SQL 相关的异常。
     */
    static boolean parse(String[] args) throws ClassNotFoundException, SQLException {
        // 检查是否提供了任何参数
        if (args.length == 0) {
            // 没有提供任何参数，打印错误信息并返回 false
            logger.error("没有提供命令行参数。");
            return false;
        }
        // 数据库连接信息
        String sqlURL = null;
        String sqlUsername = null;
        String sqlPassword = null;
        // 遍历命令行参数
        for (int i = 0; i < args.length; i++) {
            // 获取当前参数，例如 "--TheadNum=8" 或类似格式
            String arg = args[i];
            // 假设参数格式为 --option=value
            if (arg.startsWith("--")) {
                // 如果是长选项形式
                String[] parts = arg.split("=", 2);
                // 去掉开头的两个破折号
                String option = parts[0].substring(2);
                // 获取值部分，如果有的话（即是否存在等号）
                String value = getValue(parts);
                // 打印选项和值（调试用）
                System.out.printf("获得长选项: %s, 值: %s%n", option, value);
                if (option.equals("TheadNum") && value != null) {
                    // 设置打包线程数
                    BundleService.threadNum = Integer.parseInt(value);
                } else if (option.equals("TrainIdForBundle") && value != null) {
                    // 设置默认用于打包的知识库的知识库的训练批次id
                    trainIdForBundle = Integer.parseInt(value);
                } else if (option.equals("dbProperties")) {
                    // 处理数据库连接配置相关的选项
                    // jdbc:mysql://120.46.3.97:3306/db22371253
                    // 格式为 --dbProperties={url,username,password}
                    String[] dbProps = new String[4];
                    if (value != null) {
                        // 先去掉{和}以及空格，然后按逗号分割
                        dbProps = value.substring(1, value.length() - 1).replaceAll("\\s*", "").split(",");
                    }
                    // 得到数据库连接信息
                    sqlURL = dbProps[0];
                    sqlUsername = dbProps[1];
                    sqlPassword = dbProps[2];
                    // 打印数据库连接信息（调试用）
                    System.out.printf("数据库连接信息: %s, %s, %s%n", sqlURL, sqlUsername, sqlPassword);
                }
            }
        }
        // 初始化 SQLUtils 和 BundleSystem
        initSqlUtilsAndBundleSys(sqlURL, sqlUsername, sqlPassword);
        return true;
    }

    /**
     * 从命令行参数部分获取值。
     *
     * @param parts 分割后的命令行参数部分。
     * @return 参数的值，如果没有值则返回 null。
     */
    @Nullable
    private static String getValue(String[] parts) {
        // 如果参数部分有两个元素，则第二个是值
        return parts.length > 1 ? parts[1] : null;
    }

    /**
     * 初始化 SQL 工具和打包系统。
     *
     * @param sqlURL      数据库 URL。
     * @param sqlUsername 数据库用户名。
     * @param sqlPassword 数据库密码。
     * @throws ClassNotFoundException 如果找不到 SQL 驱动类。
     * @throws SQLException           如果发生 SQL 相关的异常。
     */
    static void initSqlUtilsAndBundleSys(String sqlURL, String sqlUsername, String sqlPassword) throws ClassNotFoundException, SQLException {
        // 初始化 SQLUtils 和 BundleSystem
        if (Application.sqlUtils == null) {
            // 如果初始化信息不完整，则抛出异常
            if (sqlURL == null || sqlUsername == null || sqlPassword == null) {
                throw new IllegalArgumentException("请提供完整的数据库连接信息！");
            }
            // 初始化 SQLUtils
            Application.sqlUtils = new SQLUtils(sqlURL, sqlUsername, sqlPassword);
        }
        // 若是没有设置默认的训练批次id，则不初始化打包系统
        if (trainIdForBundle != -1) {
            // 初始化打包系统
            defaultBundleSystem = getBundleSystem(trainIdForBundle);
        }
    }
}



