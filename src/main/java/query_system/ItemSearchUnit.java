package query_system;

import com.mongodb.client.*;
import com.mongodb.client.model.*;
import org.bson.*;
import org.bson.conversions.*;

import java.util.*;

import static com.mongodb.client.model.Projections.*;
import static data_processer.DataConverter.*;
import static io.IOMonitor.*;

public class ItemSearchUnit {
    private final List<Map.Entry<String, String>> itemAttributes;
    private final MongoCollection<Document> collection;
    private final Queue<ItemSearchUnit> bfsQueue;
    private final Set<Integer> haveVisited;
    private final int type;

    public ItemSearchUnit(List<Map.Entry<String, String>> itemAttributes
            , MongoCollection<Document> collection, int type
            , Queue<ItemSearchUnit> bfsQueue, Set<Integer> haveVisited) {
        this.itemAttributes = itemAttributes;
        this.collection = collection;
        this.type = type;
        this.bfsQueue = bfsQueue;
        this.haveVisited = haveVisited;
    }

    public Document search() {
        List<Bson> bsonList = new ArrayList<>();
        //从头到尾遍历List<Map.Entry<String, String>> itemAttributes的每个键值对，将键值对添加到bsonList中
        for (Map.Entry<String, String> entry : itemAttributes) {
            if (entry != null) {
                bsonList.add(Filters.eq("attributes." + entry.getKey(), entry.getValue()));
            }
        }
        FindIterable<Document> search;
        if (bsonList.isEmpty()) {
            search = collection.find().projection(fields(include(
                    "attributes" + targetItemNames[type]), excludeId()));
        } else {
            search = collection.find(Filters.and(bsonList)).projection(fields(include(
                    "attributes." + targetItemNames[type]), excludeId()));
        }

        if (search.iterator().hasNext()) {
            Document next = search.iterator().next();
            //return next.get("attributes");
            //需要先得到document才行
        }

        //运用状态压缩的技巧，将已经访问过的节点状态压缩到整数中
        int status = listToBits(itemAttributes);
        for (int i = 0; i < itemAttributes.size(); i++) {
            int nextStatus = setBitPos2zero(status, i);
            if (itemAttributes.get(i) != null || haveVisited.contains(nextStatus)) {
                continue;
            }
            haveVisited.add(nextStatus);
            List<Map.Entry<String, String>> temp = new ArrayList<>(itemAttributes);
            temp.set(i, null);
            bfsQueue.add(new ItemSearchUnit(temp, collection, type, bfsQueue, haveVisited));
        }
        return new Document();
    }

}