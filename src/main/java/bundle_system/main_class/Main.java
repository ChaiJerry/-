package bundle_system.main_class;

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
//        quickQuery.test(HOTEL);
        XMLReader xmlReader = new XMLReader();
        List<BundleItem> bundleItems = xmlReader.getInsurances(xmlReader.read());
        for (BundleItem bundleItem : bundleItems) {
            System.out.println("bundleItem: " + bundleItem);
        }
    }


}