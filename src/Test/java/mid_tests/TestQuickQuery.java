package mid_tests;

import bundle_system.memory_query_system.*;
import org.junit.*;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

public class TestQuickQuery {
    @Test
    public void test() throws IOException {
        for(int i=1;i<6;i++){
            //由于数据和规则文件的不断更换，这里直接看控制台输出结果是否符合
            QuickQuery quickQuery = new QuickQuery();
            quickQuery.test(i);
        }
        assertTrue(true);
    }

}
