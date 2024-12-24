package bundle_system.db_query_system;

import com.mongodb.client.*;
import com.mongodb.client.model.*;
import org.bson.*;
import org.bson.conversions.*;

import java.util.*;

import static com.mongodb.client.model.Projections.*;
import static bundle_system.data_processer.DataConverter.*;
import static bundle_system.io.SharedAttributes.*;

/**
 * 单个商品的搜索单元，用于单个商品属性的搜索
 * 利用这个单元进行基础的商品搜索
 * ，当使用多个该单元进行bfs搜索时便可以得到想要的结果
 */
public class BasicItemSearchUnit {
    //用于单个搜索的属性
    private final List<Map.Entry<String, String>> itemAttributes;
    //指向mongoDB订单库的集合
    private final MongoCollection<Document> collection;
    //传入的bfs搜索队列，用于bfs搜索
    private final Queue<BasicItemSearchUnit> bfsQueue;
    //已经访问过的节点集合，用于bfs搜索
    private final Set<Integer> haveVisited;
    //商品类型编码，用于确定实际对字段的处理
    private final int type;

    /**
     * 构造函数，用于初始化单个商品的搜索单元
     * @param itemAttributes 单个商品属性的键值对列表
     * @param collection mongoDB订单库的集合
     * @param type 商品类型编码
     * @param bfsQueue 传入的bfs搜索队列，用于bfs搜索
     * @param haveVisited 已经访问过的节点集合，用于bfs搜索
     */
    public BasicItemSearchUnit(List<Map.Entry<String, String>> itemAttributes
            , MongoCollection<Document> collection, int type
            , Queue<BasicItemSearchUnit> bfsQueue, Set<Integer> haveVisited) {
        this.itemAttributes = itemAttributes;
        this.collection = collection;
        this.type = type;
        this.bfsQueue = bfsQueue;
        this.haveVisited = haveVisited;
    }

    /**
     * 根据商品属性在订单库之中进行单个商品的搜索
     * 如果找到了一个商品，就快速返回该商品的Document对象
     * @return 搜索到的商品信息，如果没有找到则返回空Document对象
     */
    public Document search() {
        List<Bson> bsonList = new ArrayList<>();
        //从头到尾遍历List<Map.Entry<String, String>> itemAttributes的每个键值对，将键值对添加到bsonList中
        for (Map.Entry<String, String> entry : itemAttributes) {
            if (entry != null) {
                bsonList.add(Filters.eq(ATTRIBUTES_FIELD + entry.getKey(), entry.getValue()));
            }
        }
        FindIterable<Document> search;
        if (bsonList.isEmpty()) {
            search = collection.find().projection(fields(include(
                    getTargetItemFieldNames(type)), excludeId()));
        } else {
            search = collection.find(Filters.and(bsonList)).projection(fields(include(
                    getTargetItemFieldNames(type)), excludeId()));
        }

        if (search.iterator().hasNext()) {
            return search.iterator().next();
            //需要先得到document才行
        }

        //运用状态压缩的技巧，将已经访问过的节点状态压缩到整数中
        int status = listToBits(itemAttributes);
        for (int i = 0; i < itemAttributes.size(); i++) {
            int nextStatus = setBitPos2zero(status, i);
            if (itemAttributes.get(i) == null || haveVisited.contains(nextStatus)) {
                continue;
            }
            haveVisited.add(nextStatus);
            List<Map.Entry<String, String>> temp = new ArrayList<>(itemAttributes);
            temp.set(i, null);
            bfsQueue.add(new BasicItemSearchUnit(temp, collection, type, bfsQueue, haveVisited));
        }
        return new Document();
    }

    /**
     * 根据商品属性在订单库之中进行多个符合属性的商品的搜索
     * 找到商品后不会立刻返回，而是将多个符合条件的商品都加入到结果列表中
     * @return 搜索到的商品信息列表，如果没有找到则返回空List对象
     */
    public List<Document> searchWithoutQuickReturn() {
        List<Document> result = new ArrayList<>();
        List<Bson> bsonList = new ArrayList<>();
        //从头到尾遍历List<Map.Entry<String, String>> itemAttributes的每个键值对，将键值对添加到bsonList中
        for (Map.Entry<String, String> entry : itemAttributes) {
            if (entry != null) {
                bsonList.add(Filters.eq(ATTRIBUTES_FIELD + entry.getKey(), entry.getValue()));
            }
        }
        FindIterable<Document> search;
        if (bsonList.isEmpty()) {
            search = collection.find();
        } else {
            search = collection.find(Filters.and(bsonList));
        }

        //运用状态压缩的技巧，将已经访问过的节点状态压缩到整数中
        int status = listToBits(itemAttributes);
        for (int i = 0; i < itemAttributes.size(); i++) {
            int nextStatus = setBitPos2zero(status, i);
            if (itemAttributes.get(i) == null || haveVisited.contains(nextStatus)) {
                continue;
            }
            haveVisited.add(nextStatus);
            List<Map.Entry<String, String>> temp = new ArrayList<>(itemAttributes);
            temp.set(i, null);
            bfsQueue.add(new BasicItemSearchUnit(temp, collection, type, bfsQueue, haveVisited));
        }
        MongoCursor<Document> iterator = search.iterator();
        while(iterator.hasNext()){
            result.add(iterator.next());
        }
        return result;
    }
}
