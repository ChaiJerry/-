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
        List<RulesStorage> rulesStorages = QuickQuery.initAllRulesStorage();
        Element root = xmlReader.read();
        //由ns计算时间ms
        long start = System.nanoTime();
        Map<String, BundleItem> stringBundleItemMap = xmlParser.parseComboSource(root);
        System.out.println("time(ms):" + ((double)(System.nanoTime() - start) )/ 1000000);
        List<Operation> parseMethods = xmlParser.getParseMethods();
        for(int i = 2;i<parseMethods.size();i++){
            Map<String, List<BundleItem>> bundleItems = parseMethods.get(i).execute(root);
            List<BundleItem> sortedBundleList = testBundleMeal(stringBundleItemMap, bundleItems, rulesStorages.get(i));
            System.out.println("time(ms):" + ((double)(System.nanoTime() - start) )/ 1000000);
            for (BundleItem bundleItem : sortedBundleList) {
                System.out.println(bundleItem);
            }
            break;
        }



//        for (Map.Entry<String, BundleItem> entry : stringBundleItemMap.entrySet()) {
//            Map<String, String> map = rulesStorage.queryItemAttributes(entry.getValue().getAttributes());
//            for (Map.Entry<String, String> entry1 : map.entrySet()) {
//                System.out.println("key:" + entry1.getKey() + ",value:" + entry1.getValue());
//            }
//        }


        RulesStorage.shutdownAll();

    }

    public static List<BundleItem> testBundleMeal(Map<String, BundleItem> ticketInfo
            ,Map<String, List<BundleItem>> bundleItems,RulesStorage rulesStorage )  {
        //遍历ticketInfo，得到其中的机票属性
        List<BundleItem> sortedBundleItemList = new ArrayList<>();
        for (Map.Entry<String, BundleItem> entry : ticketInfo.entrySet()) {
            Map<String, String> map = rulesStorage.queryItemAttributes(entry.getValue().getAttributes());
            List<BundleItem> bundleItemList = bundleItems.get(entry.getKey());
            sort(map,bundleItemList);
            sortedBundleItemList.addAll(bundleItemList);
        }
        return sortedBundleItemList;
    }

    public static void sort(Map<String, String> map,List<BundleItem> bundleItemList){
        for(BundleItem bundleItem:bundleItemList){
            bundleItem.setPriority(map);
        }
        Collections.sort(bundleItemList);
    }

}
