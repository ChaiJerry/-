package bundle_system.io;



import bundle_system.memory_query_system.*;

import java.io.*;
import java.util.*;
//import java.util.logging.*;

public class SharedAttributes {

    public static final String NEW_ATT= "PAYMENTAMOUNT";
    public static final String TICKET_ATTRIBUTES_FIELD_NAME = "ticketAttributes";

    private SharedAttributes() {
    }
    //之后若是要添加新的品类，则在此处添加一个名字，并注册到FULL_NAMES中
    protected static final String[] FULL_NAMES = {"Ticket", "Hotel", "Meal", "Baggage", "Insurance", "Seat"};
    //下面用于定义商品品类对应的编号，其实可以换成枚举类
    public static final int TICKET = 0;
    public static final int HOTEL = 1;
    public static final int MEAL = 2;
    public static final int BAGGAGE = 3;
    public static final int INSURANCE = 4;
    public static final int SEAT = 5;
    /////
    //若是之后要添加新的品类，则在此处添加一个值，并注册到FULL_NAMES中
    /////
    //训练订单的品类编号
    public static final int TRAIN_TICKET = 6 ;//值为FULL_NAMES.length + 1
    //测试订单的品类编号
    public static final int TEST_TICKET = 7 ; //值为FULL_NAMES.length + 2
    //缩写的商品品类，与下面的商品品类全称对应,最后一个是为了评估临时增加的

    //用于添加到属性中的品类标识
    //机票标识
    public static final String T_SIGN = "Ticket:";
    //餐食标识
    public static final String M_SIGN = "Meal:";
    //行李标识
    public static final String B_SIGN = "Baggage:";
    //酒店标识
    public static final String H_SIGN = "Hotel:";
    //保险标识
    public static final String I_SIGN = "Insurance:";
    //选座标识
    public static final String S_SIGN = "Seat:";
    //若是之后要添加新的品类，则在注册标识

    //用于存储每个品类下的商品属性，用于快速查找，之后要是添加新的属性或者品类，则在此处添加
    public static final String[] TICKET_ATTRIBUTES = {"MONTH", "FROM", "TO" , "T_GRADE","HAVE_CHILD","PROMOTION_RATE","T_FORMER"};
    public static final String[] HOTEL_ATTRIBUTES = {"AIR_REAL_MONEY", "PRODUCTTYPE", "PRODUCT_NAME", "HOTEL_NAME"};
    public static final String[] MEAL_ATTRIBUTES = {"MEAL_CODE", "PM_PRICE"};
    public static final String[] BAGGAGE_ATTRIBUTES = {"PAYMENTAMOUNT","BAGGAGE_SPECIFICATION"};
    public static final String[] INSURANCE_ATTRIBUTES =  {"INSUR_AMOUNT","INSUR_PRO_NAME", "INSURANCE_COMPANYCODE"};
    public static final String[] SEAT_ATTRIBUTES = {"SEAT_NO"};
    //之后要是添加新的品类，则在此处添加，并注册到itemAttributeNames中
    private final static List<String[]> itemAttributeNames = new ArrayList<>();
    static {
        itemAttributeNames.add(TICKET_ATTRIBUTES);
        itemAttributeNames.add(HOTEL_ATTRIBUTES);
        itemAttributeNames.add(MEAL_ATTRIBUTES);
        itemAttributeNames.add(BAGGAGE_ATTRIBUTES);
        itemAttributeNames.add(INSURANCE_ATTRIBUTES);
        itemAttributeNames.add(SEAT_ATTRIBUTES);
        //用于训练测试的属性，这里和普通机票属性存储一样
        itemAttributeNames.add(TICKET_ATTRIBUTES);
        //之后要是添加新的品类，则在此处添加



    }
    public static Map<String,String> getTicketAttributesTemplate() {
        Map<String, String> attributes = new HashMap<>();
        for (String ticketAttribute : TICKET_ATTRIBUTES) {
            attributes.put(ticketAttribute, null);
        }
        return attributes;
    }


    public static HashMap<String, AttrValueConfidencePriority> getAttributesMap(int type) {
        HashMap<String, AttrValueConfidencePriority> attributesMap = new HashMap<>();
        for(String attributeName : itemAttributeNames.get(type)) {
           attributesMap.put(attributeName, new AttrValueConfidencePriority());
        }
        return attributesMap;
    }



    protected static final int[] attributeNumForEachType = {0,HOTEL_ATTRIBUTES.length,MEAL_ATTRIBUTES.length,BAGGAGE_ATTRIBUTES.length,
            INSURANCE_ATTRIBUTES.length,SEAT_ATTRIBUTES.length};

    public static int[] getAttributeNumForEachType() {
        return attributeNumForEachType;
    }

    protected static Map<String, List<List<String>>> testTicketsMap;

    public static ItemAttributesStorage[] getItemAttributesStorage() {
        return itemAttributesStorage;
    }

    //存储每个品类下的商品属性，用于快速查找，主要从CSV文件的头文件读取
    protected static ItemAttributesStorage[] itemAttributesStorage = new ItemAttributesStorage[9];

    // 最小置信度
    public static final double MIN_CONFIDENCE;
    // 最小支持度
    public static final double MIN_SUPPORT;
    //csv文件结果输出目录路径，若是不以csv文件的格式输出，则该属性可以为null
    public static final String RESULT_DIR_PATH;
    // 默认机票订单csv文件路径（
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
    // 测试订单csv文件路径
    public static final String PATH_TEST_T;
    // 训练订单csv文件路径
    public static final String PATH_TRAIN_T;

    // 模式(若是debug模式，则会输出一部分频繁项集和关联规则，否则不输出)
    public static final String MODE;
    // 结果输出格式（可以是csv或者是db），csv则会将结果以csv文件的形式输出，db则会将结果存入数据库中
    public static final String RESULT_FORM;
    // 训练备注
    public static final String COMMENT;

    public static final String ORDERS_FIELD_NAME = "Orders";

    //数据库中商品唯一标识符的field名
    private static final List<List<String>> targetItemFieldNames;

    //商品唯一标识符的名字
    private static final List<List<String>> targetItemNames;

    public static final String ATTRIBUTES_FIELD = "attributes.";
    public static final String ATTRIBUTES_FIELD_NAME = "attributes";
    public static final String ITEM_ATTRIBUTES_FIELD_NAME = "itemAttributes";
    public static final String TRAINING_NUMBER_FIELD_NAME = "trainingNumber";
    public static final double LITTLE_DOUBLE =10e-7;//一个非常小的数，用于抵消掉浮点数无法精确表示导致的误差

    // 创建CSVFileIO对象
    public static final CSVFileIO fileIO;

    static {
        CSVFileIO tmpFileIO;
        //Logger logger = Logger.getLogger(SharedAttributes.class.getName());
        // 创建Properties对象
        Properties properties = new Properties();
        // 读取配置文件
        try {
            InputStream stream = MongoUtils.class.getClassLoader().getResourceAsStream("System.properties");
            properties.load(stream);
        } catch (IOException e) {
            //logger.info("加载配置文件失败");
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
        // 获取机票测试订单csv文件路径
        PATH_TEST_T = properties.getProperty("testTicketFilePath");
        // 获取机票训练订单csv文件路径
        PATH_TRAIN_T = properties.getProperty("trainTicketFilePath");
        // 获取模式(若是debug模式，则会输出一部分频繁项集和关联规则，否则不输出)
        MODE = properties.getProperty("mode");
        // 获取结果输出格式
        RESULT_FORM = properties.getProperty("resultForm");
        // 获取最小置信度
        MIN_CONFIDENCE = properties.getProperty("minConfidence") != null ? Double.parseDouble(properties.getProperty("minConfidence")) : 0;
        // 获取最小支持度
        MIN_SUPPORT = properties.getProperty("minSupport") != null ? Double.parseDouble(properties.getProperty("minSupport")) : 0;
        // 获取训练备注
        COMMENT = properties.getProperty("comment");

        //初始化一个静态的CSVFileIO对象，仅作为测试，在生产环境下不要使用
        try {
            tmpFileIO = new CSVFileIO(RESULT_DIR_PATH, PATH_T, PATH_H, PATH_M, PATH_B, PATH_I, PATH_S);
        } catch (IOException e) {
            tmpFileIO = null;
            //logger.info("创建CSVFileIO对象失败");
        }
        fileIO = tmpFileIO;
        targetItemFieldNames = new ArrayList<>();
        targetItemFieldNames.add(new ArrayList<>());
        targetItemFieldNames.add(Arrays.asList(ATTRIBUTES_FIELD + "HOTEL_NAME", ATTRIBUTES_FIELD + "PRODUCT_NAME"));
        targetItemFieldNames.add(List.of(ATTRIBUTES_FIELD + "MEAL_CODE"));
        targetItemFieldNames.add(List.of(ATTRIBUTES_FIELD + "BAGGAGE_SPECIFICATION"));
        targetItemFieldNames.add(Arrays.asList(ATTRIBUTES_FIELD + "INSUR_PRO_NAME", ATTRIBUTES_FIELD + "INSURANCE_COMPANYCODE"));
        targetItemFieldNames.add(List.of(ATTRIBUTES_FIELD + "SEAT_NO"));

        targetItemNames = new ArrayList<>();
        targetItemNames.add(new ArrayList<>());
        targetItemNames.add(Arrays.asList("HOTEL_NAME", "PRODUCT_NAME"));
        targetItemNames.add(List.of( "MEAL_CODE"));
        targetItemNames.add(List.of("BAGGAGE_SPECIFICATION"));
        targetItemNames.add(Arrays.asList("INSUR_PRO_NAME","INSURANCE_COMPANYCODE"));
        targetItemNames.add(List.of("SEAT_NO"));

        //初始化属性储存的结构体
        for(int i = HOTEL;i < TEST_TICKET;i++) {
            itemAttributesStorage[i] = new ItemAttributesStorage();
            for (String s : itemAttributeNames.get(i)){
                itemAttributesStorage[i].addAttribute(s);
            }
        }

    }

    public static List<String> getTargetItemNames(int type) {
        return targetItemNames.get(type);
    }


    public static List<String> getTargetItemFieldNames(int type) {
        return targetItemFieldNames.get(type);
    }

    public static Map<String, List<List<String>>> getTestTicketsMap() {
        return testTicketsMap;
    }

    public static String[] getFullNames() {
        return FULL_NAMES;
    }

}
