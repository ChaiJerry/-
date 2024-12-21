package bundle_system.db_query_system;

import com.mongodb.client.*;

import org.bson.*;
import bundle_system.data_processer.*;
import bundle_system.io.*;

import java.io.*;
import java.util.*;

import static bundle_system.data_processer.DataConverter.*;
import static bundle_system.io.SharedAttributes.*;
import static bundle_system.io.MongoUtils.*;

public class QuerySystem {
    private QuerySystem() {
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
    public static Map<String, List<List<String>>> getTicketOrderNumAttributesMap(Map<String, List<List<String>>> dataForEvaluation, ItemAttributesStorage attributesStorage) {
        //读取文件，返回Map<String, List<List<String>>>，但是此时还不能使用，可能有不需要的属性或者属性顺序不对
        //这里可以更改使用训练集还是测试集来测试
        Map<String, List<List<String>>> ticketOrderNumAttributeMap = dataForEvaluation;
        //获取机票的itemAttributesStorage（用于处理属性名和属性对应对应关系的结构体），用于获取调整后规范的属性列表
        ItemAttributesStorage itemAttributesStorage = attributesStorage;
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



    public static String singleItemQuery(Map<String, String> itemAttributeMap
            , MongoCollection<Document> ordersCollection, int type) {
        //根据itemAttributeMap中的属性，查询ordersCollection中对应的订单
        Queue<BasicItemSearchUnit> bfsQueue = new LinkedList<>();
        Set<Integer> haveVisited = new HashSet<>();
        List<Map.Entry<String, String>> attributeList = map2List(itemAttributeMap);
        //开始bfs搜索，设定根节点，并加入队列
        BasicItemSearchUnit root = new BasicItemSearchUnit(attributeList
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

    public static Set<List<String>> itemsQuery(Map<String, String> itemAttributeMap
            , MongoCollection<Document> ordersCollection, int type) {
        Set<List<String>> itemNameAndPrices = new HashSet<>();
        //根据itemAttributeMap中的属性，查询ordersCollection中对应的订单
        Queue<BasicItemSearchUnit> bfsQueue = new LinkedList<>();
        Set<Integer> haveVisited = new HashSet<>();
        List<Map.Entry<String, String>> attributeList = map2List(itemAttributeMap);
        //开始bfs搜索，设定根节点，并加入队列
        BasicItemSearchUnit root = new BasicItemSearchUnit(attributeList
                , ordersCollection, type, bfsQueue, haveVisited);
        haveVisited.add(listToBits(attributeList));

        bfsQueue.add(root);
        while (!bfsQueue.isEmpty()) {
            List<Document> items = bfsQueue.poll().searchWithoutQuickReturn();
            if (itemNameAndPrices.size() >= 5) {
                break;
            }
            for (Document item : items) {
                if (itemNameAndPrices.size() >= 5) {
                    break;
                }
                if (item != null && !item.isEmpty()) {
                    itemNameAndPrices.add(getItemNameAndPriceFromDocument(item, type));
                }
            }
        }
        return itemNameAndPrices;
    }




}
