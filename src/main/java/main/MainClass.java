package main;

import bundle_service_for_backend.*;
import bundle_system.io.sql.*;
import org.xml.sax.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.xpath.*;
import java.io.*;
import java.sql.*;

public class MainClass {
    public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException, XPathExpressionException, TransformerException, SQLException {
//        QuickQuery quickQuery = new QuickQuery();
//        quickQuery.test(INSURANCE);
        SQLUtils sqlUtils = new SQLUtils();
//       sqlUtils.renewRulesTables();
//
//        CSVFileIO csvFileIO = new CSVFileIO(PATH_T,null,PATH_M,PATH_B, PATH_I, PATH_S);
//        List<List<String>> rules = new ArrayList<>();
//        List<List<String>> listOfAttributeList = csvFileIO.singleTypeCsv2ListOfAttributeList(MEAL);
//        API.associationRulesMining(listOfAttributeList
//                , false, true
//                , null, rules, 0.08, 0);
//        for (List<String> rule : rules) {
//            System.out.println("rule: " + rule);
//        }
//
//        sqlUtils.insertRules(MEAL,rules,1);
        BackendBundleSystem backendBundleSystem = new BackendBundleSystem(8,sqlUtils,4);
        backendBundleSystem.test();

//        sqlUtils.renewTables();
//        sqlUtils.insertTrainDataRecord("1.csv","2023...",MEAL);
//        QuickQuery.initAllRulesStorage();
        //SQLUtils.createRuleTableForMemQuery();
//        XMLReader xmlReader = new XMLReader();
//        XMLParser xmlParser = new XMLParser();
//        List<BundleItem> bundleItems = xmlParser.parseMeal(xmlReader.read());
//        for (BundleItem bundleItem : bundleItems) {
//            System.out.println("bundleItem: " + bundleItem);
//        }
//        BackendBundleSystem backendBundleSystem = new BackendBundleSystem();
//        backendBundleSystem.test();

    }


}
