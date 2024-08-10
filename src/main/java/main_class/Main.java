package main_class;



import io.*;

import java.io.*;
import java.util.*;

import static data_generating_system.FPGrowth.*;
import static io.CSVFileIO.*;
import static io.MongoUtils.*;
import static io.SharedAttributes.*;
import static query_system.QuerySystem.*;

public class Main {
    public static void main(String[] args) throws IOException {
        //queryTest("eva");
        ///fpGrowthTest();
        Map<String, List<List<String>>> testMap = fileIO.read(0);
        for(Map.Entry<String, List<List<String>>> entry : testMap.entrySet()) {
            System.out.print("OrderNum:"+entry.getKey());
            for(String list : entry.getValue().get(0)) {
                System.out.print(","+list);
            }
            System.out.println();
        }

        //ordersMap2DB(getTestMap(), 0);
    }
}
