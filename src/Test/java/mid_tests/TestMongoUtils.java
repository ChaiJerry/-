package mid_tests;

import com.mongodb.*;
import com.mongodb.MongoClient;
import com.mongodb.client.*;
import com.mongodb.client.model.*;

import org.apache.spark.ml.fpm.*;
import org.apache.spark.sql.*;
import org.bson.*;
import org.junit.*;

import static bundle_system.train_system.FPGrowth.*;
import static org.junit.Assert.*;

import java.io.*;
import java.util.*;

import static bundle_system.io.MongoUtils.*;

/**
 * mongodb在实际生产中不使用，但是这里需要mongo的特性
 * 进行测试验证算法的准确性和效率
 */
public class TestMongoUtils {
    private MongoClient mongoClient = null;
    private MongoDatabase mongoDatabase = null;
    private static final String TRAINING_NUMBER_FIELD="trainingNumber";
    @Before
    public void setUp(){
        try {
            //获取ip和端口号
            ServerAddress serverAddress = new ServerAddress("localhost", 27017);
            List<ServerAddress> addresses = new ArrayList<>();
            addresses.add(serverAddress);
            MongoCredential credential = MongoCredential.createScramSha1Credential("admin",
                    "admin", "1112345671--".toCharArray());
            List<MongoCredential> credentials = new ArrayList<>();
            //将认证信息添加到列表
            credentials.add(credential);
            //通过连接认证获取MongoDB连接
            mongoClient = new MongoClient(addresses, credentials);
            //获取数据库
            mongoDatabase = mongoClient.getDatabase("Knowledge");
        }catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Test
    public void testDatasets2DBFunction() throws IOException {
        int trainingNumber = getTrainingNumber(mongoDatabase
                .getCollection("TrainingController")) + 1;
        Dataset<Row> rowDataset = getFileIO().csv2datasetByType(1);
        FPGrowthModel model = train(rowDataset);
        Dataset<Row> frequentItemSet = model.freqItemsets();
        Dataset<Row> ruleDataset = model.associationRules();
        frequentItemSets2db(frequentItemSet, 1);
        rules2db(ruleDataset, 1);
        MongoCollection<Document> fHotel = mongoDatabase.getCollection("f_Hotel");
        MongoCollection<Document> rHotel = mongoDatabase.getCollection("r_Hotel");
        //判断是否写入成功
        FindIterable<Document> fSearch= fHotel.find(Filters.eq(TRAINING_NUMBER_FIELD, trainingNumber));
        FindIterable<Document> rSearch= rHotel.find(Filters.eq(TRAINING_NUMBER_FIELD, trainingNumber));
        assertTrue(fSearch.iterator().hasNext());
        assertTrue(rSearch.iterator().hasNext());
    }


    /**
     * 测试mongoUtils的自动写入以及关闭功能
     */
    @Test
    public void testTrainingNumberWritingFunction() {
        //获取集合
        MongoCollection<Document> collection = mongoDatabase.getCollection("TrainingController");
        //查询数据
        int trainingNumber= getTrainingNumber(collection);
        settle(0,"test",1);
        assertEquals(trainingNumber + 1, getTrainingNumber(collection));
    }




    /**
     * 获取训练集编号
     */
    private int getTrainingNumber(MongoCollection<Document> collection) {
        int trainingNumber;
        FindIterable<Document> records = collection.find().sort(Sorts.descending(TRAINING_NUMBER_FIELD));
        if (records.iterator().hasNext()) {
            Document doc = records.iterator().next();
            trainingNumber = Integer.parseInt(doc.get(TRAINING_NUMBER_FIELD).toString());
        }else {
            trainingNumber = 1;
        }
        return trainingNumber;
    }

}
