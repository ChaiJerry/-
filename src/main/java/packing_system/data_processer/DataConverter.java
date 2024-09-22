package packing_system.data_processer;

import org.apache.spark.sql.*;
import org.bson.*;

import java.util.*;

import static packing_system.data_generating_system.FPGrowth.*;
import static packing_system.io.SharedAttributes.*;


public class DataConverter {
    private DataConverter() {
    }

    // 头等舱
    protected static List<String> firstClassGrades = new ArrayList<>(Arrays.asList("F", "A", "P"));

    // 公务舱
    protected static List<String> businessClassGrades = new ArrayList<>(Arrays.asList("C", "D"));

    // 普通舱（经济舱）及其折扣
    protected static List<String> economicClassGrades = new ArrayList<>(Arrays.asList(
            "B","Y", "K", "H", "M", "N", "L", "Q", "E", "T", "X", "U", "W", "R", "O"));

    // 其余是特殊舱位



    //将训练得到的包含关联规则信息的dataset中的数据转化为List<String>
    public static void dataset2RulesList(Dataset<Row> dataSet, List<List<String>> dataList) {
        //遍历dataSet中的每一行，将每一行转化为List<String>
        for (Row r : dataSet.collectAsList()) {
            //将每一行转化为String[]
            String[] rule=row2rule(r);
            //若rule长度为空，则说明无效应当跳过
            if(rule.length!=0){
                //将有效的rule添加到dataList中
                dataList.add(Arrays.asList(rule));
            }
        }
    }


    //将训练得到的包含频繁项集信息的dataset中的数据转化为List<String>
    public static void dataset2FRList(Dataset<Row> dataSet, List<List<String>> dataList) {
        for (Row r : dataSet.collectAsList()) {
            List<String> row = new ArrayList<>();
            //机票相关的属性
            StringBuilder ticketList = new StringBuilder();
            //商品相关的属性
            StringBuilder itemsList = new StringBuilder();
            //temp用于暂存字符串
            String temp;
            //row可以用get方法将其不同的部分得到
            //这里使用getlist(0)得到频繁项集，get(1)得到频率
            //遍历r.getList(0)
            for (Object s : r.getList(0)) {
                //将Object 转化为String，并暂存
                temp = s.toString();
                //归类
                if (isTicketType(temp)) {
                    //若是机票相关的属性
                    //将属性添加到ticketList中
                    ticketList.append(temp).append(";");
                } else {
                    //若是其余商品相关的属性
                    //将属性添加到itemsList中
                    itemsList.append(temp).append(";");
                }
            }
            //若是ticketList为空，或者goodList为空，则是无意义的频繁项集，直接跳过
            if ((ticketList.isEmpty()) || (itemsList.isEmpty())) {
                continue;
            }
            //将ticketList和itemsList转化到Row之中
            row.add(ticketList.toString());
            row.add(itemsList.toString());
            //将频率添加到Row之中
            row.add("Frequency::" + r.get(1).toString());
            //将Row添加到dataList之中
            dataList.add(row);
        }
    }

    //将每一行Row转化为String[]
    public static String[] row2rule(Row row) {
        StringBuilder stringBuilder = new StringBuilder();
        String[] columns = new String[3];
        //初始化columns
        columns[1] = "";
        columns[2] = "";
        //处理第0列antecedent
        String temp;
        for (Object s : row.getList(0)) {
            temp = s.toString();
            //如果前件之中包含有一个非机票类型的属性，则跳过
            if (!isTicketType(temp)) {
                //如果前件之中包含有一个非机票类型的属性，则返回空String[]
                return new String[0];
            }
            //否则，将属性添加到antecedent中
            stringBuilder.append(temp).append("; ");
        }
        //将antecedent转化到columns[0]中
        columns[0] = stringBuilder.toString();
        stringBuilder.delete(0, stringBuilder.length());
        //处理第1列(consequent)
        temp = row.getList(1).get(0).toString() + "; ";
        //判断后件是否为机票类型的属性
        //如果temp是机票类型的属性，则返回空String[]
        if (isTicketType(temp)) {
            return new String[0];
        }
        //否则，将属性添加到columns[1]中
        columns[1] += temp;
        //赋值置信度列
        columns[2] = "Confidence::"+row.get(2).toString();
        //返回结果
        return columns;
    }

    public static boolean isTicketType(String s) {
        return s.split(":")[0].equals("Ticket");
    }

    /**
     * 获取dataset中的数据，并转化为list
     * @param dataSet 需要转化的Dataset
     * @return 转化后的list
     */
    public static List<List<String>> dataset2stringlist(Dataset<Row> dataSet){
        //初始化list
        List<List<String>> list = new ArrayList<>();
        //遍历dataSet中的每一行，将每一行转化为List<String>
        for (Row r : dataSet.collectAsList()) {
            List<String> temp = new ArrayList<>();
            //将每个string添加到temp中
            for (Object s : r.getList(0)) {
                temp.add(s.toString());
            }
            //将temp添加到list中
            list.add(temp);
        }
        //返回list
        return list;
    }

    /**
     * 根据列表中每个“有效”的元素位置设置位。
     * @param list 列表
     * @return 一个整数，其中列表中的每个有效元素位置都对应一个被设置的位
     */
    public static <T> int listToBits(List<T> list) {
        int result = 0;
        int size = list.size();
        for (int i = 0; i < size; i++) {
            if (list.get(i) != null) {
                result |= (1 << i);
            }
        }
        return result;
    }



    public static int setBitPos2zero(int num, int bitPos) {
        return num & ~(1 << bitPos);
    }

    public static List<Map.Entry<String, String>> map2List(Map<String, String> map, int type) {
        String target;
        String targetName;
        switch (type) {
            case HOTEL:
                targetName = HOTEL_ATTRIBUTES[4];
                //targetName = HOTEL_ATTRIBUTES[3];
                target= map.get(targetName);
                map.clear();
                map.put(targetName, target);

                break;
            case MEAL:
                targetName = MEAL_ATTRIBUTES[0];
                target= map.get(targetName);
                map.clear();
                map.put(targetName, target);
                break;
            case BAGGAGE:
                targetName = BAGGAGE_ATTRIBUTES[1];
                target= map.get(targetName);
                map.clear();
                map.put(targetName, target);
                break;
            case INSURANCE:
                targetName = INSURANCE_ATTRIBUTES[2];
                target= map.get(targetName);
                map.clear();
                map.put(targetName, target);
                break;
            case SEAT:
                targetName = SEAT_ATTRIBUTES[1];
                target= map.get(targetName);
                map.clear();
                map.put(targetName, target);
                break;
            default:
                break;
        }
        return new ArrayList<>(map.entrySet());
    }

    public static String ticketGrade2Specific(String ticketGrade) {
        if(economicClassGrades.contains(ticketGrade)) {
            return "经济舱";
        }else if(businessClassGrades.contains(ticketGrade)){
            return "公务舱";
        }else if(firstClassGrades.contains(ticketGrade)){
            return "头等舱";
        }else {
            return "特殊舱";
        }
    }

    public static String getItemNameFromDocument(Document document,int type) {
        List<String> targetItemNames = getTargetItemNames(type);
        StringBuilder sb = new StringBuilder();
        for (String itemName : targetItemNames) {
            if(document.get(ATTRIBUTES_FIELD_NAME) != null){
                sb.append(((Document)document.get(ATTRIBUTES_FIELD_NAME))
                        .getString(itemName)).append(";");
            }
        }
        return sb.toString();
    }

    public static Dataset<Row> listOfAttributeList2Dataset (List<List<String>> attributeLists){
        List<Row> data = new ArrayList<>();
        for (List<String> attributeList : attributeLists) {
            data.add(RowFactory.create(attributeList));
        }
        return getDataFrame(data);
    }
}
