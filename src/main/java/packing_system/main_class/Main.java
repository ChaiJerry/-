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
        //fpGrowthTest();
        queryTest();
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
