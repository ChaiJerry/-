package packing_system.query_system;

import com.mongodb.client.*;
import com.mongodb.client.model.*;
import org.bson.*;
import org.bson.conversions.*;

import java.util.*;

import static com.mongodb.client.model.Projections.*;
import static packing_system.io.SharedAttributes.*;
import static packing_system.query_system.AttributesSearchUnit.*;

public class KnowledgeBaseQuery {
    public static final List<String> TICKET_ATTRIBUTE_NAMES = getItemAttributesStorage()[TRAIN_TICKET].getAttributeNames();
    private final MongoCollection<Document> collection;
    //希望查询的训练集编号
    private final int trainingNumber;

    public KnowledgeBaseQuery(MongoCollection<Document> collection, int trainingNumber) {
        this.collection = collection;
        this.trainingNumber = trainingNumber;
    }

    public List<FrequentItemSetsQueryResults> findFrequentItemSet(List<String> ticketAttributes) {
        List<Bson> bsonList = new ArrayList<>();
        //ticketAttributes是输入的机票属性列表
        for (int i = 0; i < ticketAttributes.size(); i++) {
            String[] split = ticketAttributes.get(i).split(":");
            //split[0]是属性名
            String ticketAttributeName = split[0];
            //split[1]是属性值
            String ticketAttributeValue = split[1];
            bsonList.add(Filters.eq("antecedent." + ticketAttributeName, ticketAttributeValue));
        }
        bsonList.add(Filters.eq(TRAINING_NUMBER_FIELD_NAME, trainingNumber));
        FindIterable<Document> search = collection.find(Filters.and(bsonList)).projection(fields(include(
                "consequence", "confidence"), excludeId()));
        List<FrequentItemSetsQueryResults> frequentItemSet = new ArrayList<>();
        Map<String, String> itemAttributeMap = new HashMap<>();
        for (Document document : search) {
            int freq = document.getInteger("freq");
            Document doc = (Document)document.get("itemAttributes");
            for (String key : doc.keySet()) {
                itemAttributeMap.put(key, document.getString(key));
            }
            frequentItemSet.add(new FrequentItemSetsQueryResults(itemAttributeMap, freq));
        }
        return frequentItemSet;
    }

    public List<AssociationRulesQueryResults> findAssociationRules(List<String> ticketAttributes) {
        List<Bson> bsonList = new ArrayList<>();
        //ticketAttributes是输入的机票属性列表
        for (int i = 0; i < ticketAttributes.size(); i++) {
            if(ticketAttributes.get(i)==null){
                bsonList.add(Filters.eq("antecedent." + TICKET_ATTRIBUTE_NAMES.get(i), null));
                continue;
            }

            String[] split = ticketAttributes.get(i).split(":");
            //split[0]是属性名
            String ticketAttributeName = split[0];
            //split[1]是属性值
            String ticketAttributeValue = split[1];
            bsonList.add(Filters.eq("antecedent." + ticketAttributeName, ticketAttributeValue));
        }
        bsonList.add(Filters.eq(TRAINING_NUMBER_FIELD_NAME, trainingNumber));
        FindIterable<Document> search = collection.find(Filters.and(bsonList)).projection(fields(include(
                "consequence", "confidence"), excludeId()));
        List<AssociationRulesQueryResults> associationRules = new ArrayList<>();
        for (Document document : search) {
            String consequence = document.getString("consequence");
            double confidence = document.getDouble("confidence");
            associationRules.add(new AssociationRulesQueryResults(consequence, confidence));
        }
        return associationRules;
    }

    public List<AssociationRulesQueryResults> findAssociationRulesForEvaluation(String ticketAttribute,int eva) {
        List<Bson> bsonList = new ArrayList<>();
        //ticketAttributes是输入的机票属性列表

            if(ticketAttribute==null){
                bsonList.add(Filters.eq("antecedent." + TICKET_ATTRIBUTE_NAMES.get(eva), null));
            }

            String[] split = ticketAttribute.split(":");
            //split[0]是属性名
            String ticketAttributeName = split[0];
            //split[1]是属性值
            String ticketAttributeValue = split[1];
            bsonList.add(Filters.eq("antecedent." + ticketAttributeName, ticketAttributeValue));
        bsonList.add(Filters.eq(TRAINING_NUMBER_FIELD_NAME, trainingNumber));
        FindIterable<Document> search = collection.find(Filters.and(bsonList)).projection(fields(include(
                "consequence", "confidence"), excludeId())).sort(Sorts.descending("confidence"));
        List<AssociationRulesQueryResults> associationRules = new ArrayList<>();
        if(search.first() != null){
            Document document = search.first();
            String consequence = document.getString("consequence");
            double confidence = document.getDouble("confidence");
            associationRules.add(new AssociationRulesQueryResults(consequence, confidence));
        }else{
            associationRules.add(new AssociationRulesQueryResults("null", 0));
        }
        return associationRules;
    }
}
