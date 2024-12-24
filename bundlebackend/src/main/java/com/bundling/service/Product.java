package com.bundling.service;

import java.io.*;
import java.nio.file.*;
import java.util.*;


import com.bundling.vo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static bundle_system.io.ConstItemAttributes.*;
import static com.bundling.controllers.TrainController.*;

public class Product implements Comparable<Product> {

    public final String name;
    public final String price;
    public final Map<String,String> attributes;
    private Double priority = 0.0;
    public Double getPriority() {
        return priority;
    }
    public Product(String name, String price, Map<String,String> attributes) {
        this.name = name;
        this.price = price;
        this.attributes = attributes;
    }

    void setPriority(Map<String, String> commendedAttributes) {
        for(Map.Entry<String,String> entry : commendedAttributes.entrySet()) {
            String attributeName = entry.getKey();
            if (attributes.containsKey(attributeName) && entry.getValue().equals(attributes.get(attributeName))) {
                priority ++;
            }
        }
    }

    private ProductVO toProductVO() {
        return new ProductVO(name, price);
    }

    @Override
    public int compareTo(Product other) {
        // Compare based on priority in descending order
        return Double.compare(other.priority, this.priority);
    }

    public Product copy() {
        return new Product(name, price, attributes);
    }



    //////////////////////////////////////////////////////////////////////////////////////
    //以下为静态的方法和属性
    // 日志记录器
    private static final Logger logger = LoggerFactory.getLogger("Product");
    // 找到保存产品的目录路径
    public static final Path productsPath = projectRootDir.resolve("products");
    // 共享的产品列表，用于存储不同类型产品的信息，每个列表对应一种产品类型
    private static List<List<Product>> sharedProductLists = null;
    // 错误信息，用于记录读取产品CSV文件时发生的错误
    public static final String ERROR_READING_PRODUCTS_FROM_CSV_FILE = "Error reading products from CSV file";


    /**
     * 获取共享的产品列表的副本，用于外部调用时不会修改原始数据。
     * @return 包含产品信息的列表的副本
     */
    public static List<List<Product>> getProductListsCopy() {
        // 如果共享产品列表为空，则初始化产品列表
        if (sharedProductLists == null) {
            initializeProductLists();
        }

        // 创建一个临时产品列表集合
        List<List<Product>> tmpProductLists = new ArrayList<>();

        // 遍历共享产品列表
        for (List<Product> productList : sharedProductLists) {
            // 为当前产品列表创建一个新的列表来存储其副本
            List<Product> newList = new ArrayList<>();
            // 遍历当前产品列表中的每个产品
            for (Product product : productList) {
                // 创建产品的副本并添加到新列表中
                newList.add(product.copy());
            }
            // 将包含产品副本的新列表添加到临时产品列表集合中
            tmpProductLists.add(newList);
        }

        // 返回包含所有产品副本的临时产品列表集合
        return tmpProductLists;
    }

    /**
     * 初始化共享产品列表。
     *
     * 该方法会创建一个新的ArrayList来存储不同类型的产品列表。
     * 首先，它会跳过ticket和HOTEL类型的产品，不将它们添加到列表中。
     * 然后，它会将MEAL、BAGGAGE、INSURANCE和SEAT类型的产品分别添加到列表中。
     * 每种类型的产品列表都是通过调用相应的方法（如getMealProducts、getBaggageProducts等）来获取的。
     */
    private static void initializeProductLists() {
        // 初始化共享产品列表
        sharedProductLists = new ArrayList<>();

        // 跳过ticket类型的产品
        sharedProductLists.add(new ArrayList<>());

        // 跳过HOTEL类型的产品
        sharedProductLists.add(new ArrayList<>());

        // 加入MEAL类型的产品
        sharedProductLists.add(getMealProducts());

        // 加入BAGGAGE类型的产品
        sharedProductLists.add(getBaggageProducts());

        // 加入INSURANCE类型的产品
        sharedProductLists.add(getInsuranceProducts());

        // 加入SEAT类型的产品
        sharedProductLists.add(getSeatProducts());
    }

    /**
     * 从 Seat.csv 文件中按行读取产品信息
     *
     * @return 包含产品信息的列表
     */
    private static List<Product> getSeatProducts() {
        // 初始化价格列表，用于随机分配给选座产品的价格
        List<String> prices = Arrays.asList("0", "0", "0", "1000", "1000", "2000");
        // 初始化产品列表
        List<Product> productList = new ArrayList<>();
        // 读取Seat.csv文件
        Path mealCsvPath = productsPath.resolve("Seat.csv");
        try (BufferedReader br = Files.newBufferedReader(mealCsvPath)) {
            String line;
            boolean isFirstLine = true;
            while ((line = br.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                // 初始化产品属性映射
                Map<String, String> attributes = new HashMap<>();
                // 拆分产品行，获取产品属性值
                String[] productAttributeValues = splitProductLine(line);
                // 设置座位号属性
                attributes.put(SEAT_NO, getPriorityBySeat(productAttributeValues[0]));
                // 随机分配价格，并创建产品对象
                Collections.shuffle(prices);
                Product product = new Product(
                        getPriorityBySeat(productAttributeValues[0])
                                +"("+productAttributeValues[0]+")"
                        , prices.get(0), attributes);

                // 将产品添加到产品列表中
                productList.add(product);
            }
        } catch (IOException e) {
            // 如果在读取CSV文件时发生异常，则记录错误日志
            logger.error(ERROR_READING_PRODUCTS_FROM_CSV_FILE, e);
        }

        // 返回产品列表
        return productList;
    }


    /**
     * 从 Insurance.csv 文件中按行读取产品信息
     *
     * @return 包含产品信息的列表
     */
    private static List<Product> getInsuranceProducts() {
        List<Product> productList = new ArrayList<>();
        // 获取 Insurance.csv 文件的路径
        Path mealCsvPath = productsPath.resolve("Insurance.csv");
        try (BufferedReader br = Files.newBufferedReader(mealCsvPath)) {
            String line;
            boolean isFirstLine = true;
            while ((line = br.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                // 初始化产品属性映射
                Map<String, String> attributes = new HashMap<>();
                // 拆分产品行，获取产品属性值
                String[] productAttributeValues = splitProductLine(line);
                // 设置保险金额属性
                attributes.put(INSUR_AMOUNT, productAttributeValues[0]);
                // 设置保险产品名称属性
                attributes.put(INSUR_PRO_NAME, productAttributeValues[1]);
                // 设置保险公司代码属性
                attributes.put(INSURANCE_COMPANYCODE, productAttributeValues[2]);
                // 创建保险产品对象
                Product product = new Product(
                        productAttributeValues[1]+"(保险公司代码："+productAttributeValues[2]+")"
                        , productAttributeValues[0]
                        , attributes);
                // 将保险产品添加到产品列表中
                productList.add(product);
            }
        } catch (IOException e) {
            // 如果在读取CSV文件时发生异常，则记录错误日志
            logger.error(ERROR_READING_PRODUCTS_FROM_CSV_FILE, e);
        }
        // 返回包含保险产品的列表
        return productList;
    }
    /**
     * 从 Baggage.csv 文件中按行读取产品信息
     *
     * @return 包含产品信息的列表
     */
    private static List<Product> getBaggageProducts() {
        List<Product> productList = new ArrayList<>();
        Path mealCsvPath = productsPath.resolve("Baggage.csv");
        try (BufferedReader br = Files.newBufferedReader(mealCsvPath)) {
            String line;
            boolean isFirstLine = true;
            while ((line = br.readLine()) != null) {
                // 跳过CSV文件的第一行（通常是标题行）
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                // 初始化产品属性映射
                Map<String, String> attributes = new HashMap<>();
                // 拆分产品行，获取产品属性值
                String[] productAttributeValues = splitProductLine(line);
                // 设置行李服务金额属性
                attributes.put(PAYMENTAMOUNT, productAttributeValues[0]);
                // 设置行李规格属性
                attributes.put(BAGGAGE_SPECIFICATION, productAttributeValues[1]);
                // 创建行李服务产品对象，并设置其名称、价格和属性
                Product product = new Product("行李服务重量:" + productAttributeValues[1]
                        , getPrice(productAttributeValues[0], 200)
                        , attributes);
                // 将行李服务产品添加到产品列表中
                productList.add(product);
            }
        } catch (IOException e) {
            // 如果在读取CSV文件时发生异常，则记录错误日志
            logger.error(ERROR_READING_PRODUCTS_FROM_CSV_FILE, e);
        }
        // 返回包含行李服务产品的列表
        return productList;
    }

    /**
     * 从 Meal.csv 文件中按行读取产品信息
     *
     * @return 包含产品信息的列表
     */
    private static List<Product> getMealProducts() {
        List<Product> productList = new ArrayList<>();
        // 获取Meal.csv文件的路径
        Path mealCsvPath = productsPath.resolve("Meal.csv");
        try (BufferedReader br = Files.newBufferedReader(mealCsvPath)) {
            String line;
            boolean isFirstLine = true;
            while ((line = br.readLine()) != null) {
                // 跳过第一行（通常是标题行）
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                // 初始化产品属性映射
                Map<String, String> attributes = new HashMap<>();
                // 拆分产品行，获取产品属性值
                String[] productAttributeValues = splitProductLine(line);
                // 设置餐食代码属性
                attributes.put(MEAL_CODE, productAttributeValues[0]);
                // 设置餐食价格属性
                attributes.put(PM_PRICE, productAttributeValues[1]);
                // 创建餐食产品对象，并设置其名称、价格和属性
                Product product = new Product(getMealName(productAttributeValues[0])
                        , getPrice(productAttributeValues[1], 4)
                        , attributes);
                // 将餐食产品添加到产品列表中
                productList.add(product);
            }
        } catch (IOException e) {
            // 如果在读取CSV文件时发生异常，则记录错误日志
            logger.error(ERROR_READING_PRODUCTS_FROM_CSV_FILE, e);
        }
        // 返回包含餐食产品的列表
        return productList;
    }

    /**
     * 将产品列表转换为ProductVO列表用于发给前端
     * ，最多返回前五个
     *
     * @param productList 产品列表
     * @param commendedAttributes 推荐的属性
     * @return ProductVO列表，最多返回前五个
     */
    public static List<ProductVO> getProductVOList(List<Product> productList, Map<String, String> commendedAttributes) {
        // 如果推荐的属性中包含座位号，则将其转换为优先级
        if (commendedAttributes.containsKey(SEAT_NO)) {
            commendedAttributes.put(SEAT_NO, getPriorityBySeat(commendedAttributes.get(SEAT_NO)));
        }
        // 初始化ProductVO列表
        List<ProductVO> productVOList = new ArrayList<>();
        // 遍历产品列表，为每个产品设置推荐的属性
        for (Product product : productList) {
            product.setPriority(commendedAttributes);
        }
        // 打乱产品列表的顺序
        Collections.shuffle(productList);
        // 对产品列表进行排序
        Collections.sort(productList);
        // 转换为ProductVO列表，最多返回前五个
        for (int i = 0; i < Math.min(productList.size(), 5); i++) {
            // 将每个产品转换为ProductVO并添加到列表中
            productVOList.add(productList.get(i).toProductVO());
        }
        // 返回ProductVO列表
        return productVOList;
    }

    private static String[] splitProductLine(String productLine) {
        return productLine.split(",");
    }
    private static String getPrice(String price,int grade){
        return ((int)(Double.parseDouble(price)*grade))+"";
    }

    private static final Map<String, String> seatPriorityMap = new HashMap<>();

    static {
        // 初始化座位优先级映射
        seatPriorityMap.put("A", "靠窗座位");
        seatPriorityMap.put("K", "靠窗座位");
        String string = "靠走廊座位";
        seatPriorityMap.put("C", string);
        seatPriorityMap.put("D", string);
        seatPriorityMap.put("G", string);
        seatPriorityMap.put("H", string);
    }

    /**
     * 根据座位字母返回对应的优先级值
     *
     * @param seatNo 座位字母
     * @return 对应的优先级值，如果没有找到则返回空字符串
     */
    public static String getPriorityBySeat(String seatNo) {
        if (seatNo == null || seatNo.isEmpty()) {
            return "普通座位";
        }
        return seatPriorityMap.getOrDefault(seatNo.toUpperCase(), "普通座位");
    }

    private static final Map<String, String> mealCodeToNameMap = new HashMap<>();

    static {
        // 初始化映射关系
        mealCodeToNameMap.put("BLAK", "黑胡椒菲力牛排");
        mealCodeToNameMap.put("BRST", "红粉烤鸡胸");
        mealCodeToNameMap.put("POUT", "香煎银鳕鱼");
        mealCodeToNameMap.put("BARB", "德式烤肉拼盘");
        mealCodeToNameMap.put("CHET", "烤鸡胸佐烧烤汁");
        mealCodeToNameMap.put("FRUI06", "水果蛋糕6寸");
        mealCodeToNameMap.put("BEEF", "菲力牛排佐茴香汁");
        mealCodeToNameMap.put("HOKI", "粉/黑胡椒长尾鳕");
        mealCodeToNameMap.put("SALM", "威灵顿三文鱼");
        mealCodeToNameMap.put("CHIC", "百里香汁烤鸡排");
        mealCodeToNameMap.put("CHLD08", "儿童蛋糕8寸");
        mealCodeToNameMap.put("BARB", "German-Style Barbecue Platter");
        mealCodeToNameMap.put("CHLD08", "Child's Cake (8 inch)");
        mealCodeToNameMap.put("BLAK", "Black Pepper Beef Fillet");
        mealCodeToNameMap.put("CBL", "黑胡椒菲力牛排");
        mealCodeToNameMap.put("CHE", "烤鸡胸佐烧烤汁");
        mealCodeToNameMap.put("YAP", "芥末烤羊排");
        mealCodeToNameMap.put("CCC", "忌廉椰风蛋糕");
        mealCodeToNameMap.put("CBA", "德式烤肉拼盘");
        mealCodeToNameMap.put("CCH", "百里香汁烤鸡排");
        mealCodeToNameMap.put("CSA", "威灵顿三文鱼");
        mealCodeToNameMap.put("CBL", "Black Pepper Beef Fillet");
        mealCodeToNameMap.put("CVS", "高纤维蔬菜什锦沙拉");
        mealCodeToNameMap.put("TBS", "金枪鱼贝果三明治");
        mealCodeToNameMap.put("TRS", "提拉米苏");
        mealCodeToNameMap.put("MES", "和风肉松鸡蛋三明治");
        mealCodeToNameMap.put("CBA", "German-Style Barbecue Platter");
        mealCodeToNameMap.put("TMS", "深海金枪鱼蔬菜沙拉");
        mealCodeToNameMap.put("CSH", "俱乐部什锦三明治");
        mealCodeToNameMap.put("PBP", "烤牛肉帕尼尼三明治");
        mealCodeToNameMap.put("SSV", "老北京炒烤牛肉饭");
        mealCodeToNameMap.put("HAV", "香草鸡肉蔬菜沙拉");
        mealCodeToNameMap.put("BPG", "京味猪肉豆角焖面");
        mealCodeToNameMap.put("PQV", "鲜虾藜麦蔬菜沙拉");
        mealCodeToNameMap.put("BPR", "煎牛排配红酒蘑菇汁");
        mealCodeToNameMap.put("TBE", "海岛烧烤");
        mealCodeToNameMap.put("BPR", "Pan-fried Beef Tenderloin");
        mealCodeToNameMap.put("PBG", "香煎海鲈鱼配蒜蓉法香黄油");
    }

    /**
     * 根据餐点代码返回餐点名称的方法
     * @param mealCode 餐点代码
     * @return 餐点名称，如果找不到对应的代码则返回null
     */
    public static String getMealName(String mealCode) {
        return mealCodeToNameMap.get(mealCode);
    }
}
