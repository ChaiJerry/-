package packing_system.io;

import com.mongodb.*;
import com.mongodb.MongoClient;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import org.apache.spark.sql.*;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.logging.*;

import org.bson.*;

import static com.mongodb.client.model.Projections.*;
import static packing_system.data_processer.DataConverter.*;

public class MongoUtils {

    private MongoUtils() {
    }

    private static MongoClient mongoClient = null;
    private static MongoDatabase mongoOrdersDatabase;
    private static MongoDatabase mongoKnowledgeDatabase;
    private static final Properties properties;
    private static final String HOST;
    private static final int PORT;
    private static final String ORDERS_DB_NAME;
    private static final String KNOWLEDGE_DB_NAME;
    private static final String PASSWORD;
    private static final String USER;
    private static final String DB_SOURCE;
    private static final Logger logger = Logger.getLogger(MongoUtils.class.getName());

    //得到最近的训练编号
    public static int getLatestTrainingNumber() {
        return Integer.parseInt(nextTrainingNumber) - 1;
    }

    private static String nextTrainingNumber;
    private static final String TRAINING_NUMBER_FIELD_NAME = "trainingNumber";
    //训练的起始时间和结束时间
    private static String startTime;

    private static boolean isClosed = false;

    /*
     * 读取配置文件
     */
    static {
        properties = new Properties();
        try {
            InputStream stream = MongoUtils.class.getClassLoader().getResourceAsStream("MongoDB.properties");
            properties.load(stream);
        } catch (IOException e) {
            logger.info("读取配置文件失败！" + e.getClass().getName() + ": " + e.getMessage());
        }
        HOST = properties.getProperty("host");
        ORDERS_DB_NAME = properties.getProperty("OrdersDbname");
        KNOWLEDGE_DB_NAME = properties.getProperty("KnowledgeDbname");
        PORT = Integer.parseInt(properties.getProperty("port"));
        USER = properties.getProperty("user");
        PASSWORD = properties.getProperty("password");
        DB_SOURCE = properties.getProperty("dbSource");
        nextTrainingNumber = properties.getProperty(TRAINING_NUMBER_FIELD_NAME);
        initialMongoClient();
    }

    /*
     * 初始化MongoDB连接
     */
    private static void initialMongoClient() {
        //得到当前时间作为开始时间
        startTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        try {
            //获取ip和端口号
            ServerAddress serverAddress = new ServerAddress(HOST, PORT);
            List<ServerAddress> addresses = new ArrayList<>();
            addresses.add(serverAddress);
            //MongoCredential.createScramSha1Credential()三个参数分别为 用户名 数据库名称 密码
            MongoCredential credential = MongoCredential.createScramSha1Credential(USER,
                    DB_SOURCE, PASSWORD.toCharArray());
            List<MongoCredential> credentials = new ArrayList<>();
            //将认证信息添加到列表
            credentials.add(credential);
            //通过连接认证获取MongoDB连接
            mongoClient = new MongoClient(addresses, credentials);
            //获取数据库
            mongoKnowledgeDatabase = mongoClient.getDatabase(KNOWLEDGE_DB_NAME);
            mongoOrdersDatabase = mongoClient.getDatabase(ORDERS_DB_NAME);
            //获取训练编号,若是auto则自动获取实现自增
            if (nextTrainingNumber.equals("auto")) {
                //获取TrainingController集合
                MongoCollection<Document> collection = mongoKnowledgeDatabase.getCollection("TrainingController");
                //查询最近的TrainingNumber
                FindIterable<Document> records = collection
                        .find().sort(Sorts.descending(TRAINING_NUMBER_FIELD_NAME));
                //如果存在训练编号，则自动获取
                if (records.iterator().hasNext()) {
                    Document doc = records.iterator().next();
                    //获取训练编号
                    int number = Integer.parseInt(doc.get(TRAINING_NUMBER_FIELD_NAME).toString()) + 1;
                    nextTrainingNumber = Integer.toString(number);
                    String info = "自动获取训练编号：" + number;
                    logger.info(info);
                } else {
                    //否则自动获取1
                    logger.info("自动获取训练编号：1");
                    nextTrainingNumber = "1";
                }
            }
            logger.info("MongoDB连接成功！");
        } catch (Exception e) {
            logger.info("MongoDB连接失败！" + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    //用于创建订单集合
    public static void createOrdersCollections() {
        for (String name : SharedAttributes.FULL_NAMES) {
            //创建集合
            mongoOrdersDatabase.createCollection(name + SharedAttributes.ORDERS_FIELD_NAME);
            if (!Objects.equals(name, SharedAttributes.FULL_NAMES[SharedAttributes.TICKET])) {
                mongoOrdersDatabase.createCollection(SharedAttributes.FULL_NAMES[SharedAttributes.TICKET] + "-" + name + SharedAttributes.ORDERS_FIELD_NAME);
            }
        }
    }

    public static void ordersMap2DB(Map<String, List<List<String>>> ordersMap, int type) {
        ItemAttributesStorage itemAttributesStorage = SharedAttributes.getItemAttributesStorage()[type];
        //获取集合名称
        String collectionName = SharedAttributes.FULL_NAMES[type] + SharedAttributes.ORDERS_FIELD_NAME;
        MongoCollection<Document> collection = mongoOrdersDatabase.getCollection(collectionName);
        for (Map.Entry<String, List<List<String>>> entry : ordersMap.entrySet()) {
            List<List<String>> values = entry.getValue();
            for (List<String> value : values) {
                Document doc = new Document();
                doc.append("orderId", entry.getKey());
                if (!values.isEmpty()) {
                    //借用频繁项集的存储结构，将属性存入
                    doc.append("attributes", itemAttributesStorage.getFrequentItemSetsDocument(value));
                    collection.insertOne(doc);
                }
            }
        }
    }

    public static MongoCollection<Document> getRulesCollection(int type) {
        //获取集合名称
        String collectionName = "r_" + SharedAttributes.FULL_NAMES[type];
        return mongoKnowledgeDatabase.getCollection(collectionName);
    }

    public static MongoCollection<Document> getFrequentItemSetsCollection(int type) {
        //获取集合名称
        String collectionName = "f_" + SharedAttributes.FULL_NAMES[type];
        return mongoKnowledgeDatabase.getCollection(collectionName);
    }

    public static MongoCollection<Document> getOrdersCollection(int type) {
        //获取集合名称
        String collectionName = SharedAttributes.FULL_NAMES[type] + SharedAttributes.ORDERS_FIELD_NAME;
        return mongoOrdersDatabase.getCollection(collectionName);
    }

    public static void initializeItemAttributesStorages() {
        //排除测试以及训练集这两个特殊的种类
        for (int type = 1; type < SharedAttributes.types.length - 2; type++) {
            SharedAttributes.itemAttributesStorage[type] = new ItemAttributesStorage();
            MongoCollection<Document> collection = getFrequentItemSetsCollection(type);
            FindIterable<Document> records = collection.find().sort(Sorts.descending(TRAINING_NUMBER_FIELD_NAME));
            //只用找到一个频繁项集中的itemAttributes中的所有属性名即可
            if (records.iterator().hasNext()) {
                Document doc = records.iterator().next();
                if (type == 1) {
                    SharedAttributes.itemAttributesStorage[0] = new ItemAttributesStorage();
                    Set<String> strings = ((Document) (doc.get(SharedAttributes.TICKET_ATTRIBUTES_FIELD_NAME))).keySet();
                    for (String s : strings) {
                        SharedAttributes.itemAttributesStorage[0].addAttribute(s);
                    }
                }
                Set<String> strings = ((Document) (doc.get(SharedAttributes.ITEM_ATTRIBUTES_FIELD_NAME))).keySet();
                for (String s : strings) {
                    SharedAttributes.itemAttributesStorage[type].addAttribute(s);
                }
            }

        }
    }

    public static List<String> getTargetItemFromOrderNum(String orderNumber, int type
            , MongoCollection<Document> collection) {
        FindIterable<Document> records = collection.find(Filters
                .eq("orderId", orderNumber)).projection(fields(Projections.include(
                SharedAttributes.getTargetItemFieldNames(type)), excludeId()));
        List<String> targetItems = new ArrayList<>();
        MongoCursor<Document> iterator = records.iterator();
        while (iterator.hasNext()) {
            targetItems.add(getItemNameFromDocument((iterator.next()), type));
        }
        return targetItems;
    }

    /**
     * 读取规则，并存入MongoDB
     */
    public static void rules2db(Dataset<Row> rules, int type ,int eva) {
        for (Row r : rules.collectAsList()) {
            //处理第0列antecedent
            Document doc = new Document();
            Document antecedent = new Document();
            //得到对应的集合
            MongoCollection<Document> collection = mongoKnowledgeDatabase.getCollection("r_" + SharedAttributes.FULL_NAMES[type]);
            if (SharedAttributes.itemAttributesStorage[SharedAttributes.TRAIN_TICKET]
                    .getRulesDocument(r.getList(0), antecedent ,eva)) {
                doc.append("antecedent", antecedent);
                //处理(consequent)
                String[] parts = r.getList(1).get(0).toString().split(":");
                //如果consequent是机票类型的属性，则跳过
                if (parts[0].equals("Ticket")) {
                    continue;
                }
                //添加consequent
                doc.append("consequence", parts[1] + ":" + parts[2]);
                //添加置信度
                doc.append("confidence", Float.parseFloat(r.get(2).toString()));
                //添加训练编号
                doc.append(TRAINING_NUMBER_FIELD_NAME, Integer.parseInt(nextTrainingNumber));
                //加入数据库
                collection.insertOne(doc);
            }
        }
    }

    public static void frequentItemSets2db(Dataset<Row> itemSets, int type ) {
        MongoCollection<Document> collection = mongoKnowledgeDatabase.getCollection("f_" + SharedAttributes.FULL_NAMES[type]);
        for (Row r : itemSets.collectAsList()) {
            //写入数据库的doc
            Document doc = new Document();
            //初始化ticketAttributes和goodAttributes用于存储频繁项集
            List<String> ticketAttributes = new ArrayList<>();
            List<String> goodAttributes = new ArrayList<>();
            //得到频繁项集
            for (Object s : r.getList(0)) {
                String temp = s.toString();
                //如果频繁项集是机票类型的属性，则加入ticketAttributes
                if (isTicketType(temp)) {
                    ticketAttributes.add(temp);
                } else {
                    //否则加入goodAttributes
                    goodAttributes.add(temp);
                }
            }
            //如果ticketAttributes或goodAttributes为空则无意义，跳过
            if (ticketAttributes.isEmpty() || goodAttributes.isEmpty()) {
                continue;
            }
            //将ticketAttributes添加到doc
            doc.append(SharedAttributes.TICKET_ATTRIBUTES_FIELD_NAME, SharedAttributes.itemAttributesStorage[SharedAttributes.TRAIN_TICKET].getFrequentItemSetsDocument(ticketAttributes));
            //将goodAttributes添加到doc
            doc.append("itemAttributes", SharedAttributes.itemAttributesStorage[type].getFrequentItemSetsDocument(goodAttributes));
            //添加训练编号
            doc.append(TRAINING_NUMBER_FIELD_NAME, Integer.parseInt(nextTrainingNumber));
            //添加频次
            doc.append("freq", Integer.parseInt(r.get(1).toString()));
            //加入数据库
            collection.insertOne(doc);
        }
    }

    public static void frequentItemSets2db(Dataset<Row> itemSets, int type ,int eva) {
        MongoCollection<Document> collection = mongoKnowledgeDatabase.getCollection("f_" + SharedAttributes.FULL_NAMES[type]);
        for (Row r : itemSets.collectAsList()) {
            //写入数据库的doc
            Document doc = new Document();
            //初始化ticketAttributes和goodAttributes用于存储频繁项集
            List<String> ticketAttributes = new ArrayList<>();
            List<String> goodAttributes = new ArrayList<>();
            //得到频繁项集
            for (Object s : r.getList(0)) {
                String temp = s.toString();
                //如果频繁项集是机票类型的属性，则加入ticketAttributes
                if (isTicketType(temp)) {
                    ticketAttributes.add(temp);
                } else {
                    //否则加入goodAttributes
                    goodAttributes.add(temp);
                }
            }
            //如果ticketAttributes或goodAttributes为空则无意义，跳过
            if (ticketAttributes.isEmpty() || goodAttributes.isEmpty()) {
                continue;
            }
            //将ticketAttributes添加到doc
            doc.append(SharedAttributes.TICKET_ATTRIBUTES_FIELD_NAME, SharedAttributes.itemAttributesStorage[SharedAttributes.TRAIN_TICKET].getFrequentItemSetsDocument(ticketAttributes));
            //将goodAttributes添加到doc
            doc.append("itemAttributes", SharedAttributes.itemAttributesStorage[type].getFrequentItemSetsDocument(goodAttributes));
            //添加训练编号
            doc.append(TRAINING_NUMBER_FIELD_NAME, Integer.parseInt(nextTrainingNumber));
            //添加频次
            doc.append("freq", Integer.parseInt(r.get(1).toString()));
            //加入数据库
            collection.insertOne(doc);
        }
    }
    /*
     * 关闭MongoDB连接
     */
    public static void settle(int orderNumber, String comments, float minSupport) {

            //得到当前时间作为结束时间
            String endTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            //获取TrainingController集合
            MongoCollection<Document> collection = mongoKnowledgeDatabase.getCollection("TrainingController");
            //写入训练信息
            Document doc = new Document();
            //训练编号以整数形式存储
            doc.append(TRAINING_NUMBER_FIELD_NAME, Integer.parseInt(nextTrainingNumber));
            //训练开始时间
            doc.append("startTime", startTime);
            //训练结束时间
            doc.append("endTime", endTime);
            //训练的订单数量
            doc.append("orderNumber", orderNumber);
            //备注
            doc.append("comments", comments);
            //最小支持度
            doc.append("minSupport", minSupport);
            //将doc加入数据库
            collection.insertOne(doc);

            nextTrainingNumber = (Integer.parseInt(nextTrainingNumber) + 1) + "";


    }

    public static void rulesAndFreqInDB2csv() {
        int latestTrainingNumber = getLatestTrainingNumber();
        for (int i = 1; i < 6; i++) {
            rulesInDB2csv(i, latestTrainingNumber);
            frequentItemSetsInDB2csv(i, latestTrainingNumber);
        }
    }

    private static void frequentItemSetsInDB2csv(int i, int latestTrainingNumber) {
        MongoCollection<Document> frequentItemSetsCollection = getFrequentItemSetsCollection(i);
        FindIterable<Document> documents = frequentItemSetsCollection.find(Filters.eq(TRAINING_NUMBER_FIELD_NAME, nextTrainingNumber));
        for(Document doc : documents) {
            Document ticketAttributes = (Document) doc.get("ticketAttributes");
            Document itemAttributes = (Document) doc.get("itemAttributes");
            String trainingNumber = doc.getString(TRAINING_NUMBER_FIELD_NAME);
            String freq = doc.getString("Freq");
        }
    }

    private static void rulesInDB2csv(int i, int latestTrainingNumber) {

    }

}
