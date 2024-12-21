package bundle_system.data_generating_system;

import java.io.*;
import java.util.*;
//import java.util.logging.*;

import org.apache.spark.ml.fpm.FPGrowthModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.*;
import bundle_system.io.*;

import static bundle_system.data_processer.DataConverter.*;
import static bundle_system.io.SharedAttributes.*;

public class FPGrowth {
    private static final SparkSession spark ;
    public static final String ITEMS = "items";
    public static final String DEBUG = "debug";

    static {
        logger.info("正在初始化SparkSession");
        spark = SparkSession // 创建SparkSession对象
                .builder()
                .appName("Civil-aviation-recommended-subject")
                //设置本地运行以及线程数量最大值（local含义为本地运行，*表示线程数量尽可能多）
                .master("local[*]")
                .getOrCreate();
        logger.info("SparkSession初始化完成");
    }


    private FPGrowth() {
    }

    /**
     * 训练FPGrowth模型
     *
     * @param itemsDF 输入机票和商品属性订单共现数据
     * @return 返回训练好的FPGrowth模型
     */
    public static FPGrowthModel train(Dataset<Row> itemsDF) {
        logger.info("正在使用FPGrowth算法训练模型");
        return new org.apache.spark.ml.fpm.FPGrowth()
                .setItemsCol(ITEMS)//设置items列名
                .setMinSupport(MIN_SUPPORT)//最小支持度
                .setMinConfidence(MIN_CONFIDENCE)//最小置信度
                .fit(itemsDF);//让模型适应输入数据
    }

    /**
     * 训练FPGrowth模型
     *
     * @param itemsDF       输入机票和商品属性订单共现数据
     * @param minSupport    最小支持度
     * @param minConfidence 最小置信度
     * @return 返回训练好的FPGrowth模型
     */
    public static FPGrowthModel train(Dataset<Row> itemsDF, double minSupport, double minConfidence) {
        logger.info("正在使用FPGrowth算法训练模型");
        return new org.apache.spark.ml.fpm.FPGrowth()
                .setItemsCol(ITEMS)//设置items列名
                .setMinSupport(minSupport)//最小支持度
                .setMinConfidence(minConfidence)//最小置信度
                .fit(itemsDF);//让模型适应输入数据
    }

    /**
     * 由输入的机票和商品属性订单共现数据训练，可选择是否输出频繁项集和关联规则
     *
     * @param itemTicketAttributes 输入机票和商品属性订单共现数据
     * @param outPutFrequentItems  是否输出频繁项集
     * @param outPutRules          是否输出关联规则
     * @param frqItemSetsList      频繁项集List
     * @param rulesList            关联规则List
     * @param minSupport           最小支持度
     * @param minConfidence        最小置信度
     */
    public static void singleTypeMining(List<List<String>> itemTicketAttributes
            , boolean outPutFrequentItems, boolean outPutRules
            , List<List<String>> frqItemSetsList, List<List<String>> rulesList
            , double minSupport, double minConfidence) {
        System.out.println("启动单品类数据挖掘，输入原数据非空="+(!itemTicketAttributes.isEmpty()));
        // 创建数据集
        System.out.println("正在将原数据格式转换为df格式的数据集");
        Dataset<Row> sourceData = listOfAttributeList2Dataset(itemTicketAttributes);
        //从sourceData中训练模型
        System.out.println("开始训练模型");
        FPGrowthModel model = train(sourceData, minSupport, minConfidence);
        // 从两个boolean参数中判断是否挖掘频繁项集和关联规则并输出
        if (outPutFrequentItems) {
            // 得到频繁项集
            Dataset<Row> freqItemSets = model.freqItemsets();
            //将频繁项集转换为List<List<String>>
            dataset2FRList(freqItemSets, frqItemSetsList);
        }
        if (outPutRules) {
            System.out.println("开始挖掘关联规则");
            //  得到关联规则
            Dataset<Row> rules = model.associationRules();
            //  将关联规则转换为List<List<String>>
            dataset2RulesList(rules, rulesList);
        }
    }


    public static void fpGrowthTest() throws IOException {

        for (int i = 1; i < 6; i++) {
            //得到运行时间
            long startTime = System.currentTimeMillis();
            // 准备数据
//            logger.info("正在准备数据");
            Dataset<Row> itemsDF = fileIO.singleTypeCsv2dataset(i);

            // 使用FPGrowth算法训练模型
            FPGrowthModel model = train(itemsDF);

            // 得到频繁项集
            Dataset<Row> freqItemSets = model.freqItemsets();

            // 可以选择显示频繁项集(freqItemSets.show();)
            if (MODE.equals(DEBUG)) {
//                logger.info("显示频繁项集");
                //freqItemSets.show();
            }

            //保存频繁项集到csv
            if (RESULT_FORM.equals("csv")) {
                fileIO.freItemSet2CSV(freqItemSets, i);
            } else if (RESULT_FORM.equals("db")) {
                MongoUtils.frequentItemSets2db(freqItemSets, i);
            }

            // 显示生成的关联规则并保存到csv
            Dataset<Row> rules = model.associationRules();
            if (MODE.equals(DEBUG)) {
//                logger.info("显示关联规则");
                //rules.show();
            }

            if (RESULT_FORM.equals("csv")) {
                //保存关联规则到csv
                fileIO.rules2CSV(rules, i);
            } else if (RESULT_FORM.equals("db")) {
                //保存关联规则到数据库
                MongoUtils.rules2db(rules, i);
            }
            long endTime = System.currentTimeMillis();
            System.out.println(getFullNames()[i] + "," + MIN_CONFIDENCE + "," + (endTime - startTime) + "ms");
        }
        //停止MongoDB
        MongoUtils.settle(fileIO.getOrderNumber(), COMMENT, MIN_SUPPORT);
    }


    /**
     * 定义数据模式
     *
     * @return 返回定义好的特定数据模式
     */
    public static StructType getSchema() {
        // 定义数据模式
        logger.info("正在定义数据模式");
        return new StructType(new StructField[]{new StructField(
                ITEMS,
                new ArrayType(DataTypes.StringType, true)
                //该字段名称为items，元素类型为String
                , false,//false表示该字段不能为空
                Metadata.empty())//无附加元数据
        });
    }

    public static Dataset<Row> getDataFrame(List<Row> data) {
        return spark.createDataFrame(data, getSchema());
    }

    public static CSVFileIO getFileIO() {
        // 返回CSVFileIO对象
        return fileIO;
    }

}
