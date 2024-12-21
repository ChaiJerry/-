package bundle_system.memory_query_system;

import bundle_system.io.*;


import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import static bundle_system.io.SharedAttributes.*;
import static bundle_system.memory_query_system.QuickQuery.*;

public class RulesStorage {
    //规则编号-规则map
    private Map<Integer, AssociationRuleConsResult> rulesMap = new HashMap<>();
    //通过机票属性名查找【属性值-规则编号集合】map的map
    //外层map的key为属性名,value为该属性名对应的【属性值-规则编号集合】map
    //内层map的key为属性值，value为该属性值对应的规则编号集合
    private Map<String, Map<String, Set<Integer>>> atttributeMap = new HashMap<>();
    private int ruleCount = 0;
    // 创建一个固定大小的线程池，现在暂时为1，主要是为了之后的拓展可以调整
    private final ExecutorService executorService = Executors.newFixedThreadPool(1);
    //private final RandomLRUPool lruPool = new RandomLRUPool(40);

    int type;
    private static final List<RulesStorage> allRulesStorages = new ArrayList<>();
    /**
     * 构造函数
     *
     * @param type 商品品类
     */
    public RulesStorage(int type) {
        this.type = type;
        allRulesStorages.add(this);
    }

    /**
     * 添加规则的方法
     *
     * @param ates 前件属性列表，格式为[Ticket:属性名:属性值]
     * @param rule 关联规则的后件以及confidence值
     */
    public void addRule(String[] ates, AssociationRuleConsResult rule) {
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
     * 初始化所有规则存储对象的方法
     * @param type 商品品类代码
     * @param itemTicketRules 商品规则列表
     * @return RulesStorage对象
     */
    public static RulesStorage initRulesStorageByType(int type,List<List<String>> itemTicketRules){
        //训练阶段
        String info = "正在初始化"+SharedAttributes.getFullNames()[type]+"知识库";
        printProgressBar(0, info);
        RulesStorage rulesStorage = new RulesStorage(type);
        for (List<String> itemTicketRule : itemTicketRules) {
            String[] split = itemTicketRule.get(0).split("; ");
            String consequent = itemTicketRule.get(1).split("; ")[0];
            double confidence = Double.parseDouble(itemTicketRule.get(2).split("::")[1]);
            rulesStorage.addRule(split, new AssociationRuleConsResult(consequent, confidence));
        }
        printProgressBar(100, info);
        System.out.println();
        return rulesStorage;
    }

    /**
     * 根据机票属性查询规则后件的方法
     *
     * @param ateAttributes 机票属性列表，格式为[属性名:属性值]
     */
    public Map<String, AttrValueConfidencePriority> queryItemAttributesAndConfidence(List<String> ateAttributes) {
        //将机票属性列表转换为机票属性键值对
        Map<String, String> ateAttributesTemplate = new HashMap<>();
        for (String at : ateAttributes) {
            String[] att = at.split(":");
            //att[0]为属性名,att[1]为属性值
            String attributeName = att[0];
            String attributeValue = att[1];
            ateAttributesTemplate.put(attributeName, attributeValue);
        }
        return queryItemAttributes(ateAttributesTemplate);
    }

    public void shutdown() {
        executorService.shutdownNow();
    }

    public static void shutdownAll() {
        for (RulesStorage rulesStorage : allRulesStorages) {
            rulesStorage.shutdown();
        }
    }

    public Map<String, AttrValueConfidencePriority> queryItemAttributes(Map<String, String> ateAttributes) {
        //用于储存查到的对应规则编号集合的列表
        List<Set<Integer>> ruleIdSets = new ArrayList<>();
        //用于储存属性值为null的对应规则编号集合的列表
        List<Set<Integer>> nullRuleIdSets = new ArrayList<>();
        //用于储存查到的所有规则编号的集合
        Set<Integer> allRuleIds = new HashSet<>();
        for (Map.Entry<String,String> ateAttribute : ateAttributes.entrySet()) {
            //得到属性名以及属性值对应的规则集合，如果属性名不存在，则返回空集合
            String attributeName = ateAttribute.getKey();
            String attributeValue = ateAttribute.getValue();
            Map<String, Set<Integer>> attriValueRuleIdSetsMap = atttributeMap.get(attributeName);
            if (attriValueRuleIdSetsMap == null) {
                attriValueRuleIdSetsMap = new HashMap<>();
                atttributeMap.put(attributeName, attriValueRuleIdSetsMap);
            }
            Set<Integer> ruleIds = attriValueRuleIdSetsMap.getOrDefault(attributeValue, new HashSet<>());
            // 当确定好顺序后，关联规则前件的属性属性值为null的id集合(nullRuleIdSets)按理来说可以直接得到
            // 但是为了保险起见还是每次都得到一个新的
            nullRuleIdSets.add(attriValueRuleIdSetsMap.getOrDefault(null, new HashSet<>()));
            ruleIdSets.add(ruleIds);
            allRuleIds.addAll(ruleIds);
        }
        // map中前面是属性名，后面是属性值和置信度
        Map<String, AttrValueConfidencePriority> attributeNameVCPMap = getAttributesMap(type);
        for (int ruleId : allRuleIds) {
            int queryValue = 0;// 满足查询条件的数量
            boolean qualified = true;
            for (int i = 0; i < ruleIdSets.size(); i++) {
                if (ruleIdSets.get(i).contains(ruleId)) {
                    queryValue++;
                }else if(!nullRuleIdSets.get(i).contains(ruleId)){
                    qualified = false;
                    break;
                }
            }
            if (!qualified) {
                continue;
            }
            AssociationRuleConsResult associationRuleConsResult = rulesMap.get(ruleId);
            double confidence = associationRuleConsResult.getConfidence();
            String AttributeName = associationRuleConsResult.getAttributeName();
            String AttributeValue = associationRuleConsResult.getAttributeValue();
            attributeNameVCPMap.get(AttributeName).tryAssign(AttributeValue, queryValue, confidence);
        }
        return attributeNameVCPMap;
    }

    /**
     * 根据机票属性查询规则后件的方法
     *
     * @param ateAttributes 机票的属性键值对（作为关联规则前件，key为属性名，value为属性值）
     * @return 商品属性键值对
     */
    public Map<String, AttrValueConfidencePriority> queryItemAttributesAndConfidence(Map<String, String> ateAttributes) {
        //用于储存查到的对应规则编号集合的列表
        List<Set<Integer>> ruleIdSets = new ArrayList<>();
        //用于储存属性值为null的对应规则编号集合的列表
        List<Set<Integer>> nullRuleIdSets = new ArrayList<>();
        List<String> ateAttributesValeList = new ArrayList<>();//用于作为lruPool的key
        //用于储存查到的所有规则编号的集合
        Set<Integer> AllRuleIds = new HashSet<>();
        for (Map.Entry<String,String> ateAttribute : ateAttributes.entrySet()) {
            //得到属性名以及属性值对应的规则集合，如果属性名不存在，则返回空集合
            String attributeName = ateAttribute.getKey();
            String attributeValue = ateAttribute.getValue();
            ateAttributesValeList.add(attributeValue);
            Set<Integer> integers = atttributeMap.get(attributeName).getOrDefault(attributeValue, new HashSet<>());
            // 当确定好顺序后，关联规则前件的属性属性值为null的id集合(nullRuleIdSets)按理来说可以直接得到
            // 但是为了保险起见还是每次都得到一个新的
            nullRuleIdSets.add(atttributeMap.get(attributeName).getOrDefault(null, new HashSet<>()));
            ruleIdSets.add(integers);
            AllRuleIds.addAll(integers);
        }
        // map中前面是属性名，后面是属性值和置信度
        Map<String, AttrValueConfidencePriority> attributeNameVCPMap = getAttributesMap(type);
        // 提交查询任务到线程池
        List<Future<?>> futures = new ArrayList<>();
        for (int ruleId : AllRuleIds) {
            futures.add(executorService.submit(new QueryTask(ruleId, ruleIdSets, attributeNameVCPMap, rulesMap,nullRuleIdSets)));
        }

//        //在lruPool中查询存储的机票属性对应的商品属性
//        Map<String, AttrValueConfidencePriority> attrInLRUPool = lruPool.tryGet(ateAttributesValeList);
//        if (attrInLRUPool != null) {
//            //System.out.println("lru命中，查询到缓存");
//            return attrInLRUPool;
//        }

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
//        lruPool.add(ateAttributesValeList, attributeNameVCPMap);
        return attributeNameVCPMap;
    }
}
