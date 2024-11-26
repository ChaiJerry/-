package bundle_system.main_class;

import bundle_solutions_for_backend.*;
import bundle_system.memory_query_system.*;
import org.xml.sax.*;
import xml_parser.*;
import xml_parser.XMLReader;

import javax.xml.parsers.*;
import javax.xml.xpath.*;
import java.io.*;
import java.sql.*;
import java.util.*;

import static bundle_system.io.SharedAttributes.*;

public class Main {
    public static void main(String[] args) throws IOException, SQLException, ParserConfigurationException, SAXException, XPathExpressionException {
//        QuickQuery quickQuery = new QuickQuery();
//        quickQuery.test(SEAT);
        BackendBundleSystem.test();

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
