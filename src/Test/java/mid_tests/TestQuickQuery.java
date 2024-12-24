package mid_tests;

import bundle_system.memory_query_system.*;
import org.junit.*;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

public class TestQuickQuery {
    @Test
    public void test() throws IOException {
        double[] results = {0,0.4722222222222222,0.4357142857142857
                ,0.5095029239766081,0.6452448210922788,0.33280839895013126};
        for(int i=1;i<6;i++){
            //由于数据和规则文件的不断更换，这里直接看控制台输出结果是否符合
            QuickQuery quickQuery = new QuickQuery();
            assertTrue((quickQuery.test(i)-results[i])
                    *(quickQuery.test(i)-results[i])<1e-5);
        }

    }

}
