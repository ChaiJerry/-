package io;

import java.io.*;
import java.util.*;
import java.util.logging.*;

public class IOMonitor {
    private IOMonitor() {
    }
    //下面用于定义商品品类对应的编号，其实可以换成枚举类
    public static final int TICKET = 0;
    public static final int HOTEL = 1;
    public static final int MEAL = 2;
    public static final int BAGGAGE = 3;
    public static final int INSURANCE = 4;
    public static final int SEAT = 5;
    //缩写的商品品类，与下面的商品品类全称对应
    protected static final String[] types = {"T", "H", "M", "B", "I", "S"};
    protected static final String[] FULL_NAMES = {"Ticket", "Hotel", "Meal", "Baggage", "Insurance","Seat"};
    //用于将订单号与不同品类的商品属性对应，若是内存不足可以考虑将TicketMap改为局部变量
    protected static Map<String, List<String>> ticketMap;
    //将type与index对应，用于快速查找
    protected static HashMap<String, Integer> type2index = new HashMap<>();

    public static HeaderStorage[] getHeaderStorage() {
        return headerStorage;
    }

    //存储每个品类下的商品属性，用于快速查找，主要从CSV文件的头文件读取
    protected static HeaderStorage[] headerStorage = new HeaderStorage[6];

    // 最小置信度
    public static final float MIN_CONFIDENCE;
    // 最小支持度
    public static final float MIN_SUPPORT;
    //csv文件结果输出目录路径，若是不以csv文件的格式输出，则该属性可以为null
    public static final String RESULT_DIR_PATH;
    // 机票订单csv文件路径
    public static final String PATH_T;
    // 酒店订单csv文件路径
    public static final String PATH_H;
    // 餐饮订单csv文件路径
    public static final String PATH_M;
    // 保险订单csv文件路径
    public static final String PATH_B;
    // 保险订单csv文件路径
    public static final String PATH_I;
    // 选座订单csv文件路径
    public static final String PATH_S;
    // 模式(若是debug模式，则会输出一部分频繁项集和关联规则，否则不输出)
    public static final String MODE;
    // 结果输出格式（可以是csv或者是db），csv则会将结果以csv文件的形式输出，db则会将结果存入数据库中
    public static final String RESULT_FORM;
    // 训练备注
    public static final String COMMENT;

    public static final String ORDERS_FIELD_NAME = "Orders";


    protected static final String[] TARGET_ITEM_NAMES = {null,"HOTEL_NAME", "MEAL_NAME"
            , "BAGGAGE_SPECIFICATION", "INSUR_PRO_NAME", "SEAT_NO"};

    static {
        // 创建Properties对象
        Properties properties = new Properties();
        // 读取配置文件
        try {
            InputStream stream = MongoUtils.class.getClassLoader().getResourceAsStream("System.properties");
            properties.load(stream);
        } catch (IOException e) {
            Logger logger = Logger.getLogger(IOMonitor.class.getName());
            logger.info("加载配置文件失败");
        }
        // 获取配置文件中的属性
        // 获取csv文件结果输出目录，若是不以csv文件的格式输出，则该属性可以为null
        RESULT_DIR_PATH = properties.getProperty("resultDirPath");
        // 获取机票订单csv文件路径
        PATH_T = properties.getProperty("ticketFilePath");
        // 获取酒店订单csv文件路径
        PATH_H = properties.getProperty("hotelFilePath");
        // 获取餐饮订单csv文件路径
        PATH_M = properties.getProperty("mealFilePath");
        // 获取保险订单csv文件路径
        PATH_B = properties.getProperty("baggageFilePath");
        // 获取保险订单csv文件路径
        PATH_I = properties.getProperty("insuranceFilePath");
        // 获取选座订单csv文件路径
        PATH_S = properties.getProperty("seatFilePath");
        // 获取模式(若是debug模式，则会输出一部分频繁项集和关联规则，否则不输出)
        MODE = properties.getProperty("mode");
        // 获取结果输出格式
        RESULT_FORM = properties.getProperty("resultForm");
        // 获取最小置信度
        MIN_CONFIDENCE = properties.getProperty("minConfidence") != null ? Float.parseFloat(properties.getProperty("minConfidence")) : 0;
        // 获取最小支持度
        MIN_SUPPORT = properties.getProperty("minSupport") != null ? Float.parseFloat(properties.getProperty("minSupport")) : 0;
        // 获取训练备注
        COMMENT = properties.getProperty("comment");

    }

    public static String[] getTargetItemNames() {
        return TARGET_ITEM_NAMES;
    }
}
