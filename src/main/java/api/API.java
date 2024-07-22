package api;

import org.apache.spark.sql.*;

import java.util.*;

import static main_system.FPGrowth.*;

public class API {
    private API() {}

    /**
     * @param itemTicketAttributes Dataset<Row>之中每个Row是每个订单中机票与每个品类商品之间属性的共现，而Dataset则是这个共现的集合
     * @param outPutFrequentItems 是否输出频繁项集
     * @param outPutRules 是否输出关联规则
     * @param itemTicketFreqItemSets 频繁项集输出到的List<List<String>>的引用
     * @param itemTicketRules 关联规则输出到的List<List<String>>的引用
     */
    public static void mining(
            Dataset<Row> itemTicketAttributes
            ,boolean outPutFrequentItems,boolean outPutRules
            ,List<List<String>> itemTicketFreqItemSets
            ,List<List<String>> itemTicketRules) {
        //使用时需要使用空的List<List<String>>，传入引用，方便输出结果
        //直接调用FPGrowth的singleTypeMining方法，传入参数，方便使用FPGrowth内部的private方法和属性
        singleTypeMining(itemTicketAttributes,outPutFrequentItems,outPutRules,itemTicketFreqItemSets,itemTicketRules);
    }


}
