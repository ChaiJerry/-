package bundle_system.data_processer;

import com.csvreader.*;

import java.io.*;
import java.util.*;

import static bundle_system.data_processer.DataConverter.*;
import static bundle_system.io.ConstItemAttributes.*;
import static bundle_system.io.SharedAttributes.*;

public class DataParser {

    //乘客类型属性的属性名
    public static final String T_PASSENGER = "T_PASSENGER";
    //订单号属性的属性名
    public static final String ORDER_NO = "ORDER_NO";
    //机票航段的属性名前缀
    //形式为xxx-xxx
    public static final String T_VOYAGE = "T_VOYAGE";
    private DataParser() {
    }



    /**
     * 这是处理机票订单的方法
     * ，该方法会将订单号和属性列表放入map中
     * ，如果订单号是已有的，则将属性列表加入对应的订单号对应的商品属性列表的列表中
     * 详细说明：（所有的商品处理方式类似）
     *  1.这里的属性都是从csv文件之中读取的
     *  2.传入的csvReader是在读一行的信息
     *      ，当想要得到该行某一列的信息时（比如第6列）
     *      ，则有两种方法：
     *      （1）使用csvReader.get(5)是在读取第6列的信息（这种方式应该会效率更高，但是会比较受限于特定格式）
     *      （2）使用csvReader.get(“csv文件列名”)读取该列名对应的列中的信息
     *      dealT方法中，使用了csvReader.get(数字)的方式来读取信息，因为csv解析库无法通过列名来读取第0列的信息
     *      因此当确定好之后输入的数据后请做好适配，将每一列代表的信息固定下来
     *  3.主要读取的信息分为两个部分：
     *      （1）订单号（ORDER_NO），用于建立机票和商品之间的联系，订单号相同时便认为是同一个订单内，便于之后将机票和商品的属性放在一起训练
     *      （2）商品属性：用于训练模型，比如餐食的价格、机票的折扣率等
     *  4.这里预设机票csv文件内容是固定的，如果要更改csv文件的内容，则需要更改该方法
     * @param csvReader csvReader 读取器对象
     * @param map map 订单号和属性列表的映射
     * @throws IOException 异常
     */
    public static void dealT(CsvReader csvReader, Map<String, List<List<String>>> map) throws IOException {
        //机票属性列表
        List<String> attributeList = new ArrayList<>();
        //得到订单号
        String key = csvReader.get(ORDER_NO);
        List<List<String>> listofAttributeList;

        if (map.containsKey(key)) {
            attributeList = map.get(key).get(0);
            checkChild(csvReader, map, attributeList, key);
            //处理一个机票订单之中有往返票的情况
            if (isRoundTrip(attributeList, csvReader.get(T_VOYAGE))) {
                //如果之前没有添加往返票信息，则添加一次
                if (map.get(key).size() == 1) {
                    List<String> newAttributeList = new ArrayList<>(attributeList);

                    newAttributeList.set(1, T_SIGN + FROM + ":" + csvReader.get(T_VOYAGE).split("-")[0]);
                    newAttributeList.set(2, T_SIGN + TO + ":" + csvReader.get(T_VOYAGE).split("-")[1]);
                    map.get(key).add(newAttributeList);
                } else {
                    //如果之前已经添加了往返票信息，则修改往返票信息
                    // ，使得两个订单的除了起始地点之外的属性相同
                    List<String> newAttributeList = new ArrayList<>(attributeList);
                    newAttributeList.set(1, T_SIGN + FROM + ":" + csvReader.get(T_VOYAGE).split("-")[1]);
                    newAttributeList.set(2, T_SIGN + TO + ":" + csvReader.get(T_VOYAGE).split("-")[0]);
                    map.get(key).set(1, newAttributeList);
                }
            }
        } else {
            listofAttributeList = new ArrayList<>();
            //添加时间属性（划分到月） 0
            addDate2list(attributeList, csvReader.get(2), T_SIGN);
            //添加出发地、目的地属性 1,2
            splitSegment(attributeList, csvReader.get(5));
            //添加航班等级属性 3
            attributeList.add(T_SIGN + csvReader.getHeader(6) + ":" + ticketGrade2Specific(csvReader.get(6)));
            //添加是否有孩童票属性 4
            attributeList.add(T_SIGN + HAVE_CHILD + ":" + (csvReader.get(T_PASSENGER).equals("ADT") ? "0" : "1"));
            //添加PROMOTION_RATE属性 5
            attributeList.add(T_SIGN + PROMOTION_RATE + ":" + floatStr2Attribute(csvReader.get(PROMOTION_RATE), 10));
            //添加T_FORMER属性 6
            attributeList.add(T_SIGN + T_FORMER + ":" + floatStr2Attribute(Objects.equals(csvReader.get(T_FORMER), "")
                    ?"-1000.0":csvReader.get(T_FORMER), 1000));
            //将订单号和属性列表放入map中
            listofAttributeList.add(attributeList);
            map.put(key, listofAttributeList);
        }
    }

    /**
     * 处理餐食订单的方法
     * @param csvReader 读取器对象
     * @param map map 订单号和属性列表的映射
     * @throws IOException 异常
     */
    public static void dealM(CsvReader csvReader, Map<String, List<List<String>>> map) throws IOException {
        //餐食
        List<String> attributeList = new ArrayList<>();
        //通过订单号得到订单属性的存储列表的列表
        List<List<String>> listOfAttributeList = getListOfAttributeList(csvReader, map);
        //添加餐食代码属性
        attributeList.add(M_SIGN + MEAL_CODE + ":" + csvReader.get(MEAL_CODE ));
        //添加订单的划档位处理后的数据类属性
        attributeList.add(M_SIGN + PM_PRICE + ":"
                + floatStr2Attribute(csvReader.get(PM_PRICE), 4));

        //将属性列表放入listOfAttributeList中
        listOfAttributeList.add(attributeList);
    }


    /**
     * 处理背包订单的方法
     * @param csvReader csvReader 读取器对象
     * @param map map 订单号和属性列表的映射
     * @throws IOException 异常
     */
    public static void dealB(CsvReader csvReader, Map<String, List<List<String>>> map) throws IOException {
        List<String> attributeList = new ArrayList<>();
        //通过订单号得到订单属性的存储列表的列表
        List<List<String>> listOfAttributeList = getListOfAttributeList(csvReader, map);
        //添加划档处理后的数据类属性
        attributeList.add(B_SIGN + PAYMENTAMOUNT + ":"
                + floatStr2Attribute(csvReader.get(PAYMENTAMOUNT)
                .isEmpty()?"0":csvReader.get(PAYMENTAMOUNT), 200));
        attributeList.add(B_SIGN + BAGGAGE_SPECIFICATION + ":" + csvReader.get(BAGGAGE_SPECIFICATION));

        //将属性列表放入listOfAttributeList中
        listOfAttributeList.add(attributeList);
    }

    //处理酒店订单
    public static void dealH(CsvReader csvReader, Map<String, List<List<String>>> map) throws IOException {
        //酒店
        List<String> attributeList = new ArrayList<>();
        ///通过订单号得到订单属性的存储列表的列表
        List<List<String>> listOfAttributeList = getListOfAttributeList(csvReader, map);
        //可以选择用addDate2list(attributeList, csvReader.get(1), H_SIGN)导入酒店入住时间属性
        attributeList.add(H_SIGN + AIR_REAL_MONEY + ":" + floatStr2Attribute(csvReader.get(AIR_REAL_MONEY), 300));
        //加入其它酒店属性
        attributeList.add(H_SIGN + PRODUCTTYPE + ":" + csvReader.get(PRODUCTTYPE));
        attributeList.add(H_SIGN + PRODUCT_NAME + ":" + csvReader.get(PRODUCT_NAME));
        attributeList.add(H_SIGN + HOTEL_NAME + ":" + csvReader.get(HOTEL_NAME));

        //将属性列表放入listOfAttributeList中
        listOfAttributeList.add(attributeList);
    }

    //处理保险订单
    public static void dealI(CsvReader csvReader, Map<String, List<List<String>>> map) throws IOException {
        List<String> attributeList = new ArrayList<>();
        //通过订单号得到订单属性的存储列表的列表
        List<List<String>> listOfAttributeList = getListOfAttributeList(csvReader, map);
        //加入保险属性

        attributeList.add(I_SIGN + INSUR_AMOUNT + ":" + csvReader.get(INSUR_AMOUNT));
        attributeList.add(I_SIGN + INSUR_PRO_NAME + ":" + csvReader.get(INSUR_PRO_NAME));
        attributeList.add(I_SIGN + INSURANCE_COMPANYCODE + ":" + csvReader.get(INSURANCE_COMPANYCODE));

        //将属性列表放入listOfAttributeList中
        listOfAttributeList.add(attributeList);
    }

    //处理选座订单
    public static void dealS(CsvReader csvReader, Map<String, List<List<String>>> map) throws IOException {
        List<String> attributeList = new ArrayList<>();
        //通过订单号得到订单属性的存储列表的列表
        List<List<String>> listOfAttributeList = getListOfAttributeList(csvReader, map);
        //加入选座属性
        attributeList.add(S_SIGN + SEAT_NO + ":" + getSeatPosition(csvReader.get(SEAT_NO)));
        //将属性列表放入listOfAttributeList中
        listOfAttributeList.add(attributeList);
    }

    /**
     * 获取座位行字母的方法
     * @param seatNo 座位号
     * @return 座位行字母
     */
    public static  String getSeatPosition(String seatNo) {
        //获取座位行号
        return seatNo.substring(seatNo.length()-1);
    }

    /**
     * 浮点数划档方法
     *
     * @param str 包含浮点数内容的字符串
     * @param div 划档的间隔
     * @return 划档后的整数
     */
    public static Integer floatStr2Attribute(String str, int div) {
        //将字符串转化为浮点数
        double value = Float.parseFloat(str) + LITTLE_DOUBLE;
        return ((int) value / div);
    }

    /**
     * 添加时间属性到列表中
     * @param list 属性列表
     * @param date 日期
     * @param sign 商品前缀
     */
    private static void addDate2list(List<String> list, String date, String sign) {
        //拆分日期
        String[] results = date.split(" ")[0].split("/");
        //添加月
        list.add(sign + "MONTH:" + results[1]);
    }

    /**
     * 拆分航段的方法
     * @param list 属性列表
     * @param segment 航段
     */
    private static void splitSegment(List<String> list, String segment) {
        //拆分航段
        String[] results = segment.split("-");
        //添加航段
        list.add(T_SIGN + FROM + ":" + results[0]);
        list.add(T_SIGN + TO + ":" + results[1]);
    }

    /**
     * 通过商品csv中订单号得到对应的机票订单属性的存储列表
     * 这个方法能成立的前提是：
     *  （1）在调用该方法之前，已经通过订单号将机票订单的属性列表存储到了map中
     *  （2）该方法在调用之前，已经将csvReader定位到了当前订单所在的行
     *  （3）商品的订单号在csvReader（或者说csv文件）中是第一列
     * @param csvReader csv文件读取器
     * @param map   机票订单号和属性列表的map
     * @return  机票订单属性的存储列表
     * @throws IOException  异常处理
     */
    public static List<List<String>> getListOfAttributeList(CsvReader csvReader, Map<String, List<List<String>>> map) throws IOException {
        //得到订单号
        String key = csvReader.get(ORDER_NO);
        //这是为了处理csv解析库的bug，可能不能用列名读出第一列的东西
        if(key.isEmpty()) {
            key = csvReader.get(0);
        }
        //订单号对应的属性列表的引用
        List<List<String>> listOfAttributeList;
        //订单号相同的情况检验
        if (map.containsKey(key)) {
            //订单号相同，直接取出对应的属性列表
            listOfAttributeList = map.get(key);
        } else {
            //订单号不同，新建空的属性列表
            //返回的也会是空的属性列表，后续会添加数据
            listOfAttributeList = new ArrayList<>();
            map.put(key, listOfAttributeList);
        }
        return listOfAttributeList;
    }
    /**
     * 检查是否有孩子，如果有则将属性列表中的HAVE_CHILD设置为1
     * @param csvReader csvReader 读取器对象
     * @param map map 订单号和属性列表的映射
     * @param attributeList 属性列表
     * @param key 订单号
     * @throws IOException 异常
     */
    private static void checkChild(CsvReader csvReader, Map<String, List<List<String>>> map, List<String> attributeList, String key) throws IOException {
        if (!csvReader.get(T_PASSENGER).equals("ADT")) {
            attributeList.set(4, T_SIGN + HAVE_CHILD + ":" + "1");
            if(map.get(key).size() == 2){
                map.get(key).get(1).set(4,T_SIGN + HAVE_CHILD + ":" + "1");
            }
        }
    }

    /**
     * 专用与测试集测试评估的方法
     * @param csvReader csvReader 读取器对象
     * @param map map 订单号和属性列表的映射
     * @throws IOException 输入输出异常
     */
    public static void dealTest(CsvReader csvReader, Map<String, List<List<String>>> map) throws IOException {
        //机票属性列表
        List<String> attributeList = new ArrayList<>();
        //得到订单号
        String key = csvReader.get(ORDER_NO);
        List<List<String>> listofAttributeList;
        if (map.containsKey(key)) {
            attributeList = map.get(key).get(0);
            checkChild(csvReader, map, attributeList, key);
            //处理一个机票订单之中有往返票的情况
            if (isRoundTrip(attributeList, csvReader.get(T_VOYAGE))) {
                //如果之前没有添加往返票信息，则添加一次
                if (map.get(key).size() == 1) {
                    List<String> newAttributeList = new ArrayList<>(attributeList);
                    newAttributeList.set(1, T_SIGN + FROM + ":" + csvReader.get(T_VOYAGE).split("-")[0]);
                    newAttributeList.set(2, T_SIGN + TO + ":" + csvReader.get(T_VOYAGE).split("-")[1]);
                    map.get(key).add(newAttributeList);
                } else {
                    //如果之前已经添加了往返票信息，则修改往返票信息
                    // ，使得两个订单的除了起始地点之外的属性相同
                    List<String> newAttributeList = new ArrayList<>(attributeList);
                    newAttributeList.set(1, T_SIGN + FROM + ":" + csvReader.get(T_VOYAGE).split("-")[1]);
                    newAttributeList.set(2, T_SIGN + TO + ":" + csvReader.get(T_VOYAGE).split("-")[0]);
                    map.get(key).set(1, newAttributeList);
                }
            }
        } else {
            listofAttributeList = new ArrayList<>();
            //添加时间属性（划分到月） 0
            addDate2list(attributeList, csvReader.get(2), T_SIGN);
            //添加出发地、目的地属性 1,2
            splitSegment(attributeList, csvReader.get(5));
            //添加航班等级属性 3
            attributeList.add(T_SIGN + csvReader.getHeader(6) + ":" + ticketGrade2Specific(csvReader.get(6)));
            //添加是否有孩童票属性 4
            attributeList.add(T_SIGN + HAVE_CHILD + ":" + (csvReader.get(T_PASSENGER).equals("ADT") ? "0" : "1"));
            //添加PROMOTION_RATE属性 5
            attributeList.add(T_SIGN + PROMOTION_RATE + ":" + csvReader.get(PROMOTION_RATE));
            //添加T_FORMER属性 6
            attributeList.add(T_SIGN + T_FORMER + ":" + floatStr2Attribute(Objects.equals(csvReader.get(T_FORMER), "")
                    ?"-1000.0":csvReader.get(T_FORMER), 1000));
            //添加T_CARRIER属性 7
            // （这个属性的加入是为了确定商品的唯一属性联合主键）
            attributeList.add(T_SIGN + "T_CARRIER" + ":" + csvReader.get("T_CARRIER"));
            //将订单号和属性列表放入map中
            listofAttributeList.add(attributeList);
            map.put(key, listofAttributeList);
        }
    }

    /**
     * 检查是否为往返票
     * @param originAttributeList 原始属性列表
     * @param voyage voyage 航段
     * @return 是否为往返票的布尔值
     */
    public static boolean isRoundTrip(List<String> originAttributeList, String voyage) {
        //获取之前的出发地
        String originalFrom = originAttributeList.get(1).split(":")[2];
        //获取之前的目的地
        String originalTo = originAttributeList.get(2).split(":")[2];
        //判断是否为往返票
        return !voyage.equals(originalFrom + "-" + originalTo);
    }

}
/*
 * 数据处理基本原则：
 * 1.性质类型数据直接保留即可
 * 2.时间类型数据，需要将时间拆分成月、日等
 * 3.数值类型数据，需要将数值进行划分，如：酒店价格，需要将价格划分为100元以下、200元以下等
 */