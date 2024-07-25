package mid_tests;

import org.apache.spark.ml.fpm.*;
import org.apache.spark.sql.*;
import org.junit.*;

import java.io.*;

import static data_processer.DataConverter.*;
import static data_generating_system.FPGrowth.*;
import static org.junit.Assert.*;

public class TestDataConverter {
    @Before
    public void start() throws IOException {
        initializeSpark();
        initializeFileIO();
    }

    @Test
    public void testRow2rule() throws IOException {
        Dataset<Row> rowDataset = getFileIO().singelTypeCsv2dataset(1);
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
