package query_system;

import com.mongodb.client.*;
import io.*;
import org.bson.*;

import java.io.*;
import java.util.*;

import static data_processer.DataConverter.*;
import static io.IOMonitor.*;
import static io.MongoUtils.*;


public class QuerySystem {
    private static final int[] colNum = {0, 5, 4, 2, 4, 2};

    private final static String mode = "eva";

    private static List<List<String>> ticketAttributeValuesList = null;

    private static Map<String, List<String>> ticketOrderNumAttributeMap = null;

    private double[] seperateCoOrderNum = new double[]{0, 0, 0, 0, 0, 0};

    private static List<String> strs = new ArrayList<>();

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
        Map<String, List<String>> TicketOrderNumAttributeMap = fileIO.read(type);
        //获取HeaderStorage，用于获取调整后规范的属性列表
        HeaderStorage headerStorage = getHeaderStorage()[type];
        //遍历Map<String, List<String>> stringListMap的所有键值对
        for (String key : TicketOrderNumAttributeMap.keySet()) {
            //获取key对应的List<String>，即属性列表
            List<String> attributeList = TicketOrderNumAttributeMap.get(key);
            //将attributeList中的元素调整为规范格式
            TicketOrderNumAttributeMap.put(key, headerStorage.getAttributeLists(attributeList));
        }
        return TicketOrderNumAttributeMap;
    }

    public static List<String> queryTest2() throws IOException {
        CSVFileIO fileIO = new CSVFileIO(resultDirPath, pathT, pathH, pathM, pathB, pathI, pathS);
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

        String head = "orderNum,Ticket,Hotel,Meal,Baggage,Insurance,Seat";
        strs.add(head);
        MongoCollection<Document> ticketOrdersCollection = getOrdersCollection(TICKET);
        for (String orderNum : ticketOrderNumAttributeMap.keySet()) {
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
                //根据orderNum，查询ticketOrderNumAttributeMap中对应的属性列表
                List<String> attributeValues = ticketOrderNumAttributeMap.get(orderNum);
                //根据attributeValues，查询ordersCollection中对应的订单
                Map<String, String> singleAttributeQuery =
                        singleAttributeQuery(attributeValues, fixedPos, rulesCollection, i);
                String singleItemQuery = singleItemQuery(singleAttributeQuery, ordersCollection, i);
                sb.append(singleItemQuery).append(":").append(correctTargetItem).append(",");
            }
            strs.add(sb.toString());
        }

        return strs;
    }

    public static void queryTest() throws IOException {
        CSVFileIO fileIO = new CSVFileIO(resultDirPath, pathT, pathH, pathM, pathB, pathI, pathS);
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
                for (String orderNum : ticketOrderNumAttributeMap.keySet()) {
                    String correctTargetItem = getTargetItemFromOrderNum(orderNum, i, ordersCollection);
                    //根据orderNum，查询ticketOrderNumAttributeMap中对应的属性列表
                    List<String> attributeValues = ticketOrderNumAttributeMap.get(orderNum);
                    //根据attributeValues，查询ordersCollection中对应的订单
                    Map<String, String> singleAttributeQuery =
                            singleAttributeQuery(attributeValues, fixedPos, rulesCollection, i);
                    String singleItemQuery = singleItemQuery(singleAttributeQuery, ordersCollection, i);
                    System.out.println(singleItemQuery + ":" + correctTargetItem);
                }

            } else if (mode.equals("generate")) {
                for (List<String> ticketAttributeValues : ticketAttributeValuesList) {
                    Map<String, String> singleAttributeQuery =
                            singleAttributeQuery(ticketAttributeValues, fixedPos, rulesCollection, i);
                    String singleItemQuery = singleItemQuery(singleAttributeQuery, ordersCollection, i);
                    System.out.println(singleItemQuery);
                }
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
        haveVisited.add(listToBits(ticketAttributes));
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
        List<Map.Entry<String, String>> attributeList = map2List(itemAttributeMap, type);
        //开始bfs搜索，设定根节点，并加入队列
        ItemSearchUnit root = new ItemSearchUnit(attributeList
                , ordersCollection, type, bfsQueue, haveVisited);
        haveVisited.add(listToBits(attributeList));

        bfsQueue.add(root);
        while (!bfsQueue.isEmpty()) {
            Document item = bfsQueue.poll().search();
            if (item != null && !item.isEmpty()) {
                return item.getString(targetItemNames[type]);
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
