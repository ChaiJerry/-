package bundle_system.api;

import java.util.*;

import static bundle_system.data_generating_system.FPGrowth.*;

public class API {
    private API() {}

    /**
     * @param itemTicketAttributes List<List<String>>之中每个List<String>对应一个订单中共同出现的机票与商品的属性，每个String对应一个属性（机票或商品皆可，顺序无影响），而List<List<String>>则是共现的集合
     * @param outputFrequentItems 是否输出频繁项集
     * @param outputAssociationRules 是否输出关联规则
     * @param frequentItemSets 频繁项集输出到的List<List<String>>的引用
     * @param associationRules 关联规则输出到的List<List<String>>的引用
     * @param minSupport          最小支持度
     * @param minConfidence       最小置信度
     */
    public static void associationRulesMining(
            List<List<String>> itemTicketAttributes
            ,boolean outputFrequentItems,boolean outputAssociationRules
            ,List<List<String>> frequentItemSets
            ,List<List<String>> associationRules,double minSupport,double minConfidence) {
        //使用时需要使用空的List<List<String>>，传入引用，方便输出结果
        //直接调用FPGrowth的singleTypeMining方法，传入参数，方便使用FPGrowth内部的private方法和属性
        singleTypeMining(itemTicketAttributes,outputFrequentItems,outputAssociationRules
                ,frequentItemSets,associationRules,minSupport,minConfidence);
    }


}
