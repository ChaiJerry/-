package packing_system.main_class;

import org.apache.spark.sql.*;
import packing_system.io.*;

import java.io.*;
import java.util.*;

import static packing_system.api.API.*;
import static packing_system.data_generating_system.FPGrowth.*;
import static packing_system.io.MongoUtils.*;
import static packing_system.io.SharedAttributes.*;
import static packing_system.query_system.QuerySystem.*;

public class Main {
    public static void main(String[] args) throws IOException {
        //rulesAndFreqInDB2csv();

//        String[] fullNames = getFullNames();
//        System.out.print("AttributeName,");
//        for(int i = 1; i < fullNames.length; i++) {
//            System.out.print(fullNames[i]+" Precision,");
//            System.out.print(fullNames[i]+" RecallRate,");
//            System.out.print(fullNames[i]+" F1Score,");
//        }

        fpGrowthTest();
        long start = System.currentTimeMillis();
        for(int eva = 1; eva < 6; eva++) {
            evaluateSingleBundleItem(eva, getTestTicketsMap());
        }
        long end = System.currentTimeMillis();
        System.out.println("测试总用时: " + (end - start)+ "ms");
//        for(int i = 0; i < 16; i++) {
//            fpGrowthTest(i);
//            evaluate(i);
//        }

//        CSVFileIO fileIO = getFileIO();
//        List<List<String>> listOfAttributeList = fileIO.singleTypeCsv2ListOfAttributeList(HOTEL);
//        List<List<String>> itemTicketFreqItemSets = new ArrayList<>();
//        List<List<String>> itemTicketRules = new ArrayList<>();
//        associationRulesMining(listOfAttributeList, false, false, itemTicketFreqItemSets, itemTicketRules, 0.09,0);
//
//
//        associationRulesMining(listOfAttributeList, true, true, itemTicketFreqItemSets, itemTicketRules, 0.09,0);


    }
}
