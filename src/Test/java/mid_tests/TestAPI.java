package mid_tests;

import io.*;
import org.apache.spark.sql.*;
import org.junit.*;

import java.io.*;
import java.util.*;

import static api.API.*;
import static data_generating_system.FPGrowth.*;
import static org.junit.Assert.*;

public class TestAPI {

    @Before
    public void start() {
        initializeSpark();
    }

    @org.junit.Test
    public void test() throws IOException {
        CSVFileIO fileIO = getFileIO();
        Dataset<Row> dataset = fileIO.singelTypeCsv2dataset(1);
        List<List<String>> itemTicketFreqItemSets = new ArrayList<>();
        List<List<String>> itemTicketRules = new ArrayList<>();
        mining(dataset, false,false,itemTicketFreqItemSets,itemTicketRules);
        assertTrue(itemTicketFreqItemSets.isEmpty());
        assertTrue(itemTicketRules.isEmpty());

        mining(dataset, true,true,itemTicketFreqItemSets,itemTicketRules);
        assertFalse(itemTicketFreqItemSets.isEmpty());
        assertFalse(itemTicketRules.isEmpty());
    }
}
