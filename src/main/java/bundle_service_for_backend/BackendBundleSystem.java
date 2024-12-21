package bundle_service_for_backend;

import bundle_service_for_backend.xml_parser.*;
import bundle_service_for_backend.xml_parser.XMLIO;
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
import java.util.logging.*;

import static bundle_system.api.API.*;


public class BackendBundleSystem {

    // 创建DocumentBuilderFactory和DocumentBuilder
    static DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    static DocumentBuilder dBuilder;

    static XMLIO xmlIO;
    static Logger logger = Logger.getLogger("BackendBundleSystem");

    static {
        try {
            xmlIO = new XMLIO();
            dBuilder = dbFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            logger.info("初始化DocumentBuilder失败");
            logger.info(e.getMessage());
        }
    }

    private final int poolSize; // 线程池的大小
    private final ExecutorService executorService;

    private final List<RulesStorage> rulesStorages;
    private final SQLUtils sqlUtils;
    private final CSVFileIO fileIO;

    /**
     * 请勿使用默认构造函数，请使用带参数的构造函数，这只是用来测试的
     */
    public BackendBundleSystem() throws IOException {
        this.poolSize = 8;
        executorService = Executors.newFixedThreadPool(poolSize);
        sqlUtils = new SQLUtils();
        fileIO = SharedAttributes.fileIO;
        rulesStorages = initAllRulesStorageForTests(null);
    }

    /**
     * 当想要从数据库中读取训练然后保存到对应的数据库中就用这个构造函数
     *
     * @param poolSize 线程池的大小
     * @param sqlUtils 数据库操作对象
     * @param trainId  希望作为知识库的训练数据的id
     */
    public BackendBundleSystem(int poolSize, SQLUtils sqlUtils, Integer trainId) {
        this.poolSize = poolSize;
        this.sqlUtils = sqlUtils;
        executorService = Executors.newFixedThreadPool(poolSize);

        rulesStorages = initAllRulesStorageFromDB(trainId);

        fileIO = null;
    }

    /**
     * 从字符串中读取xml文件，然后提交打包任务到线程池的方法
     * @param xmlStr 查询的xml字符串
     * @return 结果xml字符串
     */
    public String submitBundleTaskInStr(String xmlStr) throws Exception {
        Document doc = xmlIO.stringToDocument(xmlStr);
        // 提交查询任务到线程池
        Future<?> future = executorService.submit(new BundleTask(doc, rulesStorages));
        future.get();
        return xmlIO.documentToString(doc);
    }

    public Document submitBundleTask(Document doc) throws ExecutionException, InterruptedException {
        // 提交查询任务到线程池
        Future<?> future = executorService.submit(new BundleTask(doc, rulesStorages));
        future.get();
        return doc;
    }

    public String submitQueryTaskInStr(String xmlStr) throws Exception {
        Document doc = xmlIO.stringToDocument(xmlStr);
        // 提交查询任务到线程池
        Future<?> future = executorService.submit(new QueryTask(doc, rulesStorages));
        future.get();
        return xmlIO.documentToString(doc);
    }

    public Document submitQueryTask(Document doc) throws ExecutionException, InterruptedException {
        // 提交查询任务到线程池
        Future<?> future = executorService.submit(new QueryTask(doc, rulesStorages));
        future.get();
        return doc;
    }

    public void submitBundleTasks(List<Document> docs) throws ExecutionException, InterruptedException {
        // 提交查询任务到线程池
        List<Future<?>> futures = new ArrayList<>();
        for (Document doc : docs) {
            futures.add(executorService.submit(new BundleTask(doc, rulesStorages)));
        }

        for (Future<?> future : futures) {
            future.get();
        }

    }

    /**
     * 测试多线程，会对于原来的doc修改
     *
     * @param num 测试次数
     */
    public void test(int num) throws ParserConfigurationException, IOException, SAXException, ExecutionException, InterruptedException {
        //模拟输入Document
        XMLIO xmlio = new XMLIO();
        long sum = 0;
        List<Document> docs = new ArrayList<>();
        String msg0 = "正在模拟输入文档，数量：" + num;
        logger.info(msg0);
        for (int i = 0; i < num; i++) {
            Document doc = xmlio.readTest2();
            docs.add(doc);
        }
        logger.info("文档输入完成，开始打包");
        long start = System.nanoTime();
        submitBundleTasks(docs);
        long end = System.nanoTime();
        sum += end - start;
        String msg1 = "完成，平均耗时time(ms):" + (sum) / 1000000 / num;
        logger.info(msg1);
        shutdownAll();
    }

    /**
     * 测试多线程，会对于原来的doc修改，最后保存
     */
    public void test() throws ParserConfigurationException, IOException, SAXException, XPathExpressionException, TransformerException, ExecutionException, InterruptedException {
        //模拟输入Document
        XMLIO xmlio = new XMLIO();
        long start = System.nanoTime();
        Document doc;
        doc = xmlio.readTest2();
        submitBundleTask(doc);
        saveDocument(doc, "D:\\programms\\java_projects\\version_control\\output\\test2.xml");
        String testMsg = "time(ms):" + ((double) (System.nanoTime() - start)) / 1000000;
        logger.info(testMsg);
        doc = xmlio.readTest1();
        submitQueryTask(doc);
        saveDocument(doc, "D:\\programms\\java_projects\\version_control\\output\\test1.xml");
        shutdownAll();
    }

    /**
     * 将Document保存到文件（用于测试）
     *
     * @param doc 希望保存的Document
     */
    public static void saveDocument(Document doc, String filePath) throws TransformerException {
        // 将Document转换并保存到文件
        TransformerFactory transformerFactory = TransformerFactory.newDefaultInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(filePath));
        transformer.transform(source, result);
        String msg = "XML文件已成功保存到: " + filePath;
        logger.info(msg);
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
     *
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
     * 初始化打包系统实例中所有规则存储的方法（测试方法）
     *
     * @return List<RulesStorage>
     * @throws IOException IO异常
     */
    private List<RulesStorage> initAllRulesStorageForTests(Integer tid) throws IOException {
        List<RulesStorage> rulesStoragesForTests = new ArrayList<>();
        //跳过机票标号
        rulesStoragesForTests.add(null);
        //跳过酒店品类（没有使用）
        rulesStoragesForTests.add(null);
        boolean autoSave = tid != null;
        if (tid == null) {
            tid = -1;
        }
        for (int type = 2; type < SharedAttributes.getFullNames().length; type++) {
            List<List<String>> rules;
            if (sqlUtils != null) {
                rules = getRulesFromDB(type, tid);
                if (rules.isEmpty()) {
                    rules = getRulesFromCSVFile(type, tid, autoSave);
                }
            } else {
                rules = getRulesFromCSVFile(type, tid, autoSave);
            }
            RulesStorage rulesStorage = RulesStorage.initRulesStorageByType(type, rules);
            rulesStoragesForTests.add(rulesStorage);
        }
        return rulesStoragesForTests;
    }

    /**
     * 初始化打包系统实例中所有规则存储的方法（完全从数据库中获取规则）
     *
     * @return List<RulesStorage>
     */
    private List<RulesStorage> initAllRulesStorageFromDB(Integer tid) {
        List<RulesStorage> rulesStoragesFromDB = new ArrayList<>();
        //跳过机票标号
        rulesStoragesFromDB.add(null);
        //跳过酒店品类（没有使用）
        rulesStoragesFromDB.add(null);
        for (int type = 2; type < SharedAttributes.getFullNames().length; type++) {
            List<List<String>> rules;
            rules = getRulesFromDB(type, tid);
            RulesStorage rulesStorage = RulesStorage.initRulesStorageByType(type, rules);
            rulesStoragesFromDB.add(rulesStorage);
        }
        return rulesStoragesFromDB;
    }

    private List<List<String>> getRulesFromCSVFile(int type) throws IOException {
        List<List<String>> itemTicketRules = new ArrayList<>();
        //否则，进行训练
        List<List<String>> listOfAttributeList = fileIO.singleTypeCsv2ListOfAttributeList(type);
        associationRulesMining(listOfAttributeList, false
                , true, null, itemTicketRules
                , 0.08, 0);
        return itemTicketRules;
    }

    public List<List<String>> getRulesFromCSVFile(int type, int tid, boolean autoSave) throws IOException {
        List<List<String>> itemTicketRules = getRulesFromCSVFile(type);
        if (autoSave && sqlUtils != null) {
            try {
                sqlUtils.insertRules(type, itemTicketRules, tid);
            } catch (Exception e) {
                logger.info("自动存储规则失败");
            }
        }
        return itemTicketRules;
    }

    public List<List<String>> getRulesFromDB(int type, int tid) {
        List<List<String>> itemTicketRules;
        try {
            //如果已经存在，则直接加载
            itemTicketRules = sqlUtils.loadRules(type, tid);
        } catch (Exception e) {
            return new ArrayList<>();
        }
        return itemTicketRules;
    }

    public List<RulesStorage> getRulesStorages() {
        return rulesStorages;
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
