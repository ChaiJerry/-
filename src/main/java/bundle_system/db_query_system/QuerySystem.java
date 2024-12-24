package bundle_system.db_query_system;

import com.mongodb.client.*;

import org.bson.*;
import bundle_system.io.*;


import java.util.*;

import static bundle_system.data_processer.DataConverter.*;


public class QuerySystem {
    private QuerySystem() {
    }


    /**
     * 获取 ticketOrderNumAttributeMap，用于获得规范的订单号与属性列表的列表的对应关系。
     * 主要是处理属性列表，对于属性较多的商品，去除其商品唯一标识符相关的属性，
     * 保留其余属性，提高模型泛化性，因此可以推荐从未见过的商品。
     *
     * @param dataForEvaluation 包含数据的 Map<String, List<List<String>>>
     * @param attributesStorage 用于处理属性名和属性对应关系的结构体
     * @return 规范化的 ticketOrderNumAttributeMap
     */
    public static Map<String, List<List<String>>> getTicketOrderNumAttributesMap(Map<String, List<List<String>>> dataForEvaluation, ItemAttributeNamesStorage attributesStorage) {
        // 读取文件，返回 Map<String, List<List<String>>>，但此时还不能使用，可能有不需要的属性或者属性顺序不对
        // 这里可以更改使用训练集还是测试集来测试
        Map<String, List<List<String>>> ticketOrderNumAttributeMap = dataForEvaluation;

        // 获取机票的 itemAttributesStorage（用于处理属性名和属性对应关系的结构体），用于获取调整后规范的属性列表
        ItemAttributeNamesStorage itemAttributeNamesStorage = attributesStorage;

        // 遍历 Map<String, List<List<String>>> 的所有键值对
        for (Iterator<String> iterator = ticketOrderNumAttributeMap.keySet().iterator(); iterator.hasNext(); ) {
            String key = iterator.next();
            List<List<String>> lists = ticketOrderNumAttributeMap.get(key);

            for (int i = 0; i < lists.size(); i++) {
                // 获取 key 对应的 List<List<String>>，即属性列表的列表
                List<String> attributeList = lists.get(i);

                // 将 attributeList 中的元素调整为规范格式
                lists.set(i, itemAttributeNamesStorage.getOrderedAttributeValueList(attributeList));
            }
        }

        return ticketOrderNumAttributeMap;
    }

    /**
     * 单个商品的查询方法，根据给定的属性映射查询 MongoDB 中对应的订单。
     *
     * @param itemAttributeMap   包含属性的映射
     * @param ordersCollection   MongoDB 的订单集合
     * @param type               品类编码
     * @return 查询到的商品名称
     */
    public static String singleItemQuery(Map<String, String> itemAttributeMap
            , MongoCollection<Document> ordersCollection, int type) {
        // 根据 itemAttributeMap 中的属性，查询 ordersCollection 中对应的订单
        Queue<BasicItemSearchUnit> bfsQueue = new LinkedList<>();
        Set<Integer> haveVisited = new HashSet<>();
        List<Map.Entry<String, String>> attributeList = map2List(itemAttributeMap);

        // 开始 BFS 搜索，设定根节点，并加入队列
        BasicItemSearchUnit root = new BasicItemSearchUnit(attributeList
                , ordersCollection, type, bfsQueue, haveVisited);
        haveVisited.add(listToBits(attributeList));

        bfsQueue.add(root);

        // 当队列不为空时继续搜索
        while (!bfsQueue.isEmpty()) {
            // 从队列中取出一个 BasicItemSearchUnit 进行搜索
            Document item = bfsQueue.poll().search();

            // 如果找到有效的文档
            if (item != null && !item.isEmpty()) {
                // 返回查询到的商品名称
                return getItemNameFromDocument(item, type);
            }
        }

        // 如果没有找到匹配的商品，返回空字符串
        return "";
    }



    /**
     * 通过机票属性，查询某个品类下的所有商品，最多返回5个（这是展示时使用的方法）。
     *
     * @param itemAttributeMap   机票属性
     * @param ordersCollection   MongoDB 的订单集合
     * @param type               品类编码
     * @return 品类下的所有商品，最多返回5个
     */
    public static Set<List<String>> itemsQuery(Map<String, String> itemAttributeMap
            , MongoCollection<Document> ordersCollection, int type) {
        // 创建一个 HashSet 来存储商品名称和价格
        Set<List<String>> itemNameAndPrices = new HashSet<>();

        // 根据 itemAttributeMap 中的属性，查询 ordersCollection 中对应的订单
        Queue<BasicItemSearchUnit> bfsQueue = new LinkedList<>();
        Set<Integer> haveVisited = new HashSet<>();
        List<Map.Entry<String, String>> attributeList = map2List(itemAttributeMap);

        // 开始 BFS 搜索，设定根节点，并加入队列
        BasicItemSearchUnit root = new BasicItemSearchUnit(attributeList
                , ordersCollection, type, bfsQueue, haveVisited);
        haveVisited.add(listToBits(attributeList));

        bfsQueue.add(root);

        // 当队列不为空时继续搜索
        while (!bfsQueue.isEmpty()) {
            // 从队列中取出一个 BasicItemSearchUnit 进行搜索
            List<Document> items = bfsQueue.poll().searchWithoutQuickReturn();

            // 如果已经找到5个商品，则停止搜索
            if (itemNameAndPrices.size() >= 5) {
                break;
            }

            // 遍历搜索结果中的每个 Document
            for (Document item : items) {
                // 如果已经找到5个商品，则停止搜索
                if (itemNameAndPrices.size() >= 5) {
                    break;
                }

                // 如果 Document 不为空且有效
                if (item != null && !item.isEmpty()) {
                    // 添加商品名称和价格到结果集中
                    itemNameAndPrices.add(getItemNameAndPriceFromDocument(item, type));
                }
            }
        }

        // 返回包含商品名称和价格的结果集
        return itemNameAndPrices;
    }


}




