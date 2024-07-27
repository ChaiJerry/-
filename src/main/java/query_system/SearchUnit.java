package query_system;

import com.mongodb.client.*;
import com.mongodb.client.model.*;
import org.bson.*;
import org.bson.conversions.*;

import java.util.*;

import static com.mongodb.client.model.Projections.*;

public class SearchUnit {
    private static final String[] ates = {"T_CARRIER",
            "T_GRADE", "T_PASSENGER", "S_SHOFARE", "MONTH", "TO"};

    private final int level;
    private final List<String>  ticketAttributes;
    private final int fixedPos;
    private final MongoCollection<Document> collection;
    private final Map<String, String> itemAttributeMap;
    private final Map<String, Integer> attributeConfidenceMap;
    private final Queue<SearchUnit> bfsQueue;

    public int getLevel() {
        return level;
    }


    public SearchUnit(int level, List<String> ticketAttributes
            , int fixedPos, MongoCollection<Document> collection
            , Map<String, String> itemAttributeMap
            , Map<String, Integer> attributeConfidenceMap
            , Queue<SearchUnit> bfsQueue) {
        this.level = level;
        this.ticketAttributes = ticketAttributes;
        this.fixedPos = fixedPos;
        this.collection = collection;
        this.itemAttributeMap = itemAttributeMap;
        this.attributeConfidenceMap = attributeConfidenceMap;
        this.bfsQueue = bfsQueue;
    }

    public void search() {
        List<Bson> bsonList = new ArrayList<>();
        for (int i = 0; i < ticketAttributes.size(); i++) {
            bsonList.add(Filters.eq("antecedent."+ates[i],ticketAttributes.get(i)));
        }
        FindIterable<Document> search = collection.find(Filters.and(bsonList)).projection(fields(include(
                "consequence", "confidence"), excludeId()));
        while (search.iterator().hasNext()) {
            Document document = search.iterator().next();
            String consequence = document.getString("consequence");
            int confidence = document.getInteger("confidence");
            String[] temp = consequence.split(":");
            String attribute = temp[0];
            String attributeValue = temp[1];
            if (confidence >= attributeConfidenceMap.getOrDefault(attribute,0)) {
                attributeConfidenceMap.put(attribute, confidence);
                itemAttributeMap.put(attribute, attributeValue);
            }
        }

        for(int i = 0;i < ates.length;i++) {
            if(i==fixedPos) {
                continue;
            }
            if(ticketAttributes.get(i)!=null){
                List<String> temp = new ArrayList<>(ticketAttributes);
                temp.set(i,null);
                bfsQueue.add(
                        new SearchUnit(level+1
                        , temp ,fixedPos,collection,itemAttributeMap
                        ,attributeConfidenceMap,bfsQueue));
            }
        }
    }
}
