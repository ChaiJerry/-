package query_system;

import com.mongodb.client.*;

import data_processer.*;
import io.*;
import org.bson.*;

import java.io.*;
import java.util.*;
import java.util.logging.*;

import static data_processer.DataConverter.*;
import static io.IOMonitor.*;
import static io.MongoUtils.*;
import static query_system.ItemPack.*;

public class QuerySystem {
    private QuerySystem() {
    }

    private static final int[] colNum = {0, 5, 4, 2, 4, 2};

    private static List<List<String>> ticketAttributeValuesList = null;

    private static Map<String, List<String>> ticketOrderNumAttributeMap = null;

    private static Map<String, ItemPack> itemPackMap = new HashMap<>();

    private static List<String> strs = new ArrayList<>();

    private static final Logger logger;

    static {
        logger = Logger.getLogger(QuerySystem.class.getName());
    }




    public static List<List<String>> getTicketAttributeValuesList(CSVFileIO fileIO, int type) throws IOException {
        Map<String, List<String>> stringListMap = fileIO.read(type);
        //遍历Map<String, List<String>> stringListMap的所有值
        List<List<String>> ticketAttributeValuesList = new ArrayList<>();
        HeaderStorage headerStorage = getHeaderStorage()[type];
        for (List<String> list : stringListMap.values()) {
            ticketAttributeValuesList.add(headerStorage.getAttributeLists(list));
        }
        return ticketAttributeValuesList;
    }

    public static Map<String, List<String>> getTicketOrderNumAttributeMap(CSVFileIO fileIO, int type) throws IOException {
        //读取文件，返回Map<String, List<String>>，但是此时还不能使用，可能有不需要的属性或者属性顺序不对
        Map<String, List<String>> ticketOrderNumAttributeMap = fileIO.read(type);
        //获取HeaderStorage，用于获取调整后规范的属性列表
        HeaderStorage headerStorage = getHeaderStorage()[type];
        //遍历Map<String, List<String>> stringListMap的所有键值对
        for (Iterator<String> iterator = ticketOrderNumAttributeMap.keySet().iterator(); iterator.hasNext(); ) {
            String key = iterator.next();
            //获取key对应的List<String>，即属性列表
            List<String> attributeList = ticketOrderNumAttributeMap.get(key);
            //将attributeList中的元素调整为规范格式
            ticketOrderNumAttributeMap.put(key, headerStorage.getAttributeLists(attributeList));
        }
        return ticketOrderNumAttributeMap;
    }

    public static void queryTest3() throws IOException {
        CSVFileIO fileIO = new CSVFileIO(RESULT_DIR_PATH, PATH_T, PATH_H, PATH_M, PATH_B, PATH_I, PATH_S);
        for (int i = 0; i < colNum.length; i++) {
            fileIO.read(i);
        }

        //评估模式获取ticketOrderNumAttributeMap，用于查询订单号以及属性
        ticketOrderNumAttributeMap = getTicketOrderNumAttributeMap(fileIO, TICKET);
        for (Iterator<String> iterator = ticketOrderNumAttributeMap.keySet().iterator(); iterator.hasNext(); ) {
            String orderNum = iterator.next();
            //根据orderNum，查询ticketOrderNumAttributeMap中对应的属性列表
            List<String> attributeValues = ticketOrderNumAttributeMap.get(orderNum);
            String itemPackKey = generateItemPackKey(attributeValues);
            boolean isRepeated = itemPackMap.containsKey(itemPackKey);
            ItemPack itemPack;
            if(isRepeated) {
                itemPack = itemPackMap.get(itemPackKey);
            }else {
                itemPack = new ItemPack(itemPackKey);
                itemPackMap.put(itemPackKey, itemPack);
            }

            for (int i = 1; i < colNum.length; i++) {
                //获取rulesCollection，用于查询规则
                MongoCollection<Document> rulesCollection = getRulesCollection(i);
                //获取ordersCollection，用于查询订单
                MongoCollection<Document> ordersCollection = getOrdersCollection(i);
                int fixedPos = -1;
                if (i == 1) {
                    //当推荐的是酒店时，固定在位置5的属性（飞机目的地，避免推荐到其它城市）
                    fixedPos = 5;
                }
                String correctTargetItem = getTargetItemFromOrderNum(orderNum, i, ordersCollection);
                if(!correctTargetItem.isEmpty()) {
                    itemPack.addOrderItem(correctTargetItem, i);
                }
                if(isRepeated){
                    continue;
                }
                //根据attributeValues，查询ordersCollection中对应的订单
                Map<String, String> singleAttributeQuery =
                        singleAttributeRuleQuery(attributeValues, fixedPos, rulesCollection, i);
                String singleItemQuery = singleItemQuery(singleAttributeQuery, ordersCollection, i);
                itemPack.addRecommendedItem(singleItemQuery, i);
            }
        }
        double averageAccuracy = 0;
        double averageRecallRate = 0;
        int correctRateDiv = itemPackMap.size();
        int recallRateDiv = itemPackMap.size();

        for(ItemPack itemPack : itemPackMap.values()) {
          double aRate = itemPack.calculateAccuracy();
          double rRate = itemPack.calculateRecallRate();
          //-1说明是无效值，应当将相应的div减1
            if(aRate == -1) {
                correctRateDiv--;
            }else{
                averageAccuracy += aRate;
            }
            if(rRate == -1) {
                recallRateDiv--;
            }else {
                averageRecallRate += rRate;
            }
        }
        averageAccuracy /= correctRateDiv;
        averageRecallRate /= recallRateDiv;
        String accuracyInfo = "averageAccuracy: " + averageAccuracy;
        String recallInfo = "averageRecallRate: " + averageRecallRate;
        logger.info(accuracyInfo);
        logger.info(recallInfo);
    }

    public static List<String> queryTest2() throws IOException {
        CSVFileIO fileIO = new CSVFileIO(RESULT_DIR_PATH, PATH_T, PATH_H, PATH_M, PATH_B, PATH_I, PATH_S);
        for (int i = 0; i < colNum.length; i++) {
            fileIO.read(i);
        }

        //评估模式获取ticketOrderNumAttributeMap，用于查询订单号以及属性
        ticketOrderNumAttributeMap = getTicketOrderNumAttributeMap(fileIO, TICKET);

        String head = "orderNum,Ticket,Hotel,Meal,Baggage,Insurance,Seat";
        strs.add(head);
        MongoCollection<Document> ticketOrdersCollection = getOrdersCollection(TICKET);
        for (Iterator<String> iterator = ticketOrderNumAttributeMap.keySet().iterator(); iterator.hasNext(); ) {
            String orderNum = iterator.next();
            //根据orderNum，查询ticketOrderNumAttributeMap中对应的属性列表
            List<String> attributeValues = ticketOrderNumAttributeMap.get(orderNum);
            StringBuilder sb = new StringBuilder();
            sb.append(orderNum).append(",");
            sb.append(getTargetItemFromOrderNum(orderNum, TICKET, ticketOrdersCollection)).append(",");
            for (int i = 1; i < colNum.length; i++) {
                //获取rulesCollection，用于查询规则
                MongoCollection<Document> rulesCollection = getRulesCollection(i);
                //获取ordersCollection，用于查询订单
                MongoCollection<Document> ordersCollection = getOrdersCollection(i);
                int fixedPos = -1;
                if (i == 1) {
                    fixedPos = 5;
                }
                String correctTargetItem = getTargetItemFromOrderNum(orderNum, i, ordersCollection);
                //根据attributeValues，查询ordersCollection中对应的订单
                Map<String, String> singleAttributeQuery =
                        singleAttributeRuleQuery(attributeValues, fixedPos, rulesCollection, i);
                String singleItemQuery = singleItemQuery(singleAttributeQuery, ordersCollection, i);
                sb.append(singleItemQuery).append(":").append(correctTargetItem).append(",");
            }
            strs.add(sb.toString());
        }
        return strs;
    }

    public static void queryTest(String mode) throws IOException {
        CSVFileIO fileIO = new CSVFileIO(RESULT_DIR_PATH, PATH_T, PATH_H, PATH_M, PATH_B, PATH_I, PATH_S);
        for (int i = 0; i < colNum.length; i++) {
            fileIO.read(i);
        }

        if (mode.equals("eva")) {
            //评估模式获取ticketOrderNumAttributeMap，用于查询订单号以及属性
            ticketOrderNumAttributeMap = getTicketOrderNumAttributeMap(fileIO, TICKET);
        } else if (mode.equals("generate")) {
            //生成模式获取ticketAttributeValuesList，用于查询属性
            ticketAttributeValuesList = getTicketAttributeValuesList(fileIO, TICKET);
        }

        for (int i = 1; i < colNum.length; i++) {
            //获取rulesCollection，用于查询规则
            MongoCollection<Document> rulesCollection = getRulesCollection(i);
            //获取ordersCollection，用于查询订单
            MongoCollection<Document> ordersCollection = getOrdersCollection(i);
            int fixedPos = -1;
            if (i == 1) {
                fixedPos = 5;
            }

            if (mode.equals("eva")) {
                for (Iterator<String> iterator = ticketOrderNumAttributeMap.keySet().iterator(); iterator.hasNext(); ) {
                    String orderNum = iterator.next();
                    String correctTargetItem = getTargetItemFromOrderNum(orderNum, i, ordersCollection);
                    //根据orderNum，查询ticketOrderNumAttributeMap中对应的属性列表
                    List<String> attributeValues = ticketOrderNumAttributeMap.get(orderNum);
                    //根据attributeValues，查询ordersCollection中对应的订单
                    Map<String, String> singleAttributeQuery =
                            singleAttributeRuleQuery(attributeValues, fixedPos, rulesCollection, i);
                    String singleItemQuery = singleItemQuery(singleAttributeQuery, ordersCollection, i);
                    String info = singleItemQuery + ":" + correctTargetItem;
                    logger.info(info);
                }
            } else if (mode.equals("generate")) {
                for (List<String> ticketAttributeValues : ticketAttributeValuesList) {
                    Map<String, String> singleAttributeQuery =
                            singleAttributeRuleQuery(ticketAttributeValues, fixedPos, rulesCollection, i);
                    String singleItemQuery = singleItemQuery(singleAttributeQuery, ordersCollection, i);
                    logger.info(singleItemQuery);
                }
            }
        }
    }

    public static Document singleAttributeFreqQuery(List<String> ticketAttributes
            , int fixedPos //-1表示没有必须满足的属性，否则表示必须满足的属性在ticketAttributes中的位置
            , MongoCollection<Document> rulesCollection) {
        Queue<AttributesSearchUnit> bfsQueue = new LinkedList<>();
        Set<Integer> haveVisited = new HashSet<>();
        DocFreqPair docFreqPair = new DocFreqPair(null, 0);
        //开始bfs搜索，设定根节点，并加入队列
        AttributesSearchUnit root = new AttributesSearchUnit(0, ticketAttributes
                , fixedPos, rulesCollection, docFreqPair, bfsQueue, haveVisited);
        haveVisited.add(listToBits(ticketAttributes));
        bfsQueue.add(root);
        int currentLevel = 0;
        while (!bfsQueue.isEmpty()) {
            AttributesSearchUnit current = bfsQueue.poll();
            if (current.getLevel() != currentLevel && docFreqPair.getDoc() != null) {
                break;
            }
            current.searchByFreq();
        }
        return docFreqPair.getDoc();
    }

    private static Map<String, String> singleAttributeRuleQuery(List<String> ticketAttributes
            , int fixedPos //-1表示没有必须满足的属性，否则表示必须满足的属性在ticketAttributes中的位置
            , MongoCollection<Document> rulesCollection
            , int type) {
        // 初始化itemAttributeMap，用于存储查询得到的属性
        Map<String, String> itemAttributeMap = new HashMap<>();
        // 初始化attributeConfidenceMap，用于存储属性与规则的置信度
        Map<String, Double> attributeConfidenceMap = new HashMap<>();
        // 初始化bfs队列，用于存储待搜索的节点
        Queue<AttributesSearchUnit> bfsQueue = new LinkedList<>();
        // 初始化haveVisited，用于存储已经搜索过的节点
        Set<Integer> haveVisited = new HashSet<>();
        // 开始bfs搜索，设定根节点，并加入队列
        AttributesSearchUnit root = new AttributesSearchUnit(ticketAttributes
                , fixedPos, rulesCollection, itemAttributeMap
                , attributeConfidenceMap, bfsQueue, haveVisited).setLevel(0);
        // 将ticketAttributes对应的状态值加入到haveVisited中
        haveVisited.add(listToBits(ticketAttributes));
        bfsQueue.add(root);

        int currentLevel = 0;
        while (!bfsQueue.isEmpty()) {
            // 取出队列中的节点
            AttributesSearchUnit current = bfsQueue.poll();
            // 检查当前节点层级是否变化
            if (current.getLevel() != currentLevel) {
                // 如果已经找到了足够的属性，则结束搜索
                if (itemAttributeMap.size() == colNum[type]) {
                    break;
                }
                currentLevel = current.getLevel();
                // 将attributeConfidenceMap中的所有值都变为2.0
                //这样在搜索的时候，不会改变上一层已经得到的属性
                attributeConfidenceMap.replaceAll((k, v) -> 2.0);
            }

            // 根据规则进行搜索
            current.searchByRules();
        }

        return itemAttributeMap;
    }

    public static String singleFreqQuery(Document document
            , MongoCollection<Document> ordersCollection, int type, Map<String, String> ticketAttributesMap) {
        Map<String, String> itemAttributeMap = new HashMap<>();
        Document itemDocument = (Document) document.get("itemAttributes");
        Document ticketDocument = (Document) document.get("ticketAttributes");
        //遍历itemDocument，将其中的每个属性键值对都加入到itemAttributeMap中
        for (String key : itemDocument.keySet()) {
            itemAttributeMap.put(key, itemDocument.getString(key));
        }
        //遍历ticketDocument，将其中的每个属性键值对都加入到itemAttributeMap中
        for (String key : ticketDocument.keySet()) {
            ticketAttributesMap.put(key, ticketDocument.getString(key));
        }
        return singleItemQuery(itemAttributeMap, ordersCollection, type);
    }

    private static String singleItemQuery(Map<String, String> itemAttributeMap
            , MongoCollection<Document> ordersCollection, int type) {
        //根据itemAttributeMap中的属性，查询ordersCollection中对应的订单
        Queue<ItemSearchUnit> bfsQueue = new LinkedList<>();
        Set<Integer> haveVisited = new HashSet<>();
        List<Map.Entry<String, String>> attributeList = map2List(itemAttributeMap, type);
        //开始bfs搜索，设定根节点，并加入队列
        ItemSearchUnit root = new ItemSearchUnit(attributeList
                , ordersCollection, type, bfsQueue, haveVisited);
        haveVisited.add(listToBits(attributeList));

        bfsQueue.add(root);
        while (!bfsQueue.isEmpty()) {
            Document item = bfsQueue.poll().search();
            if (item != null && !item.isEmpty()) {
                return item.getString(getTargetItemNames()[type]);
            }
        }
        return "";
    }

}
