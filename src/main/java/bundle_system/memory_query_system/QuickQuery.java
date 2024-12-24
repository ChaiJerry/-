package bundle_system.memory_query_system;

import com.mongodb.client.*;
import org.bson.*;
import bundle_system.db_query_system.*;
import bundle_system.io.*;

import java.io.*;
import java.util.*;

import static bundle_system.api.API.*;
import static bundle_system.db_query_system.QuerySystem.*;
import static bundle_system.io.MongoUtils.*;
import static bundle_system.io.SharedAttributes.*;
import static bundle_system.memory_query_system.RulesStorage.*;

public class QuickQuery {
    private static final Map<String, ItemPack> itemPackMap = new HashMap<>();

    /**
     * 为测试初始化规则存储系统
     * @param type 品类编码
     * @return 规则存储系统对象
     */
    public static RulesStorage initRulesStorageByTypeForQuickQueryTest(int type) throws IOException {
        //训练阶段
        String info = "正在初始化"+SharedAttributes.getFullNames()[type]+"知识库";
        printProgressBar(0, info);
        List<List<String>> listOfAttributeList = fileIOForTest.csv2ListOfAttributeListByType(type);
        List<List<String>> itemTicketFreqItemSets = new ArrayList<>();
        List<List<String>> itemTicketRules = new ArrayList<>();
        printProgressBar(33, info);
        associationRulesMining(listOfAttributeList, false
                , true, itemTicketFreqItemSets, itemTicketRules
                , 0.08, 0);
        return initRulesStorageByType(type, itemTicketRules);
    }

    /**
     * 测试函数，用于测试基于内存的打包系统的准确率和召回率
     * @return f1值
     * @param type 品类编码
     */
    public double test(int type) throws IOException {
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////
        //训练阶段
        RulesStorage rulesStorage = initRulesStorageByTypeForQuickQueryTest(type);
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////
        //测试阶段
        //获取ordersCollection，用于查询订单
        //获取现在时间以测得测试用时
        long totalTime = 0;
        MongoCollection<Document> ordersCollection = getOrdersCollection(type);
        ItemAttributeNamesStorage ticketAttributesStorage = getItemAttributesStorage()[TEST_TICKET];
        Map<String, List<List<String>>> ticketOrderNumAttributeMap = getTicketOrderNumAttributesMap(fileIOForTest.read(PATH_TEST_T, TEST_TICKET), ticketAttributesStorage);
        itemPackMap.clear();
        for (Iterator<String> iterator = ticketOrderNumAttributeMap.keySet().iterator(); iterator.hasNext(); ) {
            //得到orderNum（订单号）
            String orderNum = iterator.next();
            List<String> correctTargetItems = getTargetItemFromOrderNum(orderNum, type, ordersCollection);
            if (correctTargetItems.isEmpty()) {
                continue;
            }
            //根据orderNum，查询ticketOrderNumAttributeMap中对应的属性值列表的列表
            List<List<String>> listOfTicketAttributeList = ticketOrderNumAttributeMap.get(orderNum);
            //遍历listOfTicketAttributeList（属性值列表的列表）
            for (List<String> attributeValues : listOfTicketAttributeList) {
                //得到每个机票属性列表特征键
                String itemPackKey = ItemPack.generateItemPackKey(attributeValues);
                //判断是否已经存在于itemPackMap中
                boolean isRepeated = itemPackMap.containsKey(itemPackKey);
                ItemPack itemPack;
                if (isRepeated) {
                    itemPack = itemPackMap.get(itemPackKey);
                } else {
                    itemPack = new ItemPack();
                    itemPackMap.put(itemPackKey, itemPack);
                }
                long startTime = System.nanoTime();
                //通过单个属性查询，得到打包商品候选集
                Map<String, AttrValueConfidencePriority> singleAttributeQuery = rulesStorage
                        .queryItemAttributesAndConfidence(ticketAttributesStorage
                                .generateOrderedAttributeListFromAttributeValueListForEva(attributeValues));
                long endTime = System.nanoTime();
                totalTime += endTime - startTime;
                Map<String, String> commendedAttributes = convertVCPToAttributes(singleAttributeQuery);
                //通过singleAttributeQuery得到的打包属性列表，查询ordersCollection中订单存在的打包商品
                String singleItemQuery = singleItemQuery(commendedAttributes, ordersCollection, type);
                //加入原订单中同时出现的商品
                itemPack.addOrderItem(correctTargetItems, type);
                //加入推荐系统推荐的商品
                itemPack.addRecommendedItem(singleItemQuery, type);
            }
        }
        //计算打包系统准确率和召回率
        //初始化评估器
        Evaluator evaluator = new Evaluator(itemPackMap);
        double averageAccuracy = evaluator.getAverageAccuracy();
        double averageRecallRate = evaluator.getAverageRecallRate();
        //输出准确率和召回率
        String accuracyInfo = averageAccuracy+ ",";
        String recallInfo = averageRecallRate+ ",";
        logger.info(accuracyInfo);
        logger.info(recallInfo);
        //输出F1值
        double f1 = (averageAccuracy + averageRecallRate) / 2;
        String f1Info = f1 + ",";
        logger.info(f1Info);
        itemPackMap.clear();

        //输出时间
        String msg = (totalTime) / 1000000 + "ms";
        logger.info(msg);
        return f1;
    }


    private static Map<String, String> convertVCPToAttributes(Map<String, AttrValueConfidencePriority> vcpMap) {
        Map<String, String> map = new HashMap<>();
        for (Map.Entry<String, AttrValueConfidencePriority> entry : vcpMap.entrySet()) {
            map.put(entry.getKey(), entry.getValue().getAttributeValue());
        }
        return map;
    }

}
