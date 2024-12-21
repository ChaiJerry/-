package bundle_system.io;

public class ConstItemAttributes {
    private ConstItemAttributes() {
    }
    public static final String MONTH = "MONTH";
    public static final String FROM = "FROM";
    public static final String TO = "TO";
    public static final String T_GRADE = "T_GRADE";
    public static final String HAVE_CHILD = "HAVE_CHILD";
    public static final String PROMOTION_RATE = "PROMOTION_RATE";
    public static final String T_FORMER = "T_FORMER";
    public static final String AIR_REAL_MONEY = "AIR_REAL_MONEY";
    public static final String PRODUCTTYPE = "PRODUCTTYPE";
    public static final String PRODUCT_NAME = "PRODUCT_NAME";
    public static final String HOTEL_NAME = "HOTEL_NAME";
    public static final String MEAL_CODE = "MEAL_CODE";
    public static final String PM_PRICE = "PM_PRICE";
    public static final String PAYMENTAMOUNT = "PAYMENTAMOUNT";
    public static final String BAGGAGE_SPECIFICATION = "BAGGAGE_SPECIFICATION";
    public static final String INSUR_AMOUNT = "INSUR_AMOUNT";
    public static final String INSUR_PRO_NAME = "INSUR_PRO_NAME";
    public static final String INSURANCE_COMPANYCODE = "INSURANCE_COMPANYCODE";
    public static final String SEAT_NO = "SEAT_NO";
    protected static final String[] SEAT_ATTRIBUTES = {SEAT_NO};
    protected static final String[] INSURANCE_ATTRIBUTES =  {INSUR_AMOUNT, INSUR_PRO_NAME, INSURANCE_COMPANYCODE};
    protected static final String[] BAGGAGE_ATTRIBUTES = {PAYMENTAMOUNT, BAGGAGE_SPECIFICATION};
    protected static final String[] MEAL_ATTRIBUTES = {MEAL_CODE, PM_PRICE};
    protected static final String[] HOTEL_ATTRIBUTES = {AIR_REAL_MONEY, PRODUCTTYPE, PRODUCT_NAME, HOTEL_NAME};
    //用于存储每个品类下的商品属性，用于快速查找，之后要是添加新的属性或者品类，则在此处添加
    protected static final String[] TICKET_ATTRIBUTES = {MONTH, FROM, TO, T_GRADE, HAVE_CHILD, PROMOTION_RATE, T_FORMER};
}
