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
import org.bson.conversions.*;

import static com.mongodb.client.model.Projections.*;
import static io.IOMonitor.*;

public class MongoUtils {
    private MongoUtils(){
    }
    private static MongoClient mongoClient = null;
    private static MongoDatabase mongoDatabase;
    private static final Properties properties;
    private static final String HOST;
    private static final int PORT;
    private static final String DB_NAME;
    private static final String PASSWORD;
    private static final String USER;
    private static final String DB_SOURCE;
    private static final Logger logger = Logger.getLogger(MongoUtils.class.getName());
    private static String trainingNumber;
    private static final String TRAINING_NUMBER_FIELD = "trainingNumber";
    //训练的起始时间和结束时间
    private static String startTime;

    private static boolean isClosed= false;

    private static String[] ates={"T_CARRIER",
            "T_GRADE","T_PASSENGER","S_SHOFARE","MONTH","TO"};


    /*
     * 读取配置文件
     */
    static{
        properties = new Properties();
        try{
            InputStream stream = MongoUtils.class.getClassLoader().getResourceAsStream("MongoDB.properties");
            properties.load(stream);
        }catch (IOException e){
            logger.info("读取配置文件失败！" + e.getClass().getName() + ": " + e.getMessage());
        }
        HOST = properties.getProperty("host");
        DB_NAME = properties.getProperty("dbname");
        PORT = Integer.parseInt(properties.getProperty("port"));
        USER = properties.getProperty("user");
        PASSWORD = properties.getProperty("password");
        DB_SOURCE = properties.getProperty("dbSource");
        trainingNumber = properties.getProperty(TRAINING_NUMBER_FIELD);
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
            mongoDatabase = mongoClient.getDatabase(DB_NAME);
            //获取训练编号,若是auto则自动获取实现自增
            if(trainingNumber.equals("auto")){
                //获取TrainingController集合
                MongoCollection<Document> collection = mongoDatabase.getCollection("TrainingController");
                //查询最近的TrainingNumber
                FindIterable<Document> records = collection
                        .find().sort(Sorts.descending(TRAINING_NUMBER_FIELD));
                //如果存在训练编号，则自动获取
                if (records.iterator().hasNext()) {
                    Document doc = records.iterator().next();
                    //获取训练编号
                    int number = Integer.parseInt(doc.get(TRAINING_NUMBER_FIELD).toString()) + 1;
                    trainingNumber = Integer.toString(number);
                    String info = "自动获取训练编号：" + number;
                    logger.info(info);
                }else {
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



    /**
     * 读取规则，并存入MongoDB
     */
    public static void rules2db(Dataset<Row> rules, int type){
        for (Row r : rules.collectAsList()) {
            //处理第0列antecedent
            Document doc = new Document();
            Document antecedent = new Document();
            //得到对应的集合
            MongoCollection<Document> collection = mongoDatabase.getCollection("r_"+FULL_NAMES[type]);
            if (headerStorage[TICKET].getRulesDocument(r.getList(0),antecedent)) {
                doc.append("antecedent", antecedent);
                //处理(consequent)
                String[] parts = r.getList(1).get(0).toString().split(":");
                //如果consequent是机票类型的属性，则跳过
                if (parts[0].charAt(0) == 'T') {
                    continue;
                }
                //添加consequent
                doc.append("consequence", parts[1]+":"+parts[2]);
                //添加置信度
                doc.append("confidence", Float.parseFloat(r.get(2).toString()));
                //添加训练编号
                doc.append(TRAINING_NUMBER_FIELD, Integer.parseInt(trainingNumber));
                //加入数据库
                collection.insertOne(doc);
            }
        }
    }

    public static void frequentItemSets2db(Dataset<Row> itemSets, int type){
        MongoCollection<Document> collection = mongoDatabase.getCollection("f_"+FULL_NAMES[type]);
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
                if(temp.charAt(0) == 'T' ){
                    ticketAttributes.add(temp);
                }else {
                    //否则加入goodAttributes
                    goodAttributes.add(temp);
                }
            }
            //如果ticketAttributes或goodAttributes为空则无意义，跳过
            if(ticketAttributes.isEmpty() || goodAttributes.isEmpty()) {
                continue;
            }
            //将ticketAttributes添加到doc
            doc.append("ticketAttributes", headerStorage[TICKET].getFrequentItemSetsDocument(ticketAttributes));
            //将goodAttributes添加到doc
            doc.append("itemAttributes", headerStorage[type].getFrequentItemSetsDocument(goodAttributes));
            //添加训练编号
            doc.append(TRAINING_NUMBER_FIELD, Integer.parseInt(trainingNumber));
            //添加频次
            doc.append("Freq", Integer.parseInt(r.get(1).toString()));
            //加入数据库
            collection.insertOne(doc);
        }
    }

    /*
     * 关闭MongoDB连接
     */
    public static void closeMongoClient(int orderNumber,String comments,float minSupport){
        if (!isClosed) {
            //得到当前时间作为结束时间
            String endTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            //获取TrainingController集合
            MongoCollection<Document> collection = mongoDatabase.getCollection("TrainingController");
            //写入训练信息
            Document doc = new Document();
            //训练编号以整数形式存储
            doc.append(TRAINING_NUMBER_FIELD, Integer.parseInt(trainingNumber));
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
            try{
                //关闭MongoDB连接
                mongoClient.close();
            }catch(Exception e){
                logger.info("关闭MongoDB连接失败！" + e.getClass().getName() + ": " + e.getMessage());
            }
            isClosed = true;
        }
    }

    public static void baggageSearchDemo(String[] args){
        initialMongoClient();
        MongoCollection<Document> collection = mongoDatabase.getCollection("r_Baggage");
        List<Bson> bsonList = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            bsonList.add(Filters.eq("antecedent."+ates[i],args[i]));
        }
        FindIterable<Document> search= collection.find(Filters.and(bsonList)).projection(fields(include(
                "consequence", "confidence"), excludeId()));
        if(search.iterator().hasNext()){
            System.out.println("有符合条件的规则");
            System.out.println("推荐"+search.iterator().next().get("consequence"));
            return;
        }else{
            System.out.println("降低标准搜索");
            for (int i = 0; i < args.length-1; i++) {
                bsonList.set(i,Filters.eq(ates[i],null));
                search= collection.find(Filters.and(bsonList))
                        .projection(fields(include(
                                "consequence", "confidence"), excludeId()));
                if(search.iterator().hasNext()){
                    System.out.println("有符合条件的规则");
                    System.out.println(search.iterator().next());
                    return;
                }
            }
        }
        System.out.println("没有符合条件的规则!!!");
    }

}
