package main;

import bundle_service_for_backend.*;
import bundle_system.memory_query_system.*;
import bundle_system.api.*;
import bundle_system.io.*;
import bundle_system.data_generating_system.*;
import bundle_system.db_query_system.*;
import bundle_system.data_processer.*;
import org.xml.sax.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.xpath.*;
import java.io.*;
import java.sql.*;

import static bundle_system.io.SharedAttributes.*;

public class MainClass {
    public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException, XPathExpressionException, TransformerException {
//        QuickQuery quickQuery = new QuickQuery();
//        quickQuery.test(MEAL);
        BackendBundleSystem backendBundleSystem = new BackendBundleSystem();
        backendBundleSystem.test1(1);
//        SQLUtils sqlUtils = new SQLUtils();
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
