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

        queryTest("eva");
        //fpGrowthTest();
//        Map<String, List<List<String>>> testMap = getTestTicketMap();
//        for(Map.Entry<String, List<List<String>>> entry : testMap.entrySet()) {
//            System.out.print("OrderNum:"+entry.getKey());
//            List<List<String>> value = entry.getValue();
//            if(value.size() == 2) {
//                for(String list : value.get(1)) {
//                    System.out.print(","+list);
//                }
//            }
//            for(String list : value.get(0)) {
//                System.out.print(","+list);
//            }
//            System.out.println();
//        }

    }
}
