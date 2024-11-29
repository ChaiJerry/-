package bundle_system.db_query_system;

import bundle_system.memory_query_system.*;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import org.bson.*;
import org.bson.conversions.*;

import java.util.*;

import static com.mongodb.client.model.Projections.*;
import static bundle_system.data_processer.DataConverter.*;
import static bundle_system.io.SharedAttributes.*;

public class AdvancedItemSearchUnit {
    //用于单个搜索的属性
    private final List<Map.Entry<String, AttrValueConfidencePriority>> itemAttributesAndConf;
    //用于多个搜索的属性
    private final MongoCollection<Document> collection;
    private final Queue<AdvancedItemSearchUnit> bfsQueue;
    private final Set<Integer> haveVisited;
    private final int type;


    public AdvancedItemSearchUnit(List<Map.Entry<String, AttrValueConfidencePriority>> attributeAndConfList
            , MongoCollection<Document> collection
            , int type, Queue<AdvancedItemSearchUnit> bfsQueue
            , Set<Integer> haveVisited) {
        this.itemAttributesAndConf = attributeAndConfList;
        this.collection = collection;
        this.type = type;
        this.bfsQueue = bfsQueue;
        this.haveVisited = haveVisited;
    }

    public Document search() {
        List<Bson> bsonList = new ArrayList<>();
        //从头到尾遍历List<Map.Entry<String, String>> itemAttributes的每个键值对，将键值对添加到bsonList中
        for (Map.Entry<String, AttrValueConfidencePriority> entry : itemAttributesAndConf) {
            if (entry != null) {
                bsonList.add(Filters.eq(ATTRIBUTES_FIELD + entry.getKey(), entry.getValue().getAttributeValue()));
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
        int status = listToBits(itemAttributesAndConf);
        for (int i = 0; i < itemAttributesAndConf.size(); i++) {
            int nextStatus = setBitPos2zero(status, i);
            if (itemAttributesAndConf.get(i) == null || haveVisited.contains(nextStatus)) {
                continue;
            }
            haveVisited.add(nextStatus);
            List<Map.Entry<String, AttrValueConfidencePriority>> temp = new ArrayList<>(itemAttributesAndConf);
            temp.set(i, null);
            bfsQueue.add(new AdvancedItemSearchUnit(temp, collection, type, bfsQueue, haveVisited));
        }
        return new Document();
    }

}
