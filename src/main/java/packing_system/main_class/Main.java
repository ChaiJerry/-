package packing_system.main_class;

import packing_system.data_generating_system.*;
import packing_system.io.*;

import java.io.*;
import java.sql.*;
import java.util.*;

import static packing_system.api.API.*;
import static packing_system.data_generating_system.FPGrowth.*;
import static packing_system.db_query_system.QuerySystem.*;
import static packing_system.io.SharedAttributes.*;

public class Main {
    public static void main(String[] args) throws IOException, SQLException {

//        List<List<String>> listOfAttributeList = fileIO.singleTypeCsv2ListOfAttributeList(HOTEL);
//        List<List<String>> itemTicketFreqItemSets = new ArrayList<>();
//        List<List<String>> itemTicketRules = new ArrayList<>();
//        associationRulesMining(listOfAttributeList, false, true, itemTicketFreqItemSets, itemTicketRules, 0.07,0);
//        for (List<String> itemTicketRule : itemTicketRules) {
//            System.out.println(itemTicketRule);
//            String[] split = itemTicketRule.get(0).split("; ");
//            System.out.println(split);
//        }

        //evaluateSupportF1(0);
        //evaluateConfidenceF1(0.08);
    }


}
