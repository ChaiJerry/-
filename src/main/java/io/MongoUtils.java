package io;

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
import static data_processer.DataConverter.*;
import static io.SharedAttributes.*;

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

    public static int getLatestTrainingNumber() {
        return Integer.parseInt(trainingNumber)-1;
    }

    private static String trainingNumber;
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
        trainingNumber = properties.getProperty(TRAINING_NUMBER_FIELD_NAME);
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
            if (trainingNumber.equals("auto")) {
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
                    trainingNumber = Integer.toString(number);
                    String info = "自动获取训练编号：" + number;
                    logger.info(info);
                } else {
                    //否则自动获取1
                    logger.info("自动获取训练编号：1");
                    trainingNumber = "1";
                }
            }
            logger.info("MongoDB连接成功！");
        } catch (Exception e) {
            logger.info("MongoDB连接失败！" + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    public static void createOrdersCollections() {
        for (String name : FULL_NAMES) {
            //创建集合
            mongoOrdersDatabase.createCollection(name + ORDERS_FIELD_NAME);
            if (!Objects.equals(name, FULL_NAMES[TICKET])) {
                mongoOrdersDatabase.createCollection(FULL_NAMES[TICKET] + "-" + name + ORDERS_FIELD_NAME);
            }
        }
    }

    public static void ordersMap2DB(Map<String, List<List<String>>> ordersMap, int type) {
        ItemAttributesStorage itemAttributesStorage = getHeaderStorage()[type];
        //获取集合名称
        String collectionName = FULL_NAMES[type] + ORDERS_FIELD_NAME;
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
        String collectionName = "r_" + FULL_NAMES[type];
        return mongoKnowledgeDatabase.getCollection(collectionName);
    }

    public static MongoCollection<Document> getFrequentItemSetsCollection(int type) {
        //获取集合名称
        String collectionName = "f_" + FULL_NAMES[type];
        return mongoKnowledgeDatabase.getCollection(collectionName);
    }

    public static MongoCollection<Document> getOrdersCollection(int type) {
        //获取集合名称
        String collectionName = FULL_NAMES[type] + ORDERS_FIELD_NAME;
        return mongoOrdersDatabase.getCollection(collectionName);
    }

    public static void initializeItemAttributesStorages() {
        for(int type = 1;type <types.length;type++) {
            itemAttributesStorage[type] = new ItemAttributesStorage();
            MongoCollection<Document> collection = getFrequentItemSetsCollection(type);
            FindIterable<Document> records = collection.find().sort(Sorts.descending(TRAINING_NUMBER_FIELD_NAME));
            //只用找到一个频繁项集中的itemAttributes中的所有属性名即可
            if (records.iterator().hasNext()) {
                Document doc = records.iterator().next();
                if(type==1){
                    itemAttributesStorage[0] = new ItemAttributesStorage();
                    Set<String> strings = ((Document) (doc.get(TICKET_ATTRIBUTES_FIELD_NAME))).keySet();
                    for (String s : strings) {
                        itemAttributesStorage[0].addAttribute(s);
                    }
                }
                Set<String> strings = ((Document) (doc.get(ITEM_ATTRIBUTES_FIELD_NAME))).keySet();
                for (String s : strings) {
                    itemAttributesStorage[type].addAttribute(s);
                }
            }


        }
    }

    public static List<String> getTargetItemFromOrderNum(String orderNumber, int type
            , MongoCollection<Document> collection) {
        FindIterable<Document> records = collection.find(Filters
                .eq("orderId", orderNumber)).projection(fields(include(
                getTargetItemFieldNames(type)), excludeId()));
        List<String> targetItems = new ArrayList<>();
        MongoCursor<Document> iterator = records.iterator();
        while (iterator.hasNext()) {
            targetItems.add(getItemNameFromDocument((iterator.next()),type));
        }
        return targetItems;
    }

    /**
     * 读取规则，并存入MongoDB
     */
    public static void rules2db(Dataset<Row> rules, int type) {
        for (Row r : rules.collectAsList()) {
            //处理第0列antecedent
            Document doc = new Document();
            Document antecedent = new Document();
            //得到对应的集合
            MongoCollection<Document> collection = mongoKnowledgeDatabase.getCollection("r_" + FULL_NAMES[type]);
            if (itemAttributesStorage[TICKET].getRulesDocument(r.getList(0), antecedent)) {
                doc.append("antecedent", antecedent);
                //处理(consequent)
                String[] parts = r.getList(1).get(0).toString().split(":");
                //如果consequent是机票类型的属性，则跳过
                if (parts[0].charAt(0) == 'T') {
                    continue;
                }
                //添加consequent
                doc.append("consequence", parts[1] + ":" + parts[2]);
                //添加置信度
                doc.append("confidence", Float.parseFloat(r.get(2).toString()));
                //添加训练编号
                doc.append(TRAINING_NUMBER_FIELD_NAME, Integer.parseInt(trainingNumber));
                //加入数据库
                collection.insertOne(doc);
            }
        }
    }

    public static void frequentItemSets2db(Dataset<Row> itemSets, int type) {
        MongoCollection<Document> collection = mongoKnowledgeDatabase.getCollection("f_" + FULL_NAMES[type]);
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
                if (temp.charAt(0) == 'T') {
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
            doc.append(TICKET_ATTRIBUTES_FIELD_NAME, itemAttributesStorage[TICKET].getFrequentItemSetsDocument(ticketAttributes));
            //将goodAttributes添加到doc
            doc.append("itemAttributes", itemAttributesStorage[type].getFrequentItemSetsDocument(goodAttributes));
            //添加训练编号
            doc.append(TRAINING_NUMBER_FIELD_NAME, Integer.parseInt(trainingNumber));
            //添加频次
            doc.append("Freq", Integer.parseInt(r.get(1).toString()));
            //加入数据库
            collection.insertOne(doc);
        }
    }

    /*
     * 关闭MongoDB连接
     */
    public static void closeMongoClient(int orderNumber, String comments, float minSupport) {
        if (!isClosed) {
            //得到当前时间作为结束时间
            String endTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            //获取TrainingController集合
            MongoCollection<Document> collection = mongoKnowledgeDatabase.getCollection("TrainingController");
            //写入训练信息
            Document doc = new Document();
            //训练编号以整数形式存储
            doc.append(TRAINING_NUMBER_FIELD_NAME, Integer.parseInt(trainingNumber));
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
            try {
                //关闭MongoDB连接
                mongoClient.close();
            } catch (Exception e) {
                logger.info("关闭MongoDB连接失败！" + e.getClass().getName() + ": " + e.getMessage());
            }
            isClosed = true;
        }
    }

}
