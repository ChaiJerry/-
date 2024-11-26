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
    private final List<Set<Integer>> nullSets;

    public QueryTask(int queryId, List<Set<Integer>> sets, HashMap<String, ItemAttributeValueAndConfidenceAndPriority> attributesMap, Map<Integer, AssociationRuleResult> rulesMap,List<Set<Integer>> nullSets, Set<Integer> threadSafeSet) {
        this.queryId = queryId;
        this.sets = sets;
        this.attributesMap = attributesMap;
        this.rulesMap = rulesMap;
        this.havaVisited = threadSafeSet;
        this.nullSets = nullSets;
    }

    public QueryTask(int queryId, List<Set<Integer>> sets, HashMap<String, ItemAttributeValueAndConfidenceAndPriority> attributesMap, Map<Integer, AssociationRuleResult> rulesMap,List<Set<Integer>> nullSets) {
        this.queryId = queryId;
        this.sets = sets;
        this.attributesMap = attributesMap;
        this.rulesMap = rulesMap;
        this.havaVisited = null;
        this.nullSets = nullSets;
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
            }else if(!nullSets.get(i).contains(queryId)){
                return null;//TODO 检查合理性
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
