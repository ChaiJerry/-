package bundle_system.memory_query_system;

import bundle_system.io.sql.*;
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
    private static final Map<String, ItemPack> itemPackMap = new HashMap<>();




    public static List<RulesStorage> initAllRulesStorage() throws IOException {
        List<RulesStorage> rulesStorages = new ArrayList<>();
        //跳过机票标号
        rulesStorages.add(null);
        //跳过酒店品类（没有使用）
        rulesStorages.add(null);
        for(int type = 2; type < SharedAttributes.getFullNames().length; type++) {
            RulesStorage rulesStorage = initRulesStorageByType(type);
            rulesStorages.add(rulesStorage);
        }
        return rulesStorages;
    }

    public static RulesStorage initRulesStorageByType(int type) throws IOException {
        //训练阶段
        String info = "正在初始化"+SharedAttributes.getFullNames()[type]+"知识库";
        printProgressBar(0, info);
        RulesStorage rulesStorage = new RulesStorage(type);
        List<List<String>> listOfAttributeList = fileIO.singleTypeCsv2ListOfAttributeList(type);
        List<List<String>> itemTicketFreqItemSets = new ArrayList<>();
        List<List<String>> itemTicketRules = new ArrayList<>();
        printProgressBar(33, info);
        SQLUtils sqlUtils = new SQLUtils();
        //测算训练用时
        long startTime1 = System.nanoTime();
        try {
            //如果已经存在，则直接加载
            itemTicketRules = sqlUtils.loadRules(type, "test");
            if(itemTicketRules.isEmpty()) {
                associationRulesMining(listOfAttributeList, false
                        , true, itemTicketFreqItemSets, itemTicketRules
                        , 0.08, 0);
                sqlUtils.storeRules(type, itemTicketRules, "test");
            }
        }catch (Exception e) {
            //否则，进行训练
            associationRulesMining(listOfAttributeList, false
                    , true, itemTicketFreqItemSets, itemTicketRules
                    , 0.08, 0);
            sqlUtils.storeRules(type, itemTicketRules, "test");
        }
        printProgressBar(67, info);
        //System.out.println("Training time: " + (System.nanoTime() - startTime1) / 1000000+ "ms");
        for (List<String> itemTicketRule : itemTicketRules) {
            String[] split = itemTicketRule.get(0).split("; ");
            String consequent = itemTicketRule.get(1).split("; ")[0];
            double confidence = Double.parseDouble(itemTicketRule.get(2).split("::")[1]);
            rulesStorage.addRule(split, new AssociationRuleConsResult(consequent, confidence));
        }
        printProgressBar(100, info);
        System.out.println();
        return rulesStorage;
    }


    public void test(int type) throws IOException {
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////
        //训练阶段
        RulesStorage rulesStorage = initRulesStorageByType(type);
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



    /**
     * 输出一个动态的进度条。
     *
     * @param percent      当前的进度百分比（0 到 100）
     * @param progressBarName 进度条的名字
     */
    public static void printProgressBar(int percent, String progressBarName) {
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException("Percent must be between 0 and 100");
        }

        // 进度条的总长度
        int barLength = 50;

        // 计算已完成的部分
        int completed = (int) (percent * barLength / 100);

        // 构建进度条
        StringBuilder progressBar = new StringBuilder(progressBarName + "\t [");
        for (int i = 0; i < barLength; i++) {
            if (i < completed) {
                progressBar.append("\u001B[32m=\u001B[0m"); // 绿色的 "="
            } else if (i == completed) {
                progressBar.append(">");
            } else {
                progressBar.append(" ");
            }
        }
        progressBar.append("] ").append(String.format("%3d%%", percent));

        // 输出进度条
        System.out.print("\r" + progressBar.toString());
        System.out.flush(); // 确保立即输出
    }



}
