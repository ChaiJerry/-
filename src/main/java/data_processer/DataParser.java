package data_processer;

import com.csvreader.*;

import java.io.*;
import java.util.*;

import static data_processer.DataConverter.*;

public class DataParser {
    private DataParser() {
    }

    //用于添加到属性中的品类标识
    //机票标识
    private static final String T_SIGN = "Ticket:";
    //餐食标识
    private static final String M_SIGN = "Meal:";
    //行李标识
    private static final String B_SIGN = "Baggage:";
    //酒店标识
    private static final String H_SIGN = "Hotel:";
    //保险标识
    private static final String I_SIGN = "Insurance:";
    //选座标识
    private static final String S_SIGN = "Seat:";

    //处理餐食订单
    public static void dealM(CsvReader csvReader, Map<String, List<List<String>>> map) throws IOException {
        //餐食
        List<String> attributeList = new ArrayList<>();
        //通过订单号得到订单属性的存储列表的列表
        List<List<String>> listOfAttributeList = getListOfAttributeList(csvReader, map);
        //添加普通的类型属性
        for (int i = 1; i < 3; i++) {
            attributeList.add(M_SIGN + csvReader.getHeader(i) + ":" + csvReader.get(i));
        }
        //添加订单的划档位处理后的数据类属性
        attributeList.add(M_SIGN + csvReader.getHeader(3) + ":"
                + floatStr2Attribute(csvReader.get(3), 4));
        attributeList.add(M_SIGN + csvReader.getHeader(4) + ":"
                + floatStr2Attribute(csvReader.get(4), 400));
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
            //订单号相同的情况实际上不应该出现，因为订单号是唯一的
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

    public static boolean isRoundTrip(List<String> originAttributeList, String voyage) {
        //获取之前的出发地
        String originalFrom = originAttributeList.get(2).split(":")[2];
        //获取之前的目的地
        String originalTo = originAttributeList.get(3).split(":")[2];
        //判断是否为往返票
        return !voyage.equals(originalFrom + "-" + originalTo);
    }

    //处理行李订单
    public static void dealB(CsvReader csvReader, Map<String, List<List<String>>> map) throws IOException {
        List<String> attributeList = new ArrayList<>();
        //通过订单号得到订单属性的存储列表的列表
        List<List<String>> listOfAttributeList = getListOfAttributeList(csvReader, map);
        //添加划档处理后的数据类属性
        attributeList.add(B_SIGN + csvReader.getHeader(1) + ":"
                + floatStr2Attribute(csvReader.get(1), 200));
        attributeList.add(B_SIGN + csvReader.getHeader(2) + ":" + csvReader.get(2));
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
        attributeList.add(H_SIGN + "HPRICE" + ":" + floatStr2Attribute(csvReader.get(3), 300));
        //加入其它酒店属性
        for (int i = 4; i < csvReader.getColumnCount(); i++) {
            attributeList.add(H_SIGN + csvReader.getHeader(i) + ":" + csvReader.get(i));
        }
        //将属性列表放入listOfAttributeList中
        listOfAttributeList.add(attributeList);
    }

    //处理保险订单
    public static void dealI(CsvReader csvReader, Map<String, List<List<String>>> map) throws IOException {
        List<String> attributeList = new ArrayList<>();
        //通过订单号得到订单属性的存储列表的列表
        List<List<String>> listOfAttributeList = getListOfAttributeList(csvReader, map);
        //加入保险属性
        for (int i = 1; i < csvReader.getColumnCount(); i++) {
            String header = csvReader.getHeader(i);
            attributeList.add(I_SIGN + header + ":" + csvReader.get(i));
        }
        //将属性列表放入listOfAttributeList中
        listOfAttributeList.add(attributeList);
    }

    //处理选座订单
    public static void dealS(CsvReader csvReader, Map<String, List<List<String>>> map) throws IOException {
        List<String> attributeList = new ArrayList<>();
        //通过订单号得到订单属性的存储列表的列表
        List<List<String>> listOfAttributeList = getListOfAttributeList(csvReader, map);
        //加入选座属性
        for (int i = 1; i < csvReader.getColumnCount(); i++) {
            //直接加入数据之中的属性
            attributeList.add(S_SIGN + csvReader.getHeader(i) + ":" + csvReader.get(i));
        }
        //将属性列表放入listOfAttributeList中
        listOfAttributeList.add(attributeList);
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

    private static List<List<String>> getListOfAttributeList(CsvReader csvReader, Map<String, List<List<String>>> map) throws IOException {
        //得到订单号
        String key = csvReader.get(0);
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