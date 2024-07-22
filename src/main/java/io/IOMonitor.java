package io;

import java.util.*;

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
    //存储每个品类下的商品属性，用于快速查找，主要从CSV文件的头文件读取
    protected static HeaderStorage[] headerStorage = new HeaderStorage[6];

}
