package mid_tests;

import bundle_system.io.sql.*;
import org.junit.*;
import static org.junit.jupiter.api.Assertions.*;

public class TestSQLUtils {
    SQLUtils sqlUtils = new SQLUtils();
    @Test
    public void testConnection() {
        boolean connected = sqlUtils.isConnected();
        System.out.println("测试数据库连接成功="+connected);
        Assertions.assertTrue(connected);
    }

    @Test
    public void getTrainDataTableName() {
        //验证是否正确获取表名并防止不正确的表名输入
        Assertions.assertNull(sqlUtils.getTrainDataTableName("tax"));
        Assertions.assertEquals("train_data_seat",sqlUtils.getTrainDataTableName("seat"));
    }
}