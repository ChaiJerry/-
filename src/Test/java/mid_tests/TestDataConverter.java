package mid_tests;

import org.apache.spark.ml.fpm.*;
import org.apache.spark.sql.*;
import org.junit.*;

import java.io.*;

import static packing_system.data_processer.DataConverter.*;
import static packing_system.data_generating_system.FPGrowth.*;
import static org.junit.Assert.*;

public class TestDataConverter {

    @Test
    public void testRow2rule() throws IOException {
        Dataset<Row> rowDataset = getFileIO().singleTypeCsv2dataset(1,0);
        FPGrowthModel model = train(rowDataset);
        Dataset<Row> ruleDataset = model.associationRules();
        for (Row row : ruleDataset.collectAsList()) {
            boolean flag = true;
            for (Object s : row.getList(0)) {
                String temp = s.toString();
                if (temp.charAt(0) != 'T') {
                    flag = false;
                }
            }
            String temp = row.getList(1).get(0).toString() + "; ";
            if (temp.charAt(0) == 'T') {
                flag = false;
            }
            String[] processedStr = row2rule(row);
            assertEquals(flag, processedStr.length != 0);
        }

    }

}
