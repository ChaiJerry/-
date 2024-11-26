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
        for(int i = MEAL;i<parseMethods.size()-1;i++){
            Map<String, List<BundleItem>> bundleItems = parseMethods.get(i).execute(root);
            List<BundleItem> sortedBundleList = testBundleMealOrBaggage(stringBundleItemMap, bundleItems, rulesStorages.get(i));
            System.out.println("time(ms):" + ((double)(System.nanoTime() - start) )/ 1000000);
            for (BundleItem bundleItem : sortedBundleList) {
                System.out.println(bundleItem);
            }
        }

        RulesStorage.shutdownAll();

    }

    /**
     * 套餐/行李打包方法（测试版本），因为这两个比较像，所以合并了
     * @param ticketInfo 机票航段 商品键值对
     * @param bundleItems 附加产品所属航段 附加产品键值对
     * @param rulesStorage 附加产品规则存储
     * @return 返回打包后排序好的结果
     */
    public static List<BundleItem> testBundleMealOrBaggage(Map<String, BundleItem> ticketInfo
            , Map<String, List<BundleItem>> bundleItems, RulesStorage rulesStorage )  {
        //遍历ticketInfo，得到其中的机票属性
        List<BundleItem> sortedBundleItemList = new ArrayList<>();
        for (Map.Entry<String, BundleItem> entry : ticketInfo.entrySet()) {
            //根据机票属性查询附加产品规则，得到附加产品属性
            Map<String, String> map = rulesStorage.queryItemAttributes(entry.getValue().getAttributes());
            //根据机票属性查询附加产品航段，得到附加产品航段的商品键值对
            List<BundleItem> bundleItemList = bundleItems.get(entry.getKey());
            //排序
            sort(map,bundleItemList);
            //将排序好的附加产品添加到结果中
            sortedBundleItemList.addAll(bundleItemList);
        }
        return sortedBundleItemList;
    }

    /**
     * 给附加产品排序的方法
     * @param map 推荐的附加产品属性键值对
     * @param bundleItemList 附加产品键列表
     */
    public static void sort(Map<String, String> map,List<BundleItem> bundleItemList){
        for(BundleItem bundleItem:bundleItemList){
            bundleItem.setPriority(map);
        }
        Collections.sort(bundleItemList);
    }

}
