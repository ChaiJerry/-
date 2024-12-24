package com.bundling;

import com.bundling.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static bundle_system.io.SharedAttributes.*;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.SQLException;

class ParseMethodTest {
    // 初始化参数解析器
    private ArgParser argParser;

    @BeforeEach
    public void setUp() {
    }

    /**
     * 测试在传入有效参数且规则存储为空的情况下解析命令行参数。
     *
     * @throws ClassNotFoundException 如果在测试过程中遇到类未找到异常
     * @throws SQLException 如果在测试过程中遇到SQL异常
     */
    @Test
    void testParseWithValidArgumentsAndEmptyRules() throws ClassNotFoundException, SQLException {
        String[] args = {
                "--TheadNum=5", // 设置线程数为5
                "--TrainIdForBundle=10", // 设置训练ID为10
                "--dbProperties={jdbc:mysql://120.46.3.97:3306/db22371253,u22371253,Aa702451}" // 设置数据库连接属性
        };
        // 解析命令行参数
        boolean result = ArgParser.parse(args);
        // 断言解析成功
        assertTrue(result);
        // 断言MEAL类型的规则存储大小为0
        assertEquals(0, BundleService.defaultBundleSystem.getRulesStorages().get(MEAL).getSize());
        // 断言线程数为5
        assertEquals(5, BundleService.threadNum);
    }

    /**
     * 测试在传入有效参数并包含规则的情况下解析命令行参数。
     *
     * @throws ClassNotFoundException 如果在测试过程中遇到类未找到异常
     * @throws SQLException 如果在测试过程中遇到SQL异常
     */
    @Test
    void testParseWithValidArgumentsWithRules() throws ClassNotFoundException, SQLException {
        String[] args = {
                "--TheadNum=6", // 设置线程数为6
                "--TrainIdForBundle=51", // 设置训练ID为51
                "--dbProperties={jdbc:mysql://120.46.3.97:3306/db22371253,u22371253,Aa702451}" // 设置数据库连接属性
        };
        // 解析命令行参数
        boolean result = ArgParser.parse(args);
        // 断言解析成功
        assertTrue(result);
        // 断言MEAL类型的规则存储大小为246
        assertEquals(246, BundleService.defaultBundleSystem.getRulesStorages().get(MEAL).getSize());
        // 断言线程数为6
        assertEquals(6, BundleService.threadNum);
    }

    /**
     * 测试在不传递任何参数的情况下解析命令行参数。
     *
     * @throws ClassNotFoundException 如果在测试过程中遇到类未找到异常
     * @throws SQLException 如果在测试过程中遇到SQL异常
     */
    @Test
    void testParseWithoutAnyArguments() throws ClassNotFoundException, SQLException {
        // 不传递任何参数
        String[] args = {};
        // 解析命令行参数
        boolean result = ArgParser.parse(args);
        // 断言解析失败
        assertFalse(result);
    }
}