package bundle_system.db_query_system;

import com.mongodb.client.*;
import com.mongodb.client.model.*;

import org.bson.*;
import org.bson.conversions.*;
import bundle_system.data_processer.*;

import java.util.*;

import static bundle_system.data_processer.DataConverter.*;
import static bundle_system.io.SharedAttributes.*;

public class AttributesSearchUnit {
    //机票的属性，用于搜索中作为前件
//    protected static final String[] ates = {"T_CARRIER", "T_GRADE", "S_SHOFARE"
//            , "MONTH", "TO" , "FROM" , "HAVE_CHILD"};
    protected static final String[] ates = getItemAttributesStorage()[TRAIN_TICKET].getAttributeNames().toArray(new String[0]);
    private int level = 0;
    private final List<String> ticketAttributes;
    private final int fixedPos;
    private MongoCollection<Document> collection =null;
    private Map<String, String> itemAttributeMap = null;
    private Map<String, Double> attributeConfidenceMap = null;
    private DocFreqPair docFreqMap = null;
    private final Queue<AttributesSearchUnit> bfsQueue;
    private final Set<Integer> haveVisited;

    private KnowledgeBaseQuery knowledgeBaseQuery;

    public int getLevel() {
        return level;
    }

    public AttributesSearchUnit(List<String> ticketAttributes
            , int fixedPos,KnowledgeBaseQuery knowledgeBaseQuery
            , Map<String, String> itemAttributeMap
            , Map<String, Double> attributeConfidenceMap
            , Queue<AttributesSearchUnit> bfsQueue, Set<Integer> haveVisited) {
        this.ticketAttributes = ticketAttributes;
        this.fixedPos = fixedPos;
        this.knowledgeBaseQuery = knowledgeBaseQuery;
        this.itemAttributeMap = itemAttributeMap;
        this.attributeConfidenceMap = attributeConfidenceMap;
        this.bfsQueue = bfsQueue;
        this.haveVisited = haveVisited;
    }

    public AttributesSearchUnit setLevel(int level) {
        this.level = level;
        return this;
    }

    public AttributesSearchUnit(int level, List<String> ticketAttributes
            , int fixedPos, MongoCollection<Document> collection
            , DocFreqPair docFreqMap
            , Queue<AttributesSearchUnit> bfsQueue, Set<Integer> haveVisited) {
        this.level = level;
        this.ticketAttributes = ticketAttributes;
        this.fixedPos = fixedPos;
        this.collection = collection;
        this.docFreqMap = docFreqMap;
        this.bfsQueue = bfsQueue;
        this.haveVisited = haveVisited;
    }

    /**
     * 根据关联规则搜索对应item的属性
     */
    public void searchByRules(int trainingNumber) {
        List<AssociationRulesQueryResults> associationRulesResults = knowledgeBaseQuery.findAssociationRules(ticketAttributes);
        for(AssociationRulesQueryResults associationRule : associationRulesResults) {
            String consequence = associationRule.getConsequence();
            double confidence = associationRule.getConfidence();
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
            List<String> temp = checkViability(status, i);
            if (temp.isEmpty()) {
                continue;
            }
            bfsQueue.add(new AttributesSearchUnit(temp, fixedPos, knowledgeBaseQuery, itemAttributeMap
                    , attributeConfidenceMap, bfsQueue, haveVisited).setLevel(level + 1));
        }
    }

    /**
     * 根据关联规则搜索对应item的属性
     */
    public void searchByRulesForEvaluation(int trainingNumber) {
        List<AssociationRulesQueryResults> associationRulesResults = knowledgeBaseQuery.findAssociationRules(ticketAttributes);
        for(AssociationRulesQueryResults associationRule : associationRulesResults) {
            String consequence = associationRule.getConsequence();
            double confidence = associationRule.getConfidence();
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
        for (int i = 0; i < 1; i++) {
            List<String> temp = checkViability(status, i);
            if (temp.isEmpty()) {
                continue;
            }
            bfsQueue.add(new AttributesSearchUnit(temp, fixedPos, knowledgeBaseQuery, itemAttributeMap
                    , attributeConfidenceMap, bfsQueue, haveVisited).setLevel(level + 1));
        }
    }

    public void searchByFreq(int trainingNumber) {
        List<Bson> bsonList = new ArrayList<>();
        for (int i = 0; i < ticketAttributes.size(); i++) {
            bsonList.add(Filters.eq("ticketAttributes." + ates[i], ticketAttributes.get(i)));
        }
        bsonList.add(Filters.eq(TRAINING_NUMBER_FIELD_NAME, trainingNumber));
        FindIterable<Document> search = collection
                .find(Filters.and(bsonList)).sort(Sorts.descending("freq"));

        if (search.iterator().hasNext()) {
            Document document = search.iterator().next();
            int freq = document.getInteger("freq");
            if (freq > docFreqMap.getFreq()) {
                docFreqMap.setFreq(freq);
                docFreqMap.setDoc(document);
            }
        }

        //运用状态压缩的技巧，将已经访问过的节点状态压缩到整数中
        int status = listToBits(ticketAttributes);
        for (int i = 0; i < ates.length; i++) {
            List<String> temp = checkViability(status, i);
            if (temp.isEmpty()) {
                continue;
            }
            bfsQueue.add(new AttributesSearchUnit(level + 1
                    , temp, fixedPos, collection, docFreqMap
                    , bfsQueue, haveVisited));
        }
    }

    private List<String> checkViability(int status, int i) {
        int nextStatus = setBitPos2zero(status, i);
        if (ticketAttributes.get(i) == null || haveVisited.contains(nextStatus) || i == fixedPos) {
            return new ArrayList<>();
        }
        haveVisited.add(nextStatus);
        List<String> temp = new ArrayList<>(ticketAttributes);
        temp.set(i, null);
        return temp;
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
