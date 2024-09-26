package packing_system.data_processer;

import com.csvreader.*;

import java.io.*;
import java.util.*;

import static packing_system.data_processer.DataConverter.*;
import static packing_system.io.SharedAttributes.*;

public class DataParser {
    private DataParser() {
    }


    //处理餐食订单
    public static void dealM(CsvReader csvReader, Map<String, List<List<String>>> map) throws IOException {
        //餐食
        List<String> attributeList = new ArrayList<>();
        //通过订单号得到订单属性的存储列表的列表
        List<List<String>> listOfAttributeList = getListOfAttributeList(csvReader, map);
        //添加餐食代码属性
        attributeList.add(M_SIGN + "MEAL_CODE" + ":" + csvReader.get("MEAL_CODE"));
        //添加订单的划档位处理后的数据类属性
        attributeList.add(M_SIGN + "PM_PRICE" + ":"
                + floatStr2Attribute(csvReader.get("PM_PRICE"), 4));

        //将属性列表放入listOfAttributeList中
        listOfAttributeList.add(attributeList);
    }

    //处理机票订单
    public static void dealT(CsvReader csvReader, Map<String, List<List<String>>> map) throws IOException {
        //机票属性列表
        List<String> attributeList = new ArrayList<>();
        //得到订单号
        String key = csvReader.get("ORDER_NO");
        List<List<String>> listofAttributeList;

        if (map.containsKey(key)) {
            attributeList = map.get(key).get(0);
            if (!csvReader.get(4).equals("ADT")) {
                attributeList.set(5, T_SIGN + "HAVE_CHILD" + ":" + "1");
            }
            //处理一个机票订单之中有往返票的情况
            if (isRoundTrip(attributeList, csvReader.get(2))) {
                //如果之前没有添加往返票信息，则添加一次
                if (map.get(key).size() == 1) {
                    List<String> newAttributeList = new ArrayList<>(attributeList);
                    newAttributeList.set(2, T_SIGN + "FROM" + ":" + csvReader.get(2).split("-")[0]);
                    newAttributeList.set(3, T_SIGN + "TO" + ":" + csvReader.get(2).split("-")[1]);
                    map.get(key).add(newAttributeList);
                } else {
                    //如果之前已经添加了往返票信息，则修改往返票信息
                    // ，使得两个订单的除了起始地点之外的属性相同
                    List<String> newAttributeList = new ArrayList<>(attributeList);
                    newAttributeList.set(2, T_SIGN + "FROM" + ":" + csvReader.get(2).split("-")[1]);
                    newAttributeList.set(3, T_SIGN + "TO" + ":" + csvReader.get(2).split("-")[0]);
                    map.get(key).set(1, newAttributeList);
                }
            }
        } else {
            listofAttributeList = new ArrayList<>();
            //添加时间属性（划分到月） 0
            addDate2list(attributeList, csvReader.get(0), T_SIGN);
            //添加航司属性 1
            attributeList.add(T_SIGN + csvReader.getHeader(1) + ":" + csvReader.get(1));
            //添加出发地、目的地属性 2,3
            splitSegment(attributeList, csvReader.get(2));
            //添加航班号属性 4
            attributeList.add(T_SIGN + csvReader.getHeader(3) + ":" + ticketGrade2Specific(csvReader.get(3)));
            //添加是否有孩童票属性 5
            attributeList.add(T_SIGN + "HAVE_CHILD" + ":" + (csvReader.get(4).equals("ADT") ? "0" : "1"));
            //添加票价属性 6
            attributeList.add(T_SIGN + csvReader.getHeader(5) + ":"
                    + floatStr2Attribute(csvReader.get(5), 300));
            //将订单号和属性列表放入map中
            listofAttributeList.add(attributeList);
            map.put(key, listofAttributeList);
        }
    }

    public static void dealE(CsvReader csvReader, Map<String, List<List<String>>> map) throws IOException {
        //机票属性列表
        List<String> attributeList = new ArrayList<>();
        //得到订单号
        String key = csvReader.get("ORDER_NO");
        List<List<String>> listofAttributeList;

        if (map.containsKey(key)) {
            attributeList = map.get(key).get(0);
            if (!csvReader.get("T_PASSENGER").equals("ADT")) {
                attributeList.set(4, T_SIGN + "HAVE_CHILD" + ":" + "1");
                if(map.get(key).size() == 2){
                    map.get(key).get(1).set(4,T_SIGN + "HAVE_CHILD" + ":" + "1");
                }
            }
            //处理一个机票订单之中有往返票的情况
            if (isRoundTrip(attributeList, csvReader.get("T_VOYAGE"))) {
                //如果之前没有添加往返票信息，则添加一次
                if (map.get(key).size() == 1) {
                    List<String> newAttributeList = new ArrayList<>(attributeList);
                    newAttributeList.set(1, T_SIGN + "FROM" + ":" + csvReader.get("T_VOYAGE").split("-")[0]);
                    newAttributeList.set(2, T_SIGN + "TO" + ":" + csvReader.get("T_VOYAGE").split("-")[1]);
                    map.get(key).add(newAttributeList);
                } else {
                    //如果之前已经添加了往返票信息，则修改往返票信息
                    // ，使得两个订单的除了起始地点之外的属性相同
                    List<String> newAttributeList = new ArrayList<>(attributeList);
                    newAttributeList.set(1, T_SIGN + "FROM" + ":" + csvReader.get("T_VOYAGE").split("-")[1]);
                    newAttributeList.set(2, T_SIGN + "TO" + ":" + csvReader.get("T_VOYAGE").split("-")[0]);
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
            attributeList.add(T_SIGN + "HAVE_CHILD" + ":" + (csvReader.get("T_PASSENGER").equals("ADT") ? "0" : "1"));
            //添加PROMOTION_RATE属性 5
            attributeList.add(T_SIGN + "PROMOTION_RATE" + ":" + csvReader.get("PROMOTION_RATE"));
            //添加T_FORMER属性 6
            attributeList.add(T_SIGN + "T_FORMER" + ":" + floatStr2Attribute(Objects.equals(csvReader.get("T_FORMER"), "")
                    ?"-1000.0":csvReader.get("T_FORMER"), 1000));
            //将订单号和属性列表放入map中
            listofAttributeList.add(attributeList);
            map.put(key, listofAttributeList);
        }
    }

    public static void dealTest(CsvReader csvReader, Map<String, List<List<String>>> map) throws IOException {
        //机票属性列表
        List<String> attributeList = new ArrayList<>();
        //得到订单号
        String key = csvReader.get("ORDER_NO");
        List<List<String>> listofAttributeList;

        if (map.containsKey(key)) {
            attributeList = map.get(key).get(0);
            if (!csvReader.get("T_PASSENGER").equals("ADT")) {
                attributeList.set(4, T_SIGN + "HAVE_CHILD" + ":" + "1");
                if(map.get(key).size() == 2){
                    map.get(key).get(1).set(4,T_SIGN + "HAVE_CHILD" + ":" + "1");
                }
            }
            //处理一个机票订单之中有往返票的情况
            if (isRoundTrip(attributeList, csvReader.get("T_VOYAGE"))) {
                //如果之前没有添加往返票信息，则添加一次
                if (map.get(key).size() == 1) {
                    List<String> newAttributeList = new ArrayList<>(attributeList);
                    newAttributeList.set(1, T_SIGN + "FROM" + ":" + csvReader.get("T_VOYAGE").split("-")[0]);
                    newAttributeList.set(2, T_SIGN + "TO" + ":" + csvReader.get("T_VOYAGE").split("-")[1]);
                    map.get(key).add(newAttributeList);
                } else {
                    //如果之前已经添加了往返票信息，则修改往返票信息
                    // ，使得两个订单的除了起始地点之外的属性相同
                    List<String> newAttributeList = new ArrayList<>(attributeList);
                    newAttributeList.set(1, T_SIGN + "FROM" + ":" + csvReader.get("T_VOYAGE").split("-")[1]);
                    newAttributeList.set(2, T_SIGN + "TO" + ":" + csvReader.get("T_VOYAGE").split("-")[0]);
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
            attributeList.add(T_SIGN + "HAVE_CHILD" + ":" + (csvReader.get("T_PASSENGER").equals("ADT") ? "0" : "1"));
            //添加PROMOTION_RATE属性 5
            attributeList.add(T_SIGN + "PROMOTION_RATE" + ":" + csvReader.get("PROMOTION_RATE"));
            //添加T_FORMER属性 6
            attributeList.add(T_SIGN + "T_FORMER" + ":" + floatStr2Attribute(Objects.equals(csvReader.get("T_FORMER"), "")
                    ?"-1000.0":csvReader.get("T_FORMER"), 1000));
            //添加T_CARRIER属性 7
            attributeList.add(T_SIGN + "T_CARRIER" + ":" + csvReader.get("T_CARRIER"));
            //将订单号和属性列表放入map中
            listofAttributeList.add(attributeList);
            map.put(key, listofAttributeList);
        }
    }

    public static boolean isRoundTrip(List<String> originAttributeList, String voyage) {
        //获取之前的出发地
        String originalFrom = originAttributeList.get(1).split(":")[2];
        //获取之前的目的地
        String originalTo = originAttributeList.get(2).split(":")[2];
        //判断是否为往返票
        return !voyage.equals(originalFrom + "-" + originalTo);
    }

    //处理行李订单
    public static void dealB(CsvReader csvReader, Map<String, List<List<String>>> map) throws IOException {
        List<String> attributeList = new ArrayList<>();
        //通过订单号得到订单属性的存储列表的列表
        List<List<String>> listOfAttributeList = getListOfAttributeList(csvReader, map);
        //添加划档处理后的数据类属性
        attributeList.add(B_SIGN + "PAYMENTAMOUNT" + ":"
                + floatStr2Attribute(csvReader.get("PAYMENTAMOUNT")
                .isEmpty()?"0":csvReader.get("PAYMENTAMOUNT"), 200));
        attributeList.add(B_SIGN + "BAGGAGE_SPECIFICATION" + ":" + csvReader.get("BAGGAGE_SPECIFICATION"));
        //attributeList.add(B_SIGN + NEW_ATT + ":" + csvReader.get(NEW_ATT));
        //attributeList.add(B_SIGN + NEW_ATT+ ":" + floatStr2Attribute(csvReader.get(NEW_ATT), 3000));

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
        attributeList.add(H_SIGN + "AIR_REAL_MONEY" + ":" + floatStr2Attribute(csvReader.get("AIR_REAL_MONEY"), 300));
        //加入其它酒店属性
        attributeList.add(H_SIGN + "PRODUCTTYPE" + ":" + csvReader.get("PRODUCTTYPE"));
        attributeList.add(H_SIGN + "PRODUCT_NAME" + ":" + csvReader.get("PRODUCT_NAME"));
        attributeList.add(H_SIGN + "HOTEL_NAME" + ":" + csvReader.get("HOTEL_NAME"));

        //将属性列表放入listOfAttributeList中
        listOfAttributeList.add(attributeList);
    }

    //处理保险订单
    public static void dealI(CsvReader csvReader, Map<String, List<List<String>>> map) throws IOException {
        List<String> attributeList = new ArrayList<>();
        //通过订单号得到订单属性的存储列表的列表
        List<List<String>> listOfAttributeList = getListOfAttributeList(csvReader, map);
        //加入保险属性

        attributeList.add(I_SIGN + "INSUR_AMOUNT" + ":" + csvReader.get("INSUR_AMOUNT"));
        attributeList.add(I_SIGN + "INSUR_PRO_NAME" + ":" + csvReader.get("INSUR_PRO_NAME"));
        attributeList.add(I_SIGN + "INSURANCE_COMPANYCODE" + ":" + csvReader.get("INSURANCE_COMPANYCODE"));

        //将属性列表放入listOfAttributeList中
        listOfAttributeList.add(attributeList);
    }

    //处理选座订单
    public static void dealS(CsvReader csvReader, Map<String, List<List<String>>> map) throws IOException {
        List<String> attributeList = new ArrayList<>();
        //通过订单号得到订单属性的存储列表的列表
        List<List<String>> listOfAttributeList = getListOfAttributeList(csvReader, map);
        //加入选座属性,如：
        //    "PAYINTEGRAL": "0",
        //    "SEAT_NO": null
        attributeList.add(S_SIGN + "SEAT_NO" + ":" + getSeatPosition(csvReader.get("SEAT_NO")));
        //attributeList.add(S_SIGN + NEW_ATT + ":" + csvReader.get(NEW_ATT));
        //将属性列表放入listOfAttributeList中
        listOfAttributeList.add(attributeList);
    }
    public static void dealCommon(CsvReader csvReader, Map<String, List<List<String>>> map,String sign,String attributeName) throws IOException {
        //餐食
        List<String> attributeList = new ArrayList<>();
        //通过订单号得到订单属性的存储列表的列表
        List<List<String>> listOfAttributeList = getListOfAttributeList(csvReader, map);

        //普通
        attributeList.add(sign+ attributeName+ ":" + csvReader.get(attributeName));

        //数值
//        attributeList.add(sign + attributeName + ":"
//                + floatStr2Attribute(csvReader.get(ttributeName), 4));

        //将属性列表放入listOfAttributeList中
        listOfAttributeList.add(attributeList);
    }

    public static  String getSeatPosition(String seatNo) {
        //获取座位号
        return seatNo.substring(seatNo.length()-1);
    }

    /**
     * 浮点数划档方法
     *
     * @param str 包含浮点数内容的字符串
     * @param div 划档的间隔
     * @return 划档后的整数
     */
    private static Integer floatStr2Attribute(String str, int div) {
        //将字符串转化为浮点数
        float value = Float.parseFloat(str);
        return (int) value / div;
    }

    //将日期拆分成月
    private static void addDate2list(List<String> list, String date, String sign) {
        //拆分日期
        String[] results = date.split(" ")[0].split("/");
        //添加月
        list.add(sign + "MONTH:" + results[1]);
    }

    //拆分航段
    private static void splitSegment(List<String> list, String segment) {
        //拆分航段
        String[] results = segment.split("-");
        //添加航段
        list.add(T_SIGN + "FROM" + ":" + results[0]);
        list.add(T_SIGN + "TO" + ":" + results[1]);
    }

    public static List<List<String>> getListOfAttributeList(CsvReader csvReader, Map<String, List<List<String>>> map) throws IOException {
        //得到订单号
        String key = csvReader.get("ORDER_NO");
        if(key.isEmpty()) {
            key = csvReader.get(0);
        }

        List<List<String>> listOfAttributeList;
        //订单号相同的情况检验
        if (map.containsKey(key)) {
            listOfAttributeList = map.get(key);
        } else {
            listOfAttributeList = new ArrayList<>();
            map.put(key, listOfAttributeList);
        }
        return listOfAttributeList;
    }
}
/*
 * 数据处理基本原则：
 * 1.性质类型数据直接保留即可
 * 2.时间类型数据，需要将时间拆分成月、日等
 * 3.数值类型数据，需要将数值进行划分，如：酒店价格，需要将价格划分为100元以下、200元以下等
 */