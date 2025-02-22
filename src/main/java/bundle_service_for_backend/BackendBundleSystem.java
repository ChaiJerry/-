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
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

import static bundle_system.io.SharedAttributes.*;

public class BackendBundleSystem {

    // 创建 DocumentBuilderFactory 和 DocumentBuilder
    static DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    static DocumentBuilder dBuilder;

    static XMLIO xmlIO;
    static Logger logger = Logger.getLogger("BackendBundleSystem");

    static {
        try {
            xmlIO = new XMLIO();
            dBuilder = dbFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            logger.info("初始化 DocumentBuilder 失败");
            logger.info(e.getMessage());
        }
    }

    private final int poolSize; // 线程池的大小
    private final ExecutorService executorService;

    private final List<RulesStorage> rulesStorages;
    private final SQLUtils sqlUtils;

    /**
     * 默认使用训练id为1的训练数据的后端打包系统，用于测试
     * 请勿使用默认构造函数，请使用带参数的构造函数，这只是用来测试的
     */
    public BackendBundleSystem() throws IOException {
        this.poolSize = 8;
        executorService = Executors.newFixedThreadPool(poolSize);
        sqlUtils = new SQLUtils();
        rulesStorages = initAllRulesStorageFromDB(1);
    }

    /**
     * 当想要从数据库中读取训练然后保存到对应的数据库中就用这个构造函数
     *
     * @param poolSize 线程池的大小
     * @param sqlUtils 数据库操作对象
     * @param trainId  希望作为知识库的训练数据的id
     */
    public BackendBundleSystem(int poolSize, SQLUtils sqlUtils, Integer trainId) {
        // 设置线程池大小
        this.poolSize = poolSize;
        // 设置数据库操作对象
        this.sqlUtils = sqlUtils;
        // 设置线程池
        executorService = Executors.newFixedThreadPool(poolSize);
        // 从数据库中读取训练数据，并初始化所有规则存储对象
        rulesStorages = initAllRulesStorageFromDB(trainId);
    }



    /**
     * 从字符串中读取 XML 文件，然后提交打包任务到线程池的方法
     *
     * @param xmlStr 查询的 XML 字符串
     * @return 结果 XML 字符串
     * @throws Exception 如果在处理过程中发生错误
     */
    public String submitBundleTaskInStr(String xmlStr) throws Exception {
        // 将 XML 字符串转换为 Document 对象
        Document doc = xmlIO.stringToDocument(xmlStr);

        // 提交查询任务到线程池
        Future<?> future = executorService.submit(new BundleTask(doc, rulesStorages));
        // 等待查询任务完成
        future.get();

        // 将 Document 对象转换回 XML 字符串并返回
        return xmlIO.documentToString(doc);
    }

    /**
     * 提交打包任务到线程池
     *
     * @param doc 要处理的 Document 对象
     * @return 处理后的 Document 对象
     * @throws ExecutionException   如果执行过程中发生异常
     * @throws InterruptedException 如果当前线程被中断
     */
    public Document submitBundleTask(Document doc) throws ExecutionException, InterruptedException {
        // 提交查询任务到线程池
        Future<?> future = executorService.submit(new BundleTask(doc, rulesStorages));
        // 等待查询任务完成
        future.get();
        // 返回处理后的 Document 对象
        return doc;
    }

    /**
     * 从字符串中读取 XML 文件，然后提交查询任务到线程池的方法
     *
     * @param xmlStr 查询的 XML 字符串
     * @return 结果 XML 字符串
     * @throws Exception 如果在处理过程中发生错误
     */
    public String submitQueryTaskInStr(String xmlStr) throws Exception {
        // 将 XML 字符串转换为 Document 对象
        Document doc = xmlIO.stringToDocument(xmlStr);

        // 提交查询任务到线程池
        Future<?> future = executorService.submit(new QueryTask(doc, rulesStorages));
        // 等待查询任务完成
        future.get();

        // 将 Document 对象转换回 XML 字符串并返回
        return xmlIO.documentToString(doc);
    }

    /**
     * 提交查询任务到线程池
     *
     * @param doc 要处理的 Document 对象
     * @return 处理后的 Document 对象
     * @throws ExecutionException   如果执行过程中发生异常
     * @throws InterruptedException 如果当前线程被中断
     */
    public Document submitQueryTask(Document doc) throws ExecutionException, InterruptedException {
        // 提交查询任务到线程池
        Future<?> future = executorService.submit(new QueryTask(doc, rulesStorages));
        // 等待查询任务完成
        future.get();
        // 返回处理后的 Document 对象
        return doc;
    }


    /**
     * 测试正确性，会对于原来的doc修改，最后保存
     */
    public boolean test() throws InterruptedException {
        try {
            //模拟输入Document
            XMLIO xmlio = new XMLIO();
            long start = System.nanoTime();
            Document doc;
            doc = xmlio.readTest2();
            submitBundleTask(doc);
            saveDocument(doc, "test2.xml");
            String testMsg = "time(ms):" + ((double) (System.nanoTime() - start)) / 1000000;
            logger.info(testMsg);
            doc = xmlio.readTest1();
            submitQueryTask(doc);
            saveDocument(doc, "test1.xml");
            shutdownAll();
            return true;
        }catch(ParserConfigurationException | IOException | SAXException | TransformerException | ExecutionException  e){
            return false;
        }
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
     * 给附加产品排序的方法，但这个更高级一点，当遇到数值类型字符串时
     * 会解析后使用负指数取数值之间的差值来排序
     *
     * @param map            推荐的附加产品属性键值对
     * @param bundleItemList 附加产品键列表
     * @return boolean 返回是否有商品需要被解析
     */
    public static boolean setPriorityAndSortWithNumParse(Map<String, AttrValueConfidencePriority> map, List<BundleItem> bundleItemList) {
        // 判断bundleItemList是否为null，如果为空则直接返回false
        if(bundleItemList==null) {
            return false;
        }
        // 遍历每个 BundleItem 并设置优先级（带有数值解析）
        for (BundleItem bundleItem : bundleItemList) {
            bundleItem.setPriorityWithNumParse(map);
        }
        // 对 BundleItem 列表进行排序
        Collections.sort(bundleItemList);
        return true;
    }


    /**
     * 用于实际生产环境使用，该方法从数据库中获取规则。
     * 初始化打包系统实例中所有规则存储的方法（完全从数据库中获取规则）
     * 该方法不使用 CSV 文件，而是直接从数据库中获取规则
     *
     * @return List<RulesStorage>
     */
    private List<RulesStorage> initAllRulesStorageFromDB(Integer tid) {
        List<RulesStorage> rulesStoragesFromDBSystem = new ArrayList<>();

        // 跳过机票标号
        rulesStoragesFromDBSystem.add(null);

        // 遍历每个品类并初始化规则存储
        for (int type = HOTEL; type < SharedAttributes.getFullNames().length; type++) {
            List<List<String>> rules;

            // 从数据库中获取规则
            rules = getRulesFromDB(type, tid);

            // 初始化 RulesStorage 并添加到列表中
            RulesStorage rulesStorage = RulesStorage.initRulesStorageByType(type, rules);
            rulesStoragesFromDBSystem.add(rulesStorage);
        }

        return rulesStoragesFromDBSystem;
    }


    /**
     * 从数据库中获取规则的方法。
     * 该方法尝试从数据库中加载规则，如果失败则返回空列表。
     *
     * @param type 品类编号
     * @param tid 训练编号
     * @return  List<List<String>> 规则列表，如果没有找到则返回空列表。
     */
    public List<List<String>> getRulesFromDB(int type, int tid) {
        List<List<String>> itemTicketRules;

        try {
            // 如果已经存在，则直接加载
            itemTicketRules = sqlUtils.loadRules(type, tid);
        } catch (Exception e) {
            logger.info("尝试从数据库中加载规则失败");
            return new ArrayList<>();
        }
        return itemTicketRules;
    }



    /**
     * 关闭所有线程池。
     * 当结束打包系统时，应该调用此方法来关闭所有线程池。
     */
    public void shutdownAll() {
        executorService.shutdown();
    }

    /**
     * 获取规则存储列表。
     * @return List<RulesStorage> 规则存储列表。
     */
    public List<RulesStorage> getRulesStorages() {
        return rulesStorages;
    }

}
