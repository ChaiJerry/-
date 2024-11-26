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

    // 创建DocumentBuilderFactory和DocumentBuilder
    static DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    static DocumentBuilder dBuilder;

    static List<SortBundleItemMethod> sortBundleItemMethods = new ArrayList<>();

    static {
        sortBundleItemMethods.add(null);
        sortBundleItemMethods.add(null);
        sortBundleItemMethods.add(BackendBundleSystem::testBundleMealOrBaggage);
        sortBundleItemMethods.add(BackendBundleSystem::testBundleMealOrBaggage);
        sortBundleItemMethods.add(BackendBundleSystem::testBundleInsurance);
        try {
            dBuilder = dbFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public static void test(int times) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        XMLReader xmlReader = new XMLReader();
        XMLParser xmlParser = new XMLParser();
        List<RulesStorage> rulesStorages = QuickQuery.initAllRulesStorage();
        Element root = xmlReader.read();
        //由ns计算时间ms
        long start = System.nanoTime();
        //新建返回的Document部分
        Document doc = dBuilder.newDocument();
        Element comboWith = getReturnDocTemplate(doc, root.getAttribute("xmlns"));
        //解析xml文件部分
        List<ParseMethod> parseMethods = xmlParser.getParseMethods();
        Map<String, BundleItem> segTicketMap = xmlParser.parseComboSource(root);
        for (int i = 0; i < times; i++) {
            bundleAllItem(parseMethods, root, segTicketMap, rulesStorages,comboWith,doc);
        }
        System.out.println("time(ms):" + ((double) (System.nanoTime() - start)) / 1000000 / times);

        RulesStorage.shutdownAll();

    }

    public static Element getReturnDocTemplate(Document doc,String xmlns) {
        Element rootElement = doc.createElement("OJ_ComboSearchRS");
        rootElement.setAttribute("xmlns", xmlns);
        doc.appendChild(rootElement);
        Element comboWith = doc.createElement("ComboWith");
        rootElement.appendChild(comboWith);
        return comboWith;
    }

    private static void bundleAllItem(List<ParseMethod> parseMethods, Element root
            , Map<String, BundleItem> segTicketMap, List<RulesStorage> rulesStorages
            , Element comboWith,Document doc) throws XPathExpressionException {
        for (int i = MEAL; i < parseMethods.size(); i++) {
            Map<String, List<BundleItem>> bundleItems = parseMethods.get(i).execute(root);
            sortBundleItemMethods.get(i).execute(segTicketMap
                    , bundleItems, rulesStorages.get(i), comboWith ,doc);

//            for (BundleItem bundleItem : sortedBundleList) {
//                System.out.println(bundleItem);
//            }
        }
    }

    /**
     * 套餐/行李打包方法（测试版本），因为这两个比较像，所以合并了
     *
     * @param ticketInfo   机票航段 商品键值对
     * @param bundleItems  附加产品所属航段 附加产品键值对
     * @param rulesStorage 附加产品规则存储
     * @param comboWith    ComboWith节点
     * @param doc         输出的Document
     */
    public static void testBundleMealOrBaggage(Map<String, BundleItem> ticketInfo
            , Map<String, List<BundleItem>> bundleItems
            , RulesStorage rulesStorage,Element comboWith,Document doc) {
        //遍历ticketInfo，得到其中的机票属性
        for (Map.Entry<String, BundleItem> entry : ticketInfo.entrySet()) {
            //根据机票属性查询附加产品规则，得到附加产品属性
            Map<String, String> map = rulesStorage.queryItemAttributes(entry.getValue().getAttributes());
            //根据机票属性查询附加产品航段，得到附加产品航段的商品键值对
            List<BundleItem> bundleItemList = bundleItems.get(entry.getKey());
            //排序
            setPriorityAndSort(map, bundleItemList);
            //将排序好的附加产品添加到节点中
        }
    }

    /**
     * 保险打包方法（测试版本）
     *
     * @param ticketInfo   机票航段 商品键值对
     * @param bundleItems  保险所属航段 保险键值对
     * @param rulesStorage 保险规则存储
     * @return 返回打包后排序好的结果
     */
    public static void testBundleInsurance(Map<String, BundleItem> ticketInfo
            , Map<String, List<BundleItem>> bundleItems
            , RulesStorage rulesStorage,Element comboWith,Document doc) {
        Element insurance = doc.createElement("Insurance");
        comboWith.appendChild(insurance);
        //遍历ticketInfo，得到其中的机票属性
        for (Map.Entry<String, BundleItem> entry : ticketInfo.entrySet()) {
            //根据机票属性查询附加产品规则，得到附加产品属性
            Map<String, String> map = rulesStorage.queryItemAttributes(entry.getValue().getAttributes());
            //根据机票属性查询附加产品航段，得到附加产品航段的商品键值对
            List<BundleItem> bundleItemList = bundleItems.get(null);
            //排序
            setPriorityAndSort(map, bundleItemList);
            for(BundleItem bundleItem : bundleItemList) {
                insurance.appendChild(bundleItem.getElement());
            }
            break;
        }
    }

    /**
     * 给附加产品排序的方法
     *
     * @param map            推荐的附加产品属性键值对
     * @param bundleItemList 附加产品键列表
     */
    public static void setPriorityAndSort(Map<String, String> map, List<BundleItem> bundleItemList) {
        for (BundleItem bundleItem : bundleItemList) {
            bundleItem.setPriority(map);
        }
        Collections.sort(bundleItemList);
    }

}
