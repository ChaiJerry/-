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

public class QuickQuery {
    private static Map<String, ItemPack> itemPackMap = new HashMap<>();

    public static void test(int type) throws IOException {
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////
        //训练阶段
        RulesStorage rulesStorage = new RulesStorage(type);
        List<List<String>> listOfAttributeList = fileIO.singleTypeCsv2ListOfAttributeList(type);
        List<List<String>> itemTicketFreqItemSets = new ArrayList<>();
        List<List<String>> itemTicketRules = new ArrayList<>();
        //测算训练用时
        long startTime1 = System.nanoTime();
        associationRulesMining(listOfAttributeList, false, true, itemTicketFreqItemSets, itemTicketRules, 0.08, 0);
        System.out.println("Training time: " + (System.nanoTime() - startTime1) / 1000000+ "ms");
        for (List<String> itemTicketRule : itemTicketRules) {
            String[] split = itemTicketRule.get(0).split("; ");
            String consequent = itemTicketRule.get(1).split("; ")[0];
            double confidence = Double.parseDouble(itemTicketRule.get(2).split("::")[1]);
            rulesStorage.addRule(split, new AssociationRuleResult(consequent, confidence));
        }

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////
        //测试阶段
        //获取ordersCollection，用于查询订单
        //获取现在时间以测得测试用时
        long totalTime = 0;
        MongoCollection<Document> ordersCollection = getOrdersCollection(type);
        ItemAttributesStorage ticketAttributesStorage = getItemAttributesStorage()[TEST_TICKET];
        Map<String, List<List<String>>> ticketOrderNumAttributeMap = getTicketOrderNumAttributesMap(CSVFileIO.read(PATH_TEST_T, "Test"), ticketAttributesStorage);
        itemPackMap.clear();
        int total = 0;
        int round = 0;
        for (Iterator<String> iterator = ticketOrderNumAttributeMap.keySet().iterator(); iterator.hasNext(); ) {
            //得到orderNum（订单号）
//            System.out.println(round+++" ");
//            if(round==823){
//                System.out.println("stop");
//            }
            String orderNum = iterator.next();
            List<String> correctTargetItems = getTargetItemFromOrderNum(orderNum, type, ordersCollection);
            if (correctTargetItems.isEmpty()) {
                continue;
            }
            //根据orderNum，查询ticketOrderNumAttributeMap中对应的属性值列表的列表
            List<List<String>> listOfTicketAttributeList = ticketOrderNumAttributeMap.get(orderNum);
            //遍历listOfTicketAttributeList（属性值列表的列表）
            for (List<String> attributeValues : listOfTicketAttributeList) {
                //System.out.println(total++ + ": " + orderNum);

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
                Map<String, String> singleAttributeQuery = rulesStorage
                        .queryItemAttributes(ticketAttributesStorage
                                .generateOrderedAttributeListFromAttributeValueListForEva(attributeValues));
                long endTime = System.nanoTime();
                totalTime += endTime - startTime;
                //通过singleAttributeQuery得到的打包属性列表，查询ordersCollection中订单存在的打包商品
                String singleItemQuery = singleItemQuery(singleAttributeQuery, ordersCollection, type);
                //加入原订单中同时出现的商品
                itemPack.addOrderItem(correctTargetItems, type);
                //加入推荐系统推荐的商品
                itemPack.addRecommendedItem(singleItemQuery, type);
            }
        }
        Evaluator evaluator = new Evaluator(itemPackMap);
        double averageAccuracy = evaluator.getAverageAccuracy();
        double averageRecallRate = evaluator.getAverageRecallRate();

        String accuracyInfo = "" + averageAccuracy;
        String recallInfo = "" + averageRecallRate;
        //String f1=(averageAccuracy+averageRecallRate)/2;
        //System.out.print(getFullNames()[type] + ",");
        System.out.print(accuracyInfo + ",");
        System.out.print(recallInfo + ",");
        System.out.print((averageAccuracy + averageRecallRate) / 2 + ",");
        itemPackMap.clear();

        //输出时间
        System.out.println((totalTime)/1000000 + "ms");
        rulesStorage.shutdown();
    }
}
