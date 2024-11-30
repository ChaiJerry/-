package bundle_service_for_backend;

import bundle_service_for_backend.xml_parser.*;
import bundle_service_for_backend.xml_parser.XMLReader;
import bundle_system.io.*;
import bundle_system.io.sql.*;
import bundle_system.memory_query_system.*;
import org.w3c.dom.*;
import org.xml.sax.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import javax.xml.xpath.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import static bundle_system.api.API.*;
import static bundle_system.io.SharedAttributes.*;
import static bundle_system.memory_query_system.QuickQuery.*;

public class BackendBundleSystem {

    // 创建DocumentBuilderFactory和DocumentBuilder
    static DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    static DocumentBuilder dBuilder;
    private static final XPathFactory xPathfactory = XPathFactory.newInstance();

    static List<SortBundleItemMethod> sortBundleItemMethods = new ArrayList<>();

    static {
        sortBundleItemMethods.add(null);
        sortBundleItemMethods.add(null);
        sortBundleItemMethods.add(BundleMethods::testBundleMeal);
        sortBundleItemMethods.add(BundleMethods::testBundleBaggage);
        sortBundleItemMethods.add(BundleMethods::testBundleInsurance);
        sortBundleItemMethods.add(BundleMethods::testBundleSeat);

        try {
            dBuilder = dbFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    private final int poolSize; // 线程池的大小
    private final ExecutorService executorService;
    private List<RulesStorage> rulesStorages;
    private final SQLUtils sqlUtils;
    private final CSVFileIO fileIO;

    /**
     * 请勿使用默认构造函数，请使用带参数的构造函数，这只是用来测试的
     */
    public BackendBundleSystem() {
        this.poolSize = 8;
        executorService = Executors.newFixedThreadPool(poolSize);
        sqlUtils = new SQLUtils();
        fileIO = SharedAttributes.fileIO;
        try {
            rulesStorages = initAllRulesStorage(null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 当想要从数据库中读取训练然后保存到对应的数据库中就用这个构造函数
     * @param poolSize 线程池的大小
     * @param sqlUtils 数据库操作对象
     * @param trainId 希望作为知识库的训练数据的id
     */
    public BackendBundleSystem(int poolSize,SQLUtils sqlUtils,String trainId) {
        this.poolSize = poolSize;
        this.sqlUtils = sqlUtils;
        executorService = Executors.newFixedThreadPool(poolSize);
        try {
            rulesStorages = initAllRulesStorage(trainId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        fileIO = null;
    }

    /**
     * 当想要若是数据库中没有读到数据时就从csv文件中读取训练然后保存到对应的数据库中就用这个构造函数
     * @param poolSize 线程池的大小
     * @param fileIO csv文件操作对象
     * @param sqlUtils 数据库操作对象
     * @param trainId 训练数据的id
     */
    public BackendBundleSystem(int poolSize,CSVFileIO fileIO,SQLUtils sqlUtils, String trainId) {
        this.poolSize = poolSize;
        executorService = Executors.newFixedThreadPool(poolSize);
        this.sqlUtils = sqlUtils;
        this.fileIO = fileIO;
        try {
            rulesStorages = initAllRulesStorage(trainId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * 当想要直接csv文件中训练然后加载到内存就用这个构造函数
     * @param poolSize 线程池的大小
     * @param fileIO csv文件操作对象
     */
    public BackendBundleSystem(int poolSize,CSVFileIO fileIO) {
        this.sqlUtils = null;
        this.fileIO = fileIO;
        this.poolSize = poolSize;
        executorService = Executors.newFixedThreadPool(poolSize);
        try {
            rulesStorages = initAllRulesStorage(null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public Document submitBundleTask(Document doc) {
        // 提交查询任务到线程池
        Future<?> future = executorService.submit(new BundleTask(doc,rulesStorages));
        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return doc;
    }

    public Document submitQueryTask(Document doc) {
        // 提交查询任务到线程池
        Future<?> future = executorService.submit(new QueryTask(doc,rulesStorages));
        try {
            future.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return doc;
    }

    public void submitBundleTasks(List<Document> docs) {
        // 提交查询任务到线程池
        List<Future<?>> futures = new ArrayList<>();
        for (Document doc : docs) {
            futures.add(executorService.submit(new BundleTask(doc,rulesStorages)));
        }
        try {
            for (Future<?> future : futures) {
                future.get();
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    /**
     * 测试多线程，会对于原来的doc修改
     *
     * @param num 测试次数
     */
    public void test3(int num) throws ParserConfigurationException, IOException, SAXException {
        //模拟输入Document
        XMLReader xmlReader = new XMLReader();
        long sum = 0;
        List<Document> docs = new ArrayList<>();
        System.out.println("正在模拟输入文档，数量：" + num);
        for (int i = 0; i < num; i++) {
            Document doc = xmlReader.read();
            docs.add(doc);
        }
        System.out.println("文档输入完成，开始打包");
        long start = System.nanoTime();
        submitBundleTasks(docs);
        long end = System.nanoTime();
        sum += end - start;
        System.out.println("完成，平均耗时time(ms):" + (sum) / 1000000 / num);
        shutdownAll();
    }

    /**
     * 测试多线程，会对于原来的doc修改，最后保存
     */
    public void test3() throws ParserConfigurationException, IOException, SAXException, XPathExpressionException, TransformerException {
        //模拟输入Document
        XMLReader xmlReader = new XMLReader();
        long start = System.nanoTime();
        Document doc;
        doc = xmlReader.read();
        submitBundleTask(doc);
        saveDocument(doc,"D:\\programms\\java_projects\\version_control\\output\\test1.xml");
        System.out.println("time(ms):" + ((double) (System.nanoTime() - start)) / 1000000);
        doc = xmlReader.read();
        submitQueryTask(doc);
        saveDocument(doc,"D:\\programms\\java_projects\\version_control\\output\\test2.xml");
        shutdownAll();
    }


    /**
     * 将Document保存到文件
     *
     * @param doc 希望保存的Document
     */
    public static void saveDocument(Document doc,String filePath) throws TransformerException {
        // 将Document转换并保存到文件
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(filePath));
        transformer.transform(source, result);
        System.out.println("XML文件已成功保存到: " + filePath);
    }

    /**
     * 将Document保存到文件
     *
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
     *
     * @param doc   新建的返回的Document
     * @param xmlns xmlns
     * @return 返回的Document模板
     */
    public static Element getReturnDocTemplate(Document doc, String xmlns) {
        Element rootElement = doc.createElement("OJ_ComboSearchRS");
        rootElement.setAttribute("xmlns", xmlns);
        doc.appendChild(rootElement);
        Element comboWith = doc.createElement("ComboWith");
        rootElement.appendChild(comboWith);
        return comboWith;
    }

    private static void bundleAllItem(List<ParseMethod> parseMethods, Element root
            , Map<String, BundleItem> segTicketMap, List<RulesStorage> rulesStorages
            , Element comboWith, Document doc) throws XPathExpressionException {
        // 处理餐食
        Map<String, List<BundleItem>> bundleItems = parseMethods.get(MEAL).execute(root);
        // 得到选座/餐食返回的AncillaryProducts
        Element ancillaryProducts = BundleMethods.testBundleMeal(segTicketMap
                , bundleItems, rulesStorages.get(MEAL), null, doc);

        // 处理行李
        bundleItems = parseMethods.get(BAGGAGE).execute(root);
        // 得到行李返回的ancillary0
        Element ancillary0 = sortBundleItemMethods.get(BAGGAGE).execute(segTicketMap, bundleItems
                , rulesStorages.get(BAGGAGE), null, doc);

        // 处理保险
        bundleItems = parseMethods.get(INSURANCE).execute(root);
        // 得到保险返回的insurance
        Element insurance = sortBundleItemMethods.get(INSURANCE).execute(segTicketMap, bundleItems
                , rulesStorages.get(INSURANCE), null, doc);

        // 处理选座
        bundleItems = parseMethods.get(SEAT).execute(root);
        // 得到选座返回的ancillary1
        Element ancillary1 = BundleMethods.testBundleSeat(segTicketMap, bundleItems
                , rulesStorages.get(SEAT), ancillaryProducts, doc);

        comboWith.appendChild(insurance);
        comboWith.appendChild(ancillary0);
        comboWith.appendChild(ancillary1);
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
     * 给附加产品排序的方法
     *
     * @param map            推荐的附加产品属性键值对
     * @param bundleItemList 附加产品键列表
     */
    public static void setPriorityAndSort(Map<String, AttrValueConfidencePriority> map, List<BundleItem> bundleItemList) {
        for (BundleItem bundleItem : bundleItemList) {
            bundleItem.setPriority(map);
        }
        Collections.sort(bundleItemList);
    }

    /**
     * 给附加产品排序的方法，但这个更高级一点，当遇到数值类型字符串时
     * 会解析后使用负指数取数值之间的差值来排序
     * @param map            推荐的附加产品属性键值对
     * @param bundleItemList 附加产品键列表
     */
    public static void setPriorityAndSortWithNumParse(Map<String, AttrValueConfidencePriority> map, List<BundleItem> bundleItemList) {
        for (BundleItem bundleItem : bundleItemList) {
            bundleItem.setPriorityWithNumParse(map);
        }
        Collections.sort(bundleItemList);
    }

    /**
     * 初始化打包系统实例中所有规则存储的方法
     * @return List<RulesStorage>
     * @throws IOException IO异常
     */
    private List<RulesStorage> initAllRulesStorage(String trainId) throws IOException {
        List<RulesStorage> rulesStorages = new ArrayList<>();
        //跳过机票标号
        rulesStorages.add(null);
        //跳过酒店品类（没有使用）
        rulesStorages.add(null);
        boolean autoSave = trainId!= null && trainId.isEmpty();
        for(int type = 2; type < SharedAttributes.getFullNames().length; type++) {
            List<List<String>> rules;
            if(sqlUtils != null && trainId!=null){
                rules = getRulesFromDB(type, trainId);
                if(rules==null){
                    rules = getRulesFromCSVFile(type,trainId,autoSave);
                }
            }else{
                rules = getRulesFromCSVFile(type,trainId,autoSave);
            }
            RulesStorage rulesStorage = RulesStorage.initRulesStorageByType(type,rules);
            rulesStorages.add(rulesStorage);
        }
        return rulesStorages;
    }

    private List<List<String>> getRulesFromCSVFile(int  type) throws IOException {
        List<List<String>> itemTicketRules = new ArrayList<>();
        //否则，进行训练
        List<List<String>> listOfAttributeList = fileIO.singleTypeCsv2ListOfAttributeList(type);
        associationRulesMining(listOfAttributeList, false
                    , true, null, itemTicketRules
                    , 0.08, 0);
        return itemTicketRules;
    }

    public List<List<String>> getRulesFromCSVFile(int type,String trainId,boolean autoSave) throws IOException {
        List<List<String>> itemTicketRules = getRulesFromCSVFile(type);
        if(autoSave && sqlUtils!=null) {
            try {
                sqlUtils.storeRules(type, itemTicketRules, trainId);
            } catch (Exception e) {
                System.out.println("自动存储规则失败");
            }
        }
        return itemTicketRules;
    }
    public List<List<String>> getRulesFromDB(int  type,String trainId) {
        List<List<String>> itemTicketRules;
        try {
            //如果已经存在，则直接加载
            itemTicketRules = sqlUtils.loadRules(type, trainId);
        }catch (Exception e) {
            return null;
        }
        return itemTicketRules;
    }






    /**
     * 将一个节点从一个文档安全迁移到另一个文档。
     *
     * @param node      要迁移的节点
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

    public void shutdownAll() {
        executorService.shutdown();
        RulesStorage.shutdownAll();
    }
}
