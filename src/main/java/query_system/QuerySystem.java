package query_system;

import com.mongodb.client.*;
import org.bson.*;

import java.util.*;

import static io.MongoUtils.*;

public class QuerySystem {
    private static int[] colNum = {0,5,4,2,4,2};

    public void queryTest(){
        for(int i=0;i<colNum.length;i++){
            MongoCollection<Document> collection = getRulesCollection(i);
            List<String> ticketAttributes = Arrays.asList("a","b","c","d","e");
            int fixedPos = 3;
            singleQuery(ticketAttributes,fixedPos,collection,i);
        }
    }


    private Map<String, String> singleQuery(List<String> ticketAttributes
            , int fixedPos
            , MongoCollection<Document> collection
            , int type) {
        Map<String, String> itemAttributeMap = new HashMap<>();
        Map<String, Integer> attributeConfidenceMap = new HashMap<>();
        Queue<SearchUnit> bfsQueue = new LinkedList<>();
        SearchUnit root = new SearchUnit(0, ticketAttributes
                , fixedPos, collection, itemAttributeMap
                , attributeConfidenceMap, bfsQueue);
        bfsQueue.add(root);
        int currentLevel = 0;
        while (!bfsQueue.isEmpty()) {
            SearchUnit current = bfsQueue.poll();
            if (current.getLevel() != currentLevel) {
                if(itemAttributeMap.size() == colNum[type]){
                    break;
                }
               currentLevel = current.getLevel();
               //将attributeConfidenceMap中的所有值都变为2，这样在搜索的时候，不会改变上一层已经得到的属性
                attributeConfidenceMap.replaceAll((k, v) -> 2);
            }
            current.search();
        }
        return itemAttributeMap;
    }

}
