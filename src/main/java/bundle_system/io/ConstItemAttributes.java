package bundle_system.io;

import java.util.*;

/**
 * 商品属性常量类，包含了各种商品相关的属性名称。
 * 这些常量用于在代码中统一管理商品属性的字符串标识符，
 */
public class ConstItemAttributes {
    /**
     * 私有构造函数，防止实例化此类。
     * 由于这是一个常量类，不需要创建实例。
     */
    private ConstItemAttributes() {
    }

    // 定义各个商品属性的常量字符串
    ////////////////////////////////////////////////////////////////////////////////////
    //机票相关属性
    /**
     * 表示月份的机票属性。
     */
    public static final String MONTH = "MONTH";

    /**
     * 表示出发地的机票属性。
     */
    public static final String FROM = "FROM";

    /**
     * 表示目的地的机票属性。
     */
    public static final String TO = "TO";

    /**
     * 表示机票等级的机票属性。
     */
    public static final String T_GRADE = "T_GRADE";

    /**
     * 表示是否有儿童的机票属性。
     */
    public static final String HAVE_CHILD = "HAVE_CHILD";

    /**
     * 表示促销率的机票属性。
     */
    public static final String PROMOTION_RATE = "PROMOTION_RATE";

    /**
     * 表示折扣前价格的机票属性。
     */
    public static final String T_FORMER = "T_FORMER";

    ////////////////////////////////////////////////////////////////////////////////////
    //酒店相关属性

    /**
     * 表示实际支付金额的商品属性。
     */
    public static final String AIR_REAL_MONEY = "AIR_REAL_MONEY";

    /**
     * 表示酒店类型的商品属性。
     */
    public static final String PRODUCTTYPE = "PRODUCTTYPE";

    /**
     * 表示酒店产品名称的商品属性。
     */
    public static final String PRODUCT_NAME = "PRODUCT_NAME";

    /**
     * 表示酒店名称的商品属性。
     */
    public static final String HOTEL_NAME = "HOTEL_NAME";

    ////////////////////////////////////////////////////////////////////////////////////
    //餐食相关属性

    /**
     * 表示餐食代码的商品属性。
     */
    public static final String MEAL_CODE = "MEAL_CODE";

    /**
     * 表示套餐价格的商品属性。
     */
    public static final String PM_PRICE = "PM_PRICE";

    ////////////////////////////////////////////////////////////////////////////////////
    //行李相关属性

    /**
     * 表示行李支付金额的商品属性。
     */
    public static final String PAYMENTAMOUNT = "PAYMENTAMOUNT";

    /**
     * 表示行李规格的商品属性。
     */
    public static final String BAGGAGE_SPECIFICATION = "BAGGAGE_SPECIFICATION";

    ///////////////////////////////////////////////////////////////////////
    //保险相关属性

    /**
     * 表示保险金额的商品属性。
     */
    public static final String INSUR_AMOUNT = "INSUR_AMOUNT";

    /**
     * 表示保险产品名称的商品属性。
     */
    public static final String INSUR_PRO_NAME = "INSUR_PRO_NAME";

    /**
     * 表示保险公司代码的商品属性。
     */
    public static final String INSURANCE_COMPANYCODE = "INSURANCE_COMPANYCODE";
    ///////////////////////////////////////////////////////////////////////
    //座位相关属性

    /**
     * 表示座位号的商品属性。
     */
    public static final String SEAT_NO = "SEAT_NO";

    // 定义各个品类下的商品属性数组，便于快速查找

    /**
     * 包含座位相关信息的商品属性数组。
     */
    protected static final String[] SEAT_ATTRIBUTES = {SEAT_NO};

    /**
     * 包含保险相关信息的商品属性数组。
     */
    protected static final String[] INSURANCE_ATTRIBUTES = {INSUR_AMOUNT, INSUR_PRO_NAME, INSURANCE_COMPANYCODE};

    /**
     * 包含行李相关信息的商品属性数组。
     */
    protected static final String[] BAGGAGE_ATTRIBUTES = {PAYMENTAMOUNT, BAGGAGE_SPECIFICATION};

    /**
     * 包含餐食相关信息的商品属性数组。
     */
    protected static final String[] MEAL_ATTRIBUTES = {MEAL_CODE, PM_PRICE};

    /**
     * 包含酒店相关信息的商品属性数组。
     */
    protected static final String[] HOTEL_ATTRIBUTES = {AIR_REAL_MONEY, PRODUCTTYPE, PRODUCT_NAME, HOTEL_NAME};

    /**
     * 包含机票相关信息的商品属性数组。
     * 如果需要添加新的属性或品类，可以在相应的地方进行扩展。
     */
    protected static final String[] TICKET_ATTRIBUTES = {MONTH, FROM, TO, T_GRADE, HAVE_CHILD, PROMOTION_RATE, T_FORMER};

    //之后要是添加新的品类，则在此处添加，并注册到itemAttributeNames中
    protected static final List<String[]> itemAttributeNames = new ArrayList<>();
    static {
        itemAttributeNames.add(ConstItemAttributes.TICKET_ATTRIBUTES);
        itemAttributeNames.add(ConstItemAttributes.HOTEL_ATTRIBUTES);
        itemAttributeNames.add(ConstItemAttributes.MEAL_ATTRIBUTES);
        itemAttributeNames.add(ConstItemAttributes.BAGGAGE_ATTRIBUTES);
        itemAttributeNames.add(ConstItemAttributes.INSURANCE_ATTRIBUTES);
        itemAttributeNames.add(ConstItemAttributes.SEAT_ATTRIBUTES);
        //用于训练测试的属性，这里和普通机票属性存储一样
        itemAttributeNames.add(ConstItemAttributes.TICKET_ATTRIBUTES);
        //之后要是添加新的品类，则在此处添加
    }
}



