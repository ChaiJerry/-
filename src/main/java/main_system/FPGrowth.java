package main_system;

import java.io.*;
import java.util.*;
import java.util.logging.*;

import io.*;
import org.apache.spark.ml.fpm.FPGrowthModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.*;

import static data_processer.DataConverter.*;

public class FPGrowth {
    private static SparkSession spark = null;
    // 最小置信度
    private static float minConfidence;
    // 最小支持度
    private static float minSupport;
    //csv文件结果输出目录路径，若是不以csv文件的格式输出，则该属性可以为null
    private static String resultDirPath;
    // 机票订单csv文件路径
    private static String pathT;
    // 酒店订单csv文件路径
    private static String pathH;
    // 餐饮订单csv文件路径
    private static String pathM;
    // 保险订单csv文件路径
    private static String pathB;
    // 保险订单csv文件路径
    private static String pathI;
    // 选座订单csv文件路径
    private static String pathS;
    // 模式(若是debug模式，则会输出一部分频繁项集和关联规则，否则不输出)
    private static String mode;
    // 结果输出格式（可以是csv或者是db），csv则会将结果以csv文件的形式输出，db则会将结果存入数据库中
    private static String resultForm;
    // 训练备注
    private static String comment;
    private static Logger logger = null;
    //spark是否已经初始化
    private static boolean isSparkInitialed =false;
    // csv文件输入输出工具类
    private static CSVFileIO fileIO = null;

    private FPGrowth() {
    }

    public static void initializeSpark() {
        // 判断是否已经初始化
        if(isSparkInitialed){
            return;
        }
        // 创建SparkSession对象
        spark = SparkSession
                .builder()
                .appName("Civil-aviation-recommended-subject")
                //设置本地运行以及线程数量最大值（local含义为本地运行，*表示线程数量尽可能多）
                .master("local[*]")
                .getOrCreate();

        // 创建日志对象
        logger = Logger.getLogger(FPGrowth.class.getName());

        //读取配置文件
        readProperties();

        isSparkInitialed = true;
    }

    private static void readProperties() {
        // 创建Properties对象
        Properties properties = new Properties();
        // 读取配置文件
        try {
            InputStream stream = MongoUtils.class.getClassLoader().getResourceAsStream("System.properties");
            properties.load(stream);
        } catch (IOException e) {
            logger.info("加载配置文件失败");
        }
        // 获取配置文件中的属性
        // 获取csv文件结果输出目录，若是不以csv文件的格式输出，则该属性可以为null
        resultDirPath = properties.getProperty("resultDirPath");
        // 获取机票订单csv文件路径
        pathT = properties.getProperty("ticketFilePath");
        // 获取酒店订单csv文件路径
        pathH = properties.getProperty("hotelFilePath");
        // 获取餐饮订单csv文件路径
        pathM = properties.getProperty("mealFilePath");
        // 获取保险订单csv文件路径
        pathB = properties.getProperty("baggageFilePath");
        // 获取保险订单csv文件路径
        pathI = properties.getProperty("insuranceFilePath");
        // 获取选座订单csv文件路径
        pathS = properties.getProperty("seatFilePath");
        // 获取模式(若是debug模式，则会输出一部分频繁项集和关联规则，否则不输出)
        mode = properties.getProperty("mode");
        // 获取结果输出格式
        resultForm = properties.getProperty("resultForm");
        // 获取最小置信度
        minConfidence = properties.getProperty("minConfidence") != null ? Float.parseFloat(properties.getProperty("minConfidence")) : 0;
        // 获取最小支持度
        minSupport = properties.getProperty("minSupport") != null ? Float.parseFloat(properties.getProperty("minSupport")) : 0;
        // 获取训练备注
        comment = properties.getProperty("comment");
    }

    /**
     * 训练FPGrowth模型
     * @param itemsDF 输入机票和商品属性订单共现数据
     * @return 返回训练好的FPGrowth模型
     */
    public static FPGrowthModel train(Dataset<Row> itemsDF) {
        logger.info("正在使用FPGrowth算法训练模型");
        return new org.apache.spark.ml.fpm.FPGrowth()
                .setItemsCol("items")//设置items列名
                .setMinSupport(minSupport)//最小支持度
                .setMinConfidence(minConfidence)//最小置信度
                .fit(itemsDF);//让模型适应输入数据
    }

    /**
     * 由输入的机票和商品属性订单共现数据训练，可选择是否输出频繁项集和关联规则
     * @param sourceData 输入机票和商品属性订单共现数据
     * @param outPutFrequentItems 是否输出频繁项集
     * @param outPutRules   是否输出关联规则
     * @param frqItemSetsList   频繁项集List
     * @param rulesList 关联规则List
     */
    public static void singleTypeMining(Dataset<Row> sourceData,boolean outPutFrequentItems,boolean outPutRules
            ,List<List<String>> frqItemSetsList,List<List<String>> rulesList){
        // 启动SparkSession
        initializeSpark();
        //从sourceData中训练模型
        FPGrowthModel model = train(sourceData);
        // 从两个boolean参数中判断是否挖掘频繁项集和关联规则并输出
        if(outPutFrequentItems){
            // 得到频繁项集
            Dataset<Row> freqItemSets = model.freqItemsets();
            //将频繁项集转换为List<List<String>>
            dataset2FRList(freqItemSets,frqItemSetsList);
        }
        if(outPutRules){
            // 得到关联规则
            Dataset<Row> rules = model.associationRules();
            //将关联规则转换为List<List<String>>
            dataset2RulesList(rules,rulesList);
        }
    }

    public static void initializeFileIO() throws IOException {
        if(fileIO!=null){
            return;
        }
        // 创建CSVFileIO对象
        fileIO = new CSVFileIO(resultDirPath, pathT, pathH, pathM, pathB, pathI, pathS);
    }

    public static void fpGrowthTest() throws IOException{
        // 启动SparkSession
        initializeSpark();
        // 创建数据转换器对象
        initializeFileIO();
        for (int i = 1; i < 6; i++) {

            // 准备数据
            logger.info("正在准备数据");
            Dataset<Row> itemsDF = fileIO.singelTypeCsv2dataset(i);

            // 使用FPGrowth算法训练模型
            FPGrowthModel model = train(itemsDF);

            // 得到频繁项集
            Dataset<Row> freqItemSets = model.freqItemsets();

            // 可以选择显示频繁项集(freqItemSets.show();)
            if (mode.equals("debug")) {
                logger.info("显示频繁项集");
                freqItemSets.show();
            }



            //保存频繁项集到csv
            if (resultForm.equals("csv")) {
                fileIO.freItemSet2CSV(freqItemSets, i);
            }else if (resultForm.equals("db")) {
                MongoUtils.frequentItemSets2db(freqItemSets, i);
            }

            // 显示生成的关联规则并保存到csv
            Dataset<Row> rules = model.associationRules();
            if (mode.equals("debug")) {
                logger.info("显示关联规则");
                rules.show();
            }

            if (resultForm.equals("csv")) {
                //保存关联规则到csv
                fileIO.rules2CSV(rules, i);
            } else if (resultForm.equals("db")) {
                //保存关联规则到数据库
                MongoUtils.rules2db(rules, i);
            }
        }

        // 停止SparkSession
        logger.info("SparkSession停止");
        //停止MongoDB
        MongoUtils.closeMongoClient(fileIO.getOrderNumber(),comment,minSupport);
        spark.stop();
    }

    /**
     * 定义数据模式
     * @return 返回定义好的特定数据模式
     */
    public static StructType getSchema() {
        // 定义数据模式
        logger.info("正在定义数据模式");
        return new StructType(new StructField[]{new StructField(
                "items",
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
