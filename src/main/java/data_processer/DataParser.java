package data_processer;

import com.csvreader.*;

import java.io.*;
import java.util.*;

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
    public static void dealM(CsvReader csvReader, Map<String, List<String>> map) throws IOException {
        //餐食
        List<String> attributeList = new ArrayList<>();
        //得到订单号
        String key = csvReader.get(0);
        //订单号相同的情况检验
        if (map.containsKey(key)) {
            //订单号相同的情况实际上不应该出现，因为订单号是唯一的
            return;
        }
        //添加普通的类型属性
        for (int i = 1; i < 3; i++) {
            attributeList.add(M_SIGN + csvReader.getHeader(i) + ":" + csvReader.get(i));
        }
        //添加订单的划档位处理后的数据类属性
        attributeList.add(M_SIGN + csvReader.getHeader(3) + ":"
                + floatStr2Attribute(csvReader.get(3), 4));
        attributeList.add(M_SIGN + csvReader.getHeader(4) + ":"
                + floatStr2Attribute(csvReader.get(4), 400));
        //将订单号和属性列表放入map中
        map.put(key, attributeList);
    }

    //处理机票订单
    public static void dealT(CsvReader csvReader, Map<String, List<String>> map) throws IOException {
        //机票
        List<String> attributeList = new ArrayList<>();
        //得到订单号
        String key = csvReader.get("ORDER_NO");
        //订单号相同的情况检验
        if (map.containsKey(key)) {
            return;
        }
        //添加时间属性（划分到月）
        addDate2list(attributeList, csvReader.get(0), T_SIGN);
        attributeList.add(T_SIGN + csvReader.getHeader(5) + ":"
                + floatStr2Attribute(csvReader.get(5), 300));
        //添加目的地属性
        splitSegment(attributeList, csvReader.get(2));
        //添加其他属性
        for (int i = 1; i < 5; i++) {
            //跳过voyage属性
            if (i == 2) {
                continue;
            }
            //添加属性
            attributeList.add(T_SIGN + csvReader.getHeader(i) + ":" + csvReader.get(i));
        }
        //将订单号和属性列表放入map中
        map.put(key, attributeList);
    }

    //处理行李订单
    public  static void dealB(CsvReader csvReader, Map<String, List<String>> map) throws IOException {
        List<String> attributeList = new ArrayList<>();
        //得到订单号
        String key = csvReader.get(0);
        //订单号相同的情况检验
        if (map.containsKey(key)) {
            return;
        }
        //添加划档处理后的数据类属性
        attributeList.add(B_SIGN + csvReader.getHeader(1) + ":"
                + floatStr2Attribute(csvReader.get(1), 200));
        attributeList.add(B_SIGN + csvReader.getHeader(2) + ":" + csvReader.get(2));
        //添加其他属性
        map.put(key, attributeList);
    }

    //处理酒店订单
    public static void dealH(CsvReader csvReader, Map<String, List<String>> map) throws IOException {
        //酒店
        List<String> attributeList = new ArrayList<>();
        //得到订单号
        String key = csvReader.get(0);
        //订单号相同的情况检验
        if (map.containsKey(key)) {
            return;
        }
        //可以选择用addDate2list(attributeList, csvReader.get(1), H_SIGN)导入酒店入住时间属性
        attributeList.add(H_SIGN + "HPRICE" + ":" + floatStr2Attribute(csvReader.get(3), 300));
        //加入其它酒店属性
        for (int i = 4; i < csvReader.getColumnCount(); i++) {
            attributeList.add(H_SIGN + csvReader.getHeader(i) + ":" + csvReader.get(i));
        }
        //将订单号和属性列表放入map中
        map.put(key, attributeList);
    }

    //处理保险订单
    public static  void dealI(CsvReader csvReader, Map<String, List<String>> map) throws IOException {
        List<String> attributeList = new ArrayList<>();
        //得到订单号
        String key = csvReader.get(0);
        //订单号相同的情况检验
        if (map.containsKey(key)) {
            return;
        }
        //加入保险属性
        for (int i = 1; i < csvReader.getColumnCount(); i++) {
            String header = csvReader.getHeader(i);
            attributeList.add(I_SIGN + header + ":" + csvReader.get(i));
        }
        //将订单号和属性列表放入map中
        map.put(key, attributeList);
    }

    //处理选座订单
    public static void dealS(CsvReader csvReader, Map<String, List<String>> map) throws IOException {
        List<String> attributeList = new ArrayList<>();
        //得到订单号
        String key = csvReader.get(0);
        //订单号相同的情况检验
        if (map.containsKey(key)) {
            return;
        }
        //加入选座属性
        for (int i = 1; i < csvReader.getColumnCount(); i++) {
            //直接加入数据之中的属性
            attributeList.add(S_SIGN + csvReader.getHeader(i) + ":" + csvReader.get(i));
        }
        //将订单号和属性列表放入map中
        map.put(key, attributeList);
    }

    /**
     * 浮点数划档方法
     * @param str 包含浮点数内容的字符串
     * @param div 划档的间隔
     * @return 划档后的整数
     */
    private static Integer floatStr2Attribute(String str, int div) {
        //将字符串转化为浮点数
        float value = Float.parseFloat(str);
        return (int)value / div;
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
}
/*
 * 数据处理基本原则：
 * 1.性质类型数据直接保留即可
 * 2.时间类型数据，需要将时间拆分成月、日等
 * 3.数值类型数据，需要将数值进行划分，如：酒店价格，需要将价格划分为100元以下、200元以下等
 */