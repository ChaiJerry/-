package query_system;

import com.mongodb.client.*;
import io.*;
import org.bson.*;

import java.io.*;
import java.util.*;

import static io.IOMonitor.*;
import static io.MongoUtils.*;

public class QuerySystem {
    private static final int[] colNum = {0, 5, 4, 2, 4, 2};

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

    public static void queryTest() throws IOException {
        CSVFileIO fileIO = new CSVFileIO(resultDirPath, pathT, pathH, pathM, pathB, pathI, pathS);
        for (int i = 0; i < colNum.length; i++) {
            fileIO.read(i);
        }
        List<List<String>> ticketAttributeValuesList = getTicketAttributeValuesList(fileIO, TICKET);
        for (int i = 1; i < colNum.length; i++) {
            //获取rulesCollection，用于查询规则
            MongoCollection<Document> rulesCollection = getRulesCollection(i);
            //获取ordersCollection，用于查询订单
            MongoCollection<Document> ordersCollection = getOrdersCollection(i);
            int fixedPos = -1;
            if (i == 1) {
                fixedPos = 5;
            }
            for (List<String> ticketAttributeValues : ticketAttributeValuesList) {
                Map<String, String> singleAttributeQuery =
                        singleAttributeQuery(ticketAttributeValues, fixedPos, rulesCollection, i);
                String singleItemQuery = singleItemQuery(singleAttributeQuery, ordersCollection, i);
                System.out.println(singleItemQuery);
            }
        }
    }

    private static Map<String, String> singleAttributeQuery(List<String> ticketAttributes
            , int fixedPos //-1表示没有必须满足的属性，否则表示必须满足的属性在ticketAttributes中的位置
            , MongoCollection<Document> RulesCollection
            , int type) {
        Map<String, String> itemAttributeMap = new HashMap<>();
        Map<String, Double> attributeConfidenceMap = new HashMap<>();
        Queue<AttributesSearchUnit> bfsQueue = new LinkedList<>();
        Set<Integer> haveVisited = new HashSet<>();
        //开始bfs搜索，设定根节点，并加入队列
        AttributesSearchUnit root = new AttributesSearchUnit(0, ticketAttributes
                , fixedPos, RulesCollection, itemAttributeMap
                , attributeConfidenceMap, bfsQueue, haveVisited);
        haveVisited.add(-1 + ticketAttributes.size());
        bfsQueue.add(root);
        int currentLevel = 0;
        while (!bfsQueue.isEmpty()) {
            AttributesSearchUnit current = bfsQueue.poll();
            if (current.getLevel() != currentLevel) {
                if (itemAttributeMap.size() == colNum[type]) {
                    break;
                }
                currentLevel = current.getLevel();
                //将attributeConfidenceMap中的所有值都变为2，这样在搜索的时候，不会改变上一层已经得到的属性
                attributeConfidenceMap.replaceAll((k, v) -> 2.0);
            }
            current.search();
        }
        return itemAttributeMap;
    }

    private static String singleItemQuery(Map<String, String> itemAttributeMap, MongoCollection<Document> ordersCollection, int type) {
        //根据itemAttributeMap中的属性，查询ordersCollection中对应的订单
        Queue<ItemSearchUnit> bfsQueue = new LinkedList<>();
        Set<Integer> haveVisited = new HashSet<>();
        //开始bfs搜索，设定根节点，并加入队列
        ItemSearchUnit root = new ItemSearchUnit(map2List(itemAttributeMap, type)
                , ordersCollection, type, bfsQueue, haveVisited);
        haveVisited.add(-1 + itemAttributeMap.size());
        bfsQueue.add(root);
        while (!bfsQueue.isEmpty()) {
            Document item = bfsQueue.poll().search();
            if (!item.isEmpty()) {
                return item.getString("attributes."+targetItemNames[type]);
            }
        }
        return "";
    }

    private static List<Map.Entry<String, String>> map2List(Map<String, String> map, int type) {
        switch (type) {
            case HOTEL:
                map.remove("HOTEL_NAME");
                break;
            case MEAL:
                map.remove("MEAL_NAME");
                map.remove("MEAL_CODE");
                break;
            case BAGGAGE:
                break;
            case INSURANCE:
                map.remove("INSURANCE_COMPANY");
                map.remove("INSURANCE_COMPANYCODE");
                break;
            case SEAT:
                break;
            default:
                break;
        }

        return new ArrayList<>(map.entrySet());
    }

}
