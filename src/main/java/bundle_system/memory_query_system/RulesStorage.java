package bundle_system.memory_query_system;

import bundle_system.io.*;

import java.util.*;

import static bundle_system.io.SharedAttributes.*;

/**
 * 规则存储系统，用于存储和查询关联规则。
 * 需要注意的是这里由于初始化后都是读取操作因此并没有控制线程安全问题
 * 之后若是希望拓展增加写入操作则需要考虑线程安全。
 * @author lyt
 */
public class RulesStorage {
    //规则编号-规则map
    private final Map<Integer, AssociationRuleConsResult> rulesMap = new HashMap<>();
    //通过机票属性名查找【属性值-规则编号集合】map的map
    //外层map的key为属性名,value为该属性名对应的【属性值-规则编号集合】map
    //内层map的key为属性值，value为该属性值对应的规则编号集合
    private final Map<String, Map<String, Set<Integer>>> atttributeMap = new HashMap<>();
    //规则数量
    private int ruleCount = 0;
    //品类编码
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
     * 输出一个动态的进度条的方法
     * @param percent      当前的进度百分比（0 到 100）
     * @param progressBarName 进度条的名字
     */
    public static void printProgressBar(int percent, String progressBarName) {
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException("Percent must be between 0 and 100");
        }
        // 进度条的总长度
        int barLength = 50;
        // 计算已完成的部分
        int completed = (percent * barLength / 100);
        // 构建进度条
        StringBuilder progressBar = new StringBuilder(progressBarName + "\t [");
        for (int i = 0; i < barLength; i++) {
            if (i < completed) {
                progressBar.append("\u001B[32m=\u001B[0m"); // 绿色的 "="
            } else if (i == completed) {
                progressBar.append(">");
            } else {
                progressBar.append(" ");
            }
        }
        progressBar.append("] ").append(String.format("%3d%%", percent));
        // 输出进度条
        System.out.print("\r" + progressBar.toString());
        System.out.flush(); // 确保立即输出
    }

    /**
     * 向存储系统中添加规则的方法
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
            //遍历机票属性键值对
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
     * 初始化参数type对应的规则存储对象的方法
     * @param type 商品品类代码
     * @param itemTicketRules 商品规则列表
     * @return RulesStorage对象
     */
    public static RulesStorage initRulesStorageByType(int type, List<List<String>> itemTicketRules){
        //加载阶段
        //初始化进度条信息
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
     * 根据机票属性查询规则后件的方法（用于测试）
     *  会预处理机票属性，将机票属性列表转换为机票属性键值对
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
            //放入机票属性键值对
            ateAttributesTemplate.put(attributeName, attributeValue);
        }
        return queryBestRules(ateAttributesTemplate);
    }

    /**
     * 根据机票属性查询最匹配的所有关联规则的方法
     * 这些关联规则的后件是商品属性键值对，当规则充分时会填充商品的所有属性
     * @param ateAttributes 作为前件的机票属性键值对
     * @return 最匹配的规则后件的集合，商品属性键值对，其中键是属性名，值是属性值和置信度以及优先度
     */
    public Map<String, AttrValueConfidencePriority> queryBestRules(Map<String, String> ateAttributes) {
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
            // 得到属性名对应的规则编号集合
            Map<String, Set<Integer>> attriValueRuleIdSetsMap = atttributeMap.get(attributeName);
            if (attriValueRuleIdSetsMap == null) {
                attriValueRuleIdSetsMap = new HashMap<>();
                atttributeMap.put(attributeName, attriValueRuleIdSetsMap);
            }
            Set<Integer> ruleIds = attriValueRuleIdSetsMap.getOrDefault(attributeValue, new HashSet<>());
            // 当确定好顺序后，关联规则前件的属性属性值为null的id集合(nullRuleIdSets)按理来说可以直接得到
            // 但是为了保险起见还是每次都得到一个新的
            nullRuleIdSets.add(attriValueRuleIdSetsMap.getOrDefault(null, new HashSet<>()));
            // 将规则编号集合放入列表中，列表中对应着不同属性对应的规则编号集合
            ruleIdSets.add(ruleIds);
            allRuleIds.addAll(ruleIds);
        }
        // map中键是属性名，值是属性值和置信度以及优先度
        // 这里是在得到一个商品属性模板
        Map<String, AttrValueConfidencePriority> attributeNameVCPMap = getAttributesMap(type);
        // 遍历所有可能的规则编号集合
        for (int ruleId : allRuleIds) {
            int queryValue = 0;// 满足查询条件的数量
            boolean qualified = true;
            // 遍历该规则所有属性值
            for (int i = 0; i < ruleIdSets.size(); i++) {
                if (ruleIdSets.get(i).contains(ruleId)) {
                    // 如果某个属性值满足，就说明这个前件符合查询条件
                    queryValue++;
                }else if(!nullRuleIdSets.get(i).contains(ruleId)){
                    // 如果某个属性值既不满足也不为null，就说明这个前件不符合查询条件，直接跳过这个规则
                    qualified = false;
                    break;
                }
            }
            // 如果某个规则的前件不符合查询条件，则直接跳过该规则
            if (!qualified) {
                continue;
            }
            // 到这里的规则满足条件，则尝试将该规则的后件加入到结果中
            AssociationRuleConsResult associationRuleConsResult = rulesMap.get(ruleId);
            // 得到置信度
            double confidence = associationRuleConsResult.getConfidence();
            // 得到后件属性名
            String attributeName = associationRuleConsResult.getAttributeName();
            // 得到后件属性值
            String attributeValue = associationRuleConsResult.getAttributeValue();
            // 尝试将该规则的后件加入到结果中
            attributeNameVCPMap.get(attributeName).tryAssign(attributeValue, queryValue, confidence);
        }
        return attributeNameVCPMap;
    }

    /**
     * 获取规则数量
     * @return 规则数量
     */
    public int getSize(){
        //从规则映射中获取规则数量
        return rulesMap.size();
    }

}
