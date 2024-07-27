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
    public static float minConfidence;
    // 最小支持度
    public static float minSupport;
    //csv文件结果输出目录路径，若是不以csv文件的格式输出，则该属性可以为null
    public static String resultDirPath;
    // 机票订单csv文件路径
    public static String pathT;
    // 酒店订单csv文件路径
    public static String pathH;
    // 餐饮订单csv文件路径
    public static String pathM;
    // 保险订单csv文件路径
    public static String pathB;
    // 保险订单csv文件路径
    public static String pathI;
    // 选座订单csv文件路径
    public static String pathS;
    // 模式(若是debug模式，则会输出一部分频繁项集和关联规则，否则不输出)
    public static String mode;
    // 结果输出格式（可以是csv或者是db），csv则会将结果以csv文件的形式输出，db则会将结果存入数据库中
    public static String resultForm;
    // 训练备注
    public static String comment;

    public static final String[] targetItemNames = {null,"HOTEL_NAME", "MEAL_NAME"
            , "BAGGAGE_SPECIFICATION", "INSURANCE_COMPANY", "SEAT_NO"};

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

}
