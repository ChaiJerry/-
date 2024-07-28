package query_system;

import com.mongodb.client.*;
import com.mongodb.client.model.*;
import org.bson.*;
import org.bson.conversions.*;

import java.util.*;

import static com.mongodb.client.model.Projections.*;
import static data_processer.DataConverter.*;

public class AttributesSearchUnit {
    private static final String[] ates = {"T_CARRIER",
            "T_GRADE", "T_PASSENGER", "S_SHOFARE", "MONTH", "TO"};

    private final int level;
    private final List<String> ticketAttributes;
    private final int fixedPos;
    private final MongoCollection<Document> collection;
    private final Map<String, String> itemAttributeMap;
    private final Map<String, Double> attributeConfidenceMap;
    private final Queue<AttributesSearchUnit> bfsQueue;
    private final Set<Integer> haveVisited;

    public int getLevel() {
        return level;
    }

    public AttributesSearchUnit(int level, List<String> ticketAttributes
            , int fixedPos, MongoCollection<Document> collection
            , Map<String, String> itemAttributeMap
            , Map<String, Double> attributeConfidenceMap
            , Queue<AttributesSearchUnit> bfsQueue, Set<Integer> haveVisited) {
        this.level = level;
        this.ticketAttributes = ticketAttributes;
        this.fixedPos = fixedPos;
        this.collection = collection;
        this.itemAttributeMap = itemAttributeMap;
        this.attributeConfidenceMap = attributeConfidenceMap;
        this.bfsQueue = bfsQueue;
        this.haveVisited = haveVisited;
    }

    public void search() {
        List<Bson> bsonList = new ArrayList<>();
        for (int i = 0; i < ticketAttributes.size(); i++) {
            bsonList.add(Filters.eq("antecedent." + ates[i], ticketAttributes.get(i)));
        }
        FindIterable<Document> search = collection.find(Filters.and(bsonList)).projection(fields(include(
                "consequence", "confidence"), excludeId())).sort(Sorts.descending("confidence"));
        for (Document document : search) {
            String consequence = document.getString("consequence");
            double confidence = document.getDouble("confidence");
            String[] temp = consequence.split(":");
            String attribute = temp[0];
            String attributeValue = temp[1];
            if (confidence >= attributeConfidenceMap.getOrDefault(attribute, 0.0)) {
                attributeConfidenceMap.put(attribute, confidence);
                itemAttributeMap.put(attribute, attributeValue);
            }
        }
        //运用状态压缩的技巧，将已经访问过的节点状态压缩到整数中
        int status = listToBits(ticketAttributes);
        for (int i = 0; i < ates.length; i++) {
            int nextStatus = setBitPos2zero(status, i);
            if (ticketAttributes.get(i) == null || haveVisited.contains(nextStatus) || i == fixedPos) {
                continue;
            }
            haveVisited.add(nextStatus);
            List<String> temp = new ArrayList<>(ticketAttributes);
            temp.set(i, null);
            bfsQueue.add(
                    new AttributesSearchUnit(level + 1
                            , temp, fixedPos, collection, itemAttributeMap
                            , attributeConfidenceMap, bfsQueue, haveVisited));
        }
    }

    @Override
    public String toString() {
        int length = 6;
        int number = listToBits(ticketAttributes);
        // 将整数转换为二进制字符串
        String binary = Integer.toBinaryString(number);
        // 计算需要补多少个0
        int zerosToAdd = length - binary.length();
        // 如果不需要补0，则直接返回原字符串
        if (zerosToAdd <= 0) {
            return binary.substring(0, length); // 确保不会超出原字符串长度
        }
        // 使用StringBuilder来高效地构建结果字符串
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < zerosToAdd; i++) {
            sb.append('0');
        }
        sb.append(binary);

        // 如果结果字符串仍然超出指定长度，则截断它
        return sb.substring(0, length);
    }

}
