package bundle_solutions_for_backend;

import bundle_system.memory_query_system.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import xml_parser.*;
import xml_parser.XMLReader;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
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
        sortBundleItemMethods.add(BackendBundleSystem::testBundleMeal);
        sortBundleItemMethods.add(BackendBundleSystem::testBundleBaggage);
        sortBundleItemMethods.add(BackendBundleSystem::testBundleInsurance);
        sortBundleItemMethods.add(BackendBundleSystem::testBundleSeat);

        try {
            dBuilder = dbFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 测试Bundle功能，对于原来的doc不修改
     * @param times 测试次数
     */
    public static void test(int times) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException, TransformerException {
        XMLReader xmlReader = new XMLReader();
        XMLParser xmlParser = new XMLParser();
        List<RulesStorage> rulesStorages = QuickQuery.initAllRulesStorage();
        Document originalDoc =xmlReader.read();
        Element root = originalDoc.getDocumentElement();
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
        saveDocument(doc);
        RulesStorage.shutdownAll();
    }

    /**
     * 测试Bundle功能，对于原来的doc修改
     * @param times 测试次数
     */
    public static void test1(int times) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException, TransformerException {
        XMLReader xmlReader = new XMLReader();
        XMLParser xmlParser = new XMLParser();
        List<RulesStorage> rulesStorages = QuickQuery.initAllRulesStorage();
        //模拟输入Document
        Document doc =xmlReader.read();
        Element root = doc.getDocumentElement();
        Element comboWith = doc.createElement("ComboWith");
        //由ns计算时间ms
        long start = System.nanoTime();
        //新建返回的Document部分
        //解析xml文件部分
        List<ParseMethod> parseMethods = xmlParser.getParseMethods();
        Map<String, BundleItem> segTicketMap = xmlParser.parseComboSource(root);
        for (int i = 0; i < times; i++) {
            bundleAllItem(parseMethods, root, segTicketMap, rulesStorages,comboWith,doc);
        }
        xmlParser.renewComboWith(root,comboWith);
        System.out.println("time(ms):" + ((double) (System.nanoTime() - start)) / 1000000 / times);
        saveDocument(doc);
        RulesStorage.shutdownAll();
    }

    /**
     * 将Document保存到文件
     * @param doc 希望保存的Document
     */
    public static void saveDocument(Document doc) throws TransformerException {
        // 指定文件路径
        String filePath = "D:\\programms\\java_projects\\version_control\\output\\test.xml";  // 替换为你想要保存的路径

        // 将Document转换并保存到文件
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(filePath));
        transformer.transform(source, result);
        System.out.println("XML文件已成功保存到: " + filePath);
    }

    /**
     * 获取返回的Document模板（用于非破坏性操作）
     * @param doc 新建的返回的Document
     * @param xmlns xmlns
     * @return 返回的Document模板
     */
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
        // 处理餐食
        Map<String, List<BundleItem>> bundleItems = parseMethods.get(MEAL).execute(root);
        // 得到选座/餐食返回的AncillaryProducts
        Element ancillaryProducts = testBundleMeal(segTicketMap
                , bundleItems, rulesStorages.get(MEAL), null, doc);

        // 处理行李
        bundleItems = parseMethods.get(BAGGAGE).execute(root);
        // 得到行李返回的ancillary0
        Element ancillary0 =sortBundleItemMethods.get(BAGGAGE).execute(segTicketMap, bundleItems
                , rulesStorages.get(BAGGAGE), null ,doc);

        // 处理保险
        bundleItems = parseMethods.get(INSURANCE).execute(root);
        // 得到保险返回的insurance
        Element insurance = sortBundleItemMethods.get(INSURANCE).execute(segTicketMap, bundleItems
                , rulesStorages.get(INSURANCE), null ,doc);

        // 处理选座
        bundleItems = parseMethods.get(SEAT).execute(root);
        // 得到选座返回的ancillary1
        Element ancillary1 = testBundleSeat(segTicketMap, bundleItems
                , rulesStorages.get(SEAT), ancillaryProducts, doc);

        comboWith.appendChild(insurance);
        comboWith.appendChild(ancillary0);
        comboWith.appendChild(ancillary1);
    }

    /**
     * 套餐/行李打包方法（测试版本），因为这两个比较像，所以合并了
     *
     * @param ticketInfo   机票航段 商品键值对
     * @param bundleItems  附加产品所属航段 附加产品键值对
     * @param rulesStorage 附加产品规则存储
     * @param fatherElement     父节点
     * @param doc         最终返回到Document，这里用来创造节点
     */
    public static Element testBundleMeal(Map<String, BundleItem> ticketInfo
            , Map<String, List<BundleItem>> bundleItems
            , RulesStorage rulesStorage,Element fatherElement ,Document doc) {
        Element ancillary = doc.createElement("Ancillary");
        Element boundProducts = doc.createElement("BoundProducts");
        Element ancillaryProducts = doc.createElement("AncillaryProducts");
        ancillary.appendChild(boundProducts);
        boundProducts.appendChild(ancillaryProducts);
        //遍历ticketInfo，得到其中的机票属性
        for (Map.Entry<String, BundleItem> entry : ticketInfo.entrySet()) {
            //根据机票属性查询附加产品规则，得到附加产品属性
            Map<String, String> map = rulesStorage.queryItemAttributes(entry.getValue().getAttributes());
            //根据机票属性查询附加产品航段，得到附加产品航段的商品键值对
            List<BundleItem> bundleItemList = bundleItems.get(entry.getKey());
            //排序
            setPriorityAndSort(map, bundleItemList);
            //将排序好的附加产品添加到节点中
            for(int i = 0 ,size=bundleItemList.size();i<size && i<5;i++) {
                BundleItem bundleItem = bundleItemList.get(i);
                //非破坏性迁移 TODO 确认是否需要非破坏性迁移
                //ancillaryProducts.appendChild(migrateNode(bundleItem.getElement(),doc));
                ancillaryProducts.appendChild(bundleItem.getElement());
            }
        }
        return ancillaryProducts;
    }

    /**
     * 套餐/行李打包方法（测试版本），因为这两个比较像，所以合并了
     *
     * @param ticketInfo   机票航段 商品键值对
     * @param bundleItems  附加产品所属航段 附加产品键值对
     * @param rulesStorage 附加产品规则存储
     * @param fatherElement    fatherElement节点，大多数时候为null，主要是为了作为和餐食在一个父节点下设计的
     * @param doc         输出的Document
     */
    public static Element testBundleBaggage(Map<String, BundleItem> ticketInfo
            , Map<String, List<BundleItem>> bundleItems
            , RulesStorage rulesStorage,Element fatherElement,Document doc) {
        Element ancillary = doc.createElement("Ancillary");
        Element baggage = doc.createElement("Baggage");
        ancillary.appendChild(baggage);
        Element originDestination = doc.createElement("OriginDestination");
        baggage.appendChild(originDestination);
        //遍历ticketInfo，得到其中的机票属性
        for (Map.Entry<String, BundleItem> entry : ticketInfo.entrySet()) {
            //根据机票属性查询附加产品规则，得到附加产品属性
            Map<String, String> map = rulesStorage.queryItemAttributes(entry.getValue().getAttributes());
            //根据机票属性查询附加产品航段，得到附加产品航段的商品键值对
            List<BundleItem> bundleItemList = bundleItems.get(entry.getKey());
            //排序
            setPriorityAndSort(map, bundleItemList);
            //将排序好的附加产品添加到节点中
            for(int i = 0 ,size=bundleItemList.size();i<size && i<5;i++) {
                BundleItem bundleItem = bundleItemList.get(i);
                //非破坏性迁移 TODO 确认是否需要非破坏性迁移
                //originDestination.appendChild(migrateNode(bundleItem.getElement(),doc));
                //破坏性迁移（效率更高）
                originDestination.appendChild(bundleItem.getElement());
            }
        }
        return ancillary;
    }


    /**
     * 套餐/行李打包方法（测试版本），因为这两个比较像，所以合并了
     *
     * @param ticketInfo   机票航段 商品键值对
     * @param bundleItems  附加产品所属航段 附加产品键值对，这里的键为 航段号|subtype
     * @param rulesStorage 附加产品规则存储
     * @param fatherElement    fatherElement节点，大多数时候为null，主要是为了选座和餐食在一个父节点下设计的
     * @param doc         输出的Document
     */
    public static Element testBundleSeat(Map<String, BundleItem> ticketInfo
            , Map<String, List<BundleItem>> bundleItems
            , RulesStorage rulesStorage,Element fatherElement,Document doc) {
        Map<String, Map<String, String>> segAttributesmap = new HashMap<>();
        //遍历ticketInfo，得到其中的机票属性
        for (Map.Entry<String, BundleItem> entry : ticketInfo.entrySet()) {
            //根据机票属性查询附加产品规则，得到附加产品属性
            Map<String, String> map = rulesStorage.queryItemAttributes(entry.getValue().getAttributes());
            segAttributesmap.put(entry.getKey(), map);
        }
        //遍历bundleItems，得到其中的附加产品属性
        for (Map.Entry<String, List<BundleItem>> entry : bundleItems.entrySet())
        {
            //根据机票属性查询附加产品航段，得到附加产品航段的商品键值对
            List<BundleItem> bundleItemList = entry.getValue();
            String segRef = entry.getKey().split("\\|")[0];
            //排序
            setPriorityAndSort(segAttributesmap.get(segRef), bundleItemList);
            //将排序好的附加产品添加到节点中
            for(int i = 0 ,size=bundleItemList.size();i<size && i<5;i++) {
                BundleItem bundleItem = bundleItemList.get(i);
                //非破坏性迁移 TODO 确认是否需要非破坏性迁移
                //originDestination.appendChild(migrateNode(bundleItem.getElement(),doc));
                //破坏性迁移（效率更高）
                fatherElement.appendChild(buildSeatElement(bundleItem, doc));
            }
        }
        return (Element) fatherElement.getParentNode().getParentNode();
    }

    public static Element buildSeatElement(BundleItem bundleItem, Document doc) {
        //建立各级节点
        Element ancillary = doc.createElement("Ancillary");
        Element prices = doc.createElement("Prices");
        Element price = doc.createElement("Price");
        Element total = doc.createElement("Total");
        
        //为节点属性赋值
        Map<String, String> xmlAttributes = bundleItem.getXmlAttributes();
        /*结构示例
        <Ancillary Tag="Recommended" SubType="85" SegmentRef="2" SupplierProductCode="L" Type="Seat">
						<Prices>
							<Price>
								<Total Amount="0.0" CurrencyCode="CNY"/>
							</Price>
						</Prices>
					</Ancillary>
         */
        ancillary.setAttribute("Tag", xmlAttributes.get("Tag"));
        ancillary.setAttribute("SubType", xmlAttributes.get("SubType"));
        ancillary.setAttribute("SegmentRef", xmlAttributes.get("SegmentRef"));
        ancillary.setAttribute("SupplierProductCode", xmlAttributes.get("SupplierProductCode"));
        ancillary.setAttribute("Type", "Seat");
        total.setAttribute("Amount", xmlAttributes.get("Amount"));
        total.setAttribute("CurrencyCode", xmlAttributes.get("CurrencyCode"));

        //建立节点关系
        ancillary.appendChild(prices);
        prices.appendChild(price);
        price.appendChild(total);
        return ancillary;
    }

    /**
     * 保险打包方法（测试版本）
     *
     * @param ticketInfo   机票航段 商品键值对
     * @param bundleItems  保险所属航段 保险键值对
     * @param rulesStorage 保险规则存储
     * @return 返回打包后排序好的结果对应Insurance节点
     */
    public static Element testBundleInsurance(Map<String, BundleItem> ticketInfo
            , Map<String, List<BundleItem>> bundleItems
            , RulesStorage rulesStorage,Element fatherElement,Document doc) {
        Element insurance = doc.createElement("Insurance");
        //遍历ticketInfo，得到其中的机票属性
        for (Map.Entry<String, BundleItem> entry : ticketInfo.entrySet()) {
            //根据机票属性查询附加产品规则，得到附加产品属性
            Map<String, String> map = rulesStorage.queryItemAttributes(entry.getValue().getAttributes());
            //根据机票属性查询附加产品航段，得到附加产品航段的商品键值对
            List<BundleItem> bundleItemList = bundleItems.get(null);
            //排序
            setPriorityAndSort(map, bundleItemList);
            for(int i = 0 ,size=bundleItemList.size();i<size && i<5;i++) {
                BundleItem bundleItem = bundleItemList.get(i);
                //非破坏性迁移 TODO 确认是否需要非破坏性迁移
                //insurance.appendChild(migrateNode(bundleItem.getElement(),doc));
                //破坏性迁移（效率更高）
                insurance.appendChild(bundleItem.getElement());
            }
            break;
        }
        return insurance;
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

    /**
     * 将一个节点从一个文档迁移到另一个文档。
     *
     * @param node 要迁移的节点
     * @param targetDoc 目标文档
     * @return 迁移后的节点
     */
    private static Node migrateNode(Node node, Document targetDoc) {
        // 根据节点类型创建新的节点
        Node newNode;
        switch (node.getNodeType()) {
            case Node.ELEMENT_NODE:
                // 在目标文档中创建一个新的Element
                Element element = (Element) node;
                newNode = targetDoc.createElement(element.getTagName());

                // 复制属性
                NamedNodeMap attributes = element.getAttributes();
                for (int i = 0; i < attributes.getLength(); i++) {
                    Attr attribute = (Attr) attributes.item(i);
                    ((Element) newNode).setAttribute(attribute.getName(), attribute.getValue());
                }
                break;
            case Node.TEXT_NODE:
                // 创建一个新的文本节点
                Text text = (Text) node;
                newNode = targetDoc.createTextNode(text.getData());
                break;
            case Node.COMMENT_NODE:
                // 创建一个新的注释节点
                Comment comment = (Comment) node;
                newNode = targetDoc.createComment(comment.getData());
                break;
            case Node.CDATA_SECTION_NODE:
                // 创建一个新的CDATA节点
                CDATASection cdata = (CDATASection) node;
                newNode = targetDoc.createCDATASection(cdata.getData());
                break;
            default:
                // 对于其他类型的节点，直接返回null或抛出异常
                throw new IllegalArgumentException("Unsupported node type: " + node.getNodeType());
        }

        // 复制子节点
        NodeList childNodes = node.getChildNodes();
        for (int j = 0; j < childNodes.getLength(); j++) {
            Node child = childNodes.item(j);
            Node migratedChild = migrateNode(child, targetDoc);
            if (migratedChild != null) {
                newNode.appendChild(migratedChild);
            }
        }

        return newNode;
    }
}
