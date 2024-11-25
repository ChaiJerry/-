package bundle_system.memory_query_system;

import bundle_system.io.*;

import java.util.*;
import java.util.concurrent.*;

public class QueryTask implements Callable<Void> {
    private final int queryId;
    private final List<Set<Integer>> sets;
    private final HashMap<String, ItemAttributeValueAndConfidenceAndPriority> attributesMap;
    private final Map<Integer, AssociationRuleResult> rulesMap;
    private final Set<Integer> havaVisited;

    public QueryTask(int queryId, List<Set<Integer>> sets, HashMap<String, ItemAttributeValueAndConfidenceAndPriority> attributesMap, Map<Integer, AssociationRuleResult> rulesMap, Set<Integer> threadSafeSet) {
        this.queryId = queryId;
        this.sets = sets;
        this.attributesMap = attributesMap;
        this.rulesMap = rulesMap;
        this.havaVisited = threadSafeSet;
    }

    public QueryTask(int queryId, List<Set<Integer>> sets, HashMap<String, ItemAttributeValueAndConfidenceAndPriority> attributesMap, Map<Integer, AssociationRuleResult> rulesMap) {
        this.queryId = queryId;
        this.sets = sets;
        this.attributesMap = attributesMap;
        this.rulesMap = rulesMap;
        this.havaVisited = null;
    }

    @Override
    public Void call() {
        if(havaVisited != null) {
            if (havaVisited.contains(queryId)) {
                return null;
            } else {
                havaVisited.add(queryId);
            }
        }
        int queryValue = 0;// 满足查询条件的数量
        for (int i = 0; i < sets.size(); i++) {
            if (sets.get(i).contains(queryId)) {
                queryValue++;
            }
        }
        AssociationRuleResult associationRuleResult = rulesMap.get(queryId);
        double confidence = associationRuleResult.getConfidence();
        String AttributeName = associationRuleResult.getAttributeName();
        String AttributeValue = associationRuleResult.getAttributeValue();
        attributesMap.get(AttributeName).tryAssign(AttributeValue, queryValue, confidence);
        return null;// 返回 null 表示任务完成
    }

}
