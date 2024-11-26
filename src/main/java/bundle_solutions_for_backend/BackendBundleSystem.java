package bundle_solutions_for_backend;

import bundle_system.memory_query_system.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import xml_parser.*;
import xml_parser.XMLReader;

import javax.xml.parsers.*;
import javax.xml.xpath.*;
import java.io.*;
import java.util.*;

import static bundle_system.io.SharedAttributes.*;

public class BackendBundleSystem {

    public static void test() throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        XMLReader xmlReader = new XMLReader();
        XMLParser xmlParser = new XMLParser();
        //RulesStorage rulesStorage = QuickQuery.initRulesStorageByType(MEAL);
        Element root = xmlReader.read();
        //由ns计算时间ms
        long start = System.nanoTime();
        Map<String, BundleItem> stringBundleItemMap = xmlParser.parseComboSource(root);
        System.out.println("time(ms):" + ((double)(System.nanoTime() - start) )/ 1000000);
        List<Operation> parseMethods = xmlParser.getParseMethods();
        for(int i = 2;i<parseMethods.size();i++){
            parseMethods.get(i).execute(root);
            System.out.println("time(ms):" + ((double)(System.nanoTime() - start) )/ 1000000);
        }



//        for (Map.Entry<String, BundleItem> entry : stringBundleItemMap.entrySet()) {
//            Map<String, String> map = rulesStorage.queryItemAttributes(entry.getValue().getAttributes());
//            for (Map.Entry<String, String> entry1 : map.entrySet()) {
//                System.out.println("key:" + entry1.getKey() + ",value:" + entry1.getValue());
//            }
//        }
//
//
//        rulesStorage.shutdown();

    }

}
