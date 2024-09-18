package packing_system.query_system;

import com.mongodb.client.*;

import org.bson.*;
import packing_system.data_processer.*;
import packing_system.io.*;

import java.io.*;
import java.util.*;
import java.util.logging.*;

import static packing_system.data_processer.DataConverter.*;
import static packing_system.io.SharedAttributes.*;
import static packing_system.io.MongoUtils.*;

public class QuerySystem {
    private QuerySystem() {
    }

    private static final int[] colNum = {0, 5, 4, 2, 4, 2};

    private static Map<String, ItemPack> itemPackMap = new HashMap<>();

    private static final Logger logger;

    static {
        logger = Logger.getLogger(QuerySystem.class.getName());
    }

    public static List<List<String>> getTicketAttributeValuesList(CSVFileIO fileIO, int type) throws IOException {
        Map<String, List<List<String>>> stringListMap = fileIO.read(type);
        //遍历Map<String, List<String>> stringListMap的所有值
        List<List<String>> ticketAttributeValuesList = new ArrayList<>();
        ItemAttributesStorage itemAttributesStorage = getItemAttributesStorage()[type];
        for (List<List<String>> lists : stringListMap.values()) {
            ticketAttributeValuesList.add(itemAttributesStorage.getOrderedAttributeValueList(lists.get(0)));
        }
        return ticketAttributeValuesList;
    }

    /**
     * 获取ticketOrderNumAttributeMap，用于获得规范的订单号与属性列表的列表的对应关系
     * （主要是处理属性列表，对于属性较多的商品，去除其商品唯一标识符相关的属性，
     * 保留其余属性，提高模型泛化性，因此可以推荐从未见过的商品）
     */
    public static Map<String, List<List<String>>> getTicketOrderNumAttributesMap() {
        //读取文件，返回Map<String, List<List<String>>>，但是此时还不能使用，可能有不需要的属性或者属性顺序不对
        //这里可以更改使用训练集还是测试集来测试
        Map<String, List<List<String>>> ticketOrderNumAttributeMap = getTestTicketMap();
        //获取机票的itemAttributesStorage（用于处理属性名和属性对应对应关系的结构体），用于获取调整后规范的属性列表
        ItemAttributesStorage itemAttributesStorage = getItemAttributesStorage()[TICKET];
        //遍历Map<String, List<List<String>>> itemAttributesStorage的所有键值对
        for (Iterator<String> iterator = ticketOrderNumAttributeMap.keySet().iterator(); iterator.hasNext(); ) {
            String key = iterator.next();
            List<List<String>> lists = ticketOrderNumAttributeMap.get(key);
            for (int i = 0; i < lists.size(); i++) {
                //获取key对应的List<List<String>>，即属性列表的列表
                List<String> attributeList = lists.get(i);
                //将attributeList中的元素调整为规范格式
                lists.set(i, itemAttributesStorage.getOrderedAttributeValueList(attributeList));
            }
        }
        return ticketOrderNumAttributeMap;
    }

    public static void queryTest() {
        //初始化各个商品所具有的的属性名称
        initializeItemAttributesStorages();

        //评估模式获取ticketOrderNumAttributeMap，用于查询订单号以及属性
        Map<String, List<List<String>>> ticketOrderNumAttributeMap = getTicketOrderNumAttributesMap();
        ItemAttributesStorage ticketAttributesStorage = getItemAttributesStorage()[TICKET];
        for (int i = 1; i < colNum.length; i++) {
            itemPackMap.clear();
            //获取rulesCollection，用于查询规则
            MongoCollection<Document> rulesCollection = getRulesCollection(i);
            int latestTrainingNumber = getLatestTrainingNumber();
            KnowledgeBaseQuery knowledgeBaseQuery = new KnowledgeBaseQuery(rulesCollection,latestTrainingNumber);
            //获取ordersCollection，用于查询订单
            MongoCollection<Document> ordersCollection = getOrdersCollection(i);
            double total = 0;
            for (Iterator<String> iterator = ticketOrderNumAttributeMap.keySet().iterator(); iterator.hasNext(); ) {
                //得到orderNum（订单号）
                String orderNum = iterator.next();
                List<String> correctTargetItems = getTargetItemFromOrderNum(orderNum, i, ordersCollection);
                if (correctTargetItems.isEmpty()) {
                    continue;
                }
                //根据orderNum，查询ticketOrderNumAttributeMap中对应的属性值列表的列表
                List<List<String>> listOfTicketAttributeList = ticketOrderNumAttributeMap.get(orderNum);
                //遍历listOfTicketAttributeList（属性值列表的列表）
                for (List<String> attributeValues : listOfTicketAttributeList) {
                    //得到每个属性值列表
                    total++;
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
                    //根据attributeValues，查询ordersCollection中对应的订单
                    Map<String, String> singleAttributeQuery =
                            generateAttributeBundleByAssociationRules(ticketAttributesStorage
                                            .generateOrderedAttributeListFromAttributeValueList(attributeValues)
                                    , knowledgeBaseQuery, i);
                    //通过singleAttributeQuery得到的打包属性列表，查询ordersCollection中订单存在的打包商品
                    String singleItemQuery = singleItemQuery(singleAttributeQuery, ordersCollection, i);
                    //加入原订单中同时出现的商品
                    itemPack.addOrderItem(correctTargetItems, i);
                    //加入推荐系统推荐的商品
                    itemPack.addRecommendedItem(singleItemQuery, i);
                    //String info = orderNum + ":" + singleItemQuery + " : " + correctTargetItems;
                    //logger.info(info);
                }
            }
            String info = getFullNames()[i] + "有效测试订单条数共: " + total;
            logger.info(info);
            Evaluator evaluator = new Evaluator(itemPackMap);
            double averageAccuracy = evaluator.getAverageAccuracy();
            double averageRecallRate = evaluator.getAverageRecallRate();
            String accuracyInfo = "averageAccuracy: " + averageAccuracy;
            String recallInfo = "averageRecallRate: " + averageRecallRate;
            logger.info(accuracyInfo);
            logger.info(recallInfo);
        }
    }

    //public static List<String> generate

    private static int getFixedPos(int i) {
        int fixedPos = -1;
        if (i == 1) {
            fixedPos = 3;
        }
        return fixedPos;
    }

    public static Document generateAttributeBundleByFrequentItemSets(List<String> ticketAttributes
            , int fixedPos //-1表示没有必须满足的属性，否则表示必须满足的属性在ticketAttributes中的位置
            , MongoCollection<Document> frequentItemSetsCollection) {
        Queue<AttributesSearchUnit> bfsQueue = new LinkedList<>();
        Set<Integer> haveVisited = new HashSet<>();
        DocFreqPair docFreqPair = new DocFreqPair(null, 0);
        //开始bfs搜索，设定根节点，并加入队列
        AttributesSearchUnit root = new AttributesSearchUnit(0, ticketAttributes
                , fixedPos, frequentItemSetsCollection, docFreqPair, bfsQueue, haveVisited);
        haveVisited.add(listToBits(ticketAttributes));
        bfsQueue.add(root);
        //从最新的知识库的数据开始搜索，直到找到足够的属性或者所有规则都搜索完毕
        int latestTrainingNumber = getLatestTrainingNumber();
        int currentLevel = 0;
        while (!bfsQueue.isEmpty()) {
            AttributesSearchUnit current = bfsQueue.poll();
            if (current.getLevel() != currentLevel && docFreqPair.getDoc() != null) {
                break;
            }
            current.searchByFreq(latestTrainingNumber);
        }
        return docFreqPair.getDoc();
    }

    /**
     * 该方法用于通过给定的机票属性得到打包商品的属性
     * 通过给定的机票属性匹配关联规则前件，然后取出匹配的关联规则的后件作为打包商品属性，若是没有找到则减少需要匹配的机票属性，直到找到满足条件的属性或者所有规则都搜索完毕
     * @param ticketAttributes 查询的机票属性列表，List<String>中的String格式为"属性名:属性值"
     * @return Map<String, String> 键为打包商品属性名，值为打包商品属性值
     */
    private static Map<String, String> generateAttributeBundleByAssociationRules(List<String> ticketAttributes
            , KnowledgeBaseQuery knowledgeBaseQuery
            , int type) {
        // 初始化itemAttributeMap，用于存储查询得到的属性
        Map<String, String> itemAttributeMap = new HashMap<>();
        // 初始化attributeConfidenceMap，用于存储属性与规则的置信度
        Map<String, Double> attributeConfidenceMap = new HashMap<>();
        // 初始化bfs队列，用于存储待搜索的节点
        Queue<AttributesSearchUnit> bfsQueue = new LinkedList<>();
        // 初始化haveVisited，用于存储已经搜索过的节点
        Set<Integer> haveVisited = new HashSet<>();
        // 通过类型获得必须要满足的属性在ticketAttributes中的位置
        int fixedPos = getFixedPos(type);
        // 对于实际应用中若是使用数据库查询可以直接通过聚类等方式通过检索满足条件的订单属性查询得到需要的关联规则（得视具体使用的数据库确定）
        // 下面的bfs加状态压缩的搜索方式是为了演示匹配多个属性然后逐渐回退的方式查询希望得到的关联规则，同时该方法可以通用到其他类型的查询方式（如直接查询内存）
        // 在实际使用之中还是应当针对具体使用的查询方式进行优化修改
        // 开始bfs搜索，设定根节点，并加入队列
        AttributesSearchUnit root = new AttributesSearchUnit(ticketAttributes
                , fixedPos, knowledgeBaseQuery, itemAttributeMap
                , attributeConfidenceMap, bfsQueue, haveVisited).setLevel(0);
        // 将ticketAttributes对应的状态值加入到haveVisited中
        haveVisited.add(listToBits(ticketAttributes));
        bfsQueue.add(root);
        //从最新的知识库的数据开始搜索，直到找到足够的属性或者所有规则都搜索完毕
        int latestTrainingNumber = getLatestTrainingNumber();
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
            current.searchByRules(latestTrainingNumber);
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

    //从属性名属性值键值对之中尝试直接得到商品唯一标识符
    private static String tryGetItemNameDirectlyFromMap(Map<String, String> itemAttributeMap, int type) {
        //酒店的属性较多，适合直接属性查询
        if (type == HOTEL) {
            return "";
        }
        StringBuilder itemName = new StringBuilder();
        //从type种类得到需要的商品唯一标识符的信息（属性名）
        List<String> targetItemNames = getTargetItemNames(type);
        //遍历targetItemNames，如果itemAttributeMap中存在全部对应的属性，
        //则将其加入到itemName中并返回，否则返回空串
        for (String itemNameStr : targetItemNames) {
            if (itemAttributeMap.containsKey(itemNameStr)) {
                itemName.append(itemAttributeMap.get(itemNameStr)).append(";");
            } else {
                return "";
            }
        }
        //当所有属性都存在时，返回itemName(商品唯一标识符)
        return itemName.toString();
    }

    private static String singleItemQuery(Map<String, String> itemAttributeMap
            , MongoCollection<Document> ordersCollection, int type) {
        String itemName = tryGetItemNameDirectlyFromMap(itemAttributeMap, type);
        //如果直接得到商品唯一标识符，则不进行bfs查询操作，直接返回唯一标识符即可
        if (!itemName.isEmpty()) {
            return itemName;
        }

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
                return getItemNameFromDocument(item, type);
            }
        }
        return "";
    }

}
