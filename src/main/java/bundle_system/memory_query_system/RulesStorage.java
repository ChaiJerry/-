package bundle_system.memory_query_system;

import bundle_system.io.*;
import bundle_system.memory_query_system.lru_pool.*;
import xml_parser.*;

import java.util.*;
import java.util.concurrent.*;

import static bundle_system.io.SharedAttributes.*;

public class RulesStorage {
    //规则编号-规则map
    private Map<Integer, AssociationRuleResult> rulesMap = new HashMap<>();
    //通过机票属性名查找【属性值-规则编号集合】map的map
    //外层map的key为属性名,value为该属性名对应的【属性值-规则编号集合】map
    //内层map的key为属性值，value为该属性值对应的规则编号集合
    private Map<String, Map<String, Set<Integer>>> atttributeMap = new HashMap<>();
    private int ruleCount = 0;
    // 创建一个固定大小的线程池
    private final ExecutorService executorService = Executors.newFixedThreadPool(16);
    private final RandomLRUPool lruPool = new RandomLRUPool(40);

    int type;

    /**
     * 构造函数
     *
     * @param type 商品品类
     */
    public RulesStorage(int type) {
        this.type = type;
    }

    /**
     * 添加规则的方法
     *
     * @param ates 前件属性列表，格式为[Ticket:属性名:属性值]
     * @param rule 关联规则的后件以及confidence值
     */
    public void addRule(String[] ates, AssociationRuleResult rule) {
        rulesMap.put(ruleCount, rule);
        Map<String, String> ticketAttributesTemplate = getTicketAttributesTemplate();
        for (String at : ates) {
            String[] att = at.split(":");
            //att[0]为字符串Ticket，att[1]为属性名,att[2]为属性值
            String attributeName = att[1];
            String attributeValue = att[2];
            ticketAttributesTemplate.put(attributeName, attributeValue);
        }
        for (Map.Entry<String, String> entry : ticketAttributesTemplate.entrySet()) {
            String attributeName = entry.getKey();
            String attributeValue = entry.getValue();
            if (!atttributeMap.containsKey(attributeName)) {
                //如果该属性名不存在，则新建map
                atttributeMap.put(attributeName, new HashMap<>());
                //放入属性值集合
                atttributeMap.get(attributeName).put(attributeValue, new HashSet<>());
                atttributeMap.get(attributeName).get(attributeValue).add(ruleCount);
            } else {
                //如果该属性名存在，则直接放入属性值集合
                if (!atttributeMap.get(attributeName).containsKey(attributeValue)) {
                    //如果该属性值不存在，则新建集合
                    atttributeMap.get(attributeName).put(attributeValue, new HashSet<>());
                    //放入规则编号
                    atttributeMap.get(attributeName).get(attributeValue).add(ruleCount);
                } else {
                    //如果该属性值存在，则直接放入规则编号
                    atttributeMap.get(attributeName).get(attributeValue).add(ruleCount);
                }
            }
        }
        ruleCount++;
    }

    /**
     * 根据机票属性查询规则后件的方法
     *
     * @param ateAttributes 机票属性列表，格式为[属性名:属性值]
     */
    public Map<String, String> queryItemAttributes(List<String> ateAttributes) {
        //用于储存查到的对应规则编号集合的列表
        List<Set<Integer>> ruleIdSets = new ArrayList<>();
        //用于储存查到的所有规则编号的集合
        Set<Integer> AllRuleIds = new HashSet<>();
        Set<Integer> threadSafeSet = Collections.synchronizedSet(new HashSet<>());
        for (String ateAttribute : ateAttributes) {
            //att[0]为属性名,att[1]为属性值
            String[] att = ateAttribute.split(":");
            //得到属性名以及属性值对应的规则集合，如果属性名不存在，则返回空集合
            Set<Integer> integers = atttributeMap.get(att[0]).getOrDefault(att[1], new HashSet<>());
            ruleIdSets.add(integers);
            AllRuleIds.addAll(integers);
        }
        // map中前面是属性名，后面是属性值和置信度
        HashMap<String, ItemAttributeValueAndConfidenceAndPriority> attributeNameVCPMap = getAttributesMap(type);
        // 提交查询任务到线程池
        List<Future<?>> futures = new ArrayList<>();
        for (int ruleId : AllRuleIds) {
            futures.add(executorService.submit(new QueryTask(ruleId, ruleIdSets, attributeNameVCPMap, rulesMap)));
        }
//        for(Set<Integer> ruleIds : ruleIdSets){
//            for(int ruleId:ruleIds) {
//                futures.add(executorService.submit(new QueryTask(ruleId, ruleIdSets, attributeNameVCPMap, rulesMap, threadSafeSet)));
//            }
//        }

        //在lruPool中查询存储的机票属性对应的商品属性
        Map<String, String> attrInLRUPool = lruPool.tryGet(ateAttributes);
        if (attrInLRUPool != null) {
            return attrInLRUPool;
        }

        // 创建属性名-属性值查询结果
        HashMap<String, String> result = new HashMap<>();
        // 等待并获取查询结果
        for (Future<?> future : futures) {
            try {
                future.get(); // 这会阻塞直到结果可用
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        for (String attributeName : attributeNameVCPMap.keySet()) {
            result.put(attributeName, attributeNameVCPMap.get(attributeName).getAttributeValue());
        }
        lruPool.add(ateAttributes, result);
        threadSafeSet = null;
        return result;
    }

    public void shutdown() {
        executorService.shutdownNow();
    }

    public void queryItemAttributes(BundleItem ticketInfo) {
        Map<String, String> attributes = ticketInfo.getAttributes();

    }
}
