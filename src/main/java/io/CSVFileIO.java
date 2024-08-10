package io;

import com.csvreader.*;
import org.apache.spark.sql.*;

import java.io.*;
import java.nio.charset.*;
import java.util.*;
import java.util.logging.*;

import static data_processer.DataConverter.*;
import static data_processer.DataParser.*;
import static io.SharedAttributes.*;
import static data_generating_system.FPGrowth.*;
import static io.MongoUtils.*;

public class CSVFileIO {

    //csv文件结果输出的目录路径
    private final String resultDirPath;
    //csv文件读取的路径，数组长度为types.length，数组下标与types对应
    private final String[] csvPaths = new String[types.length];

    private final Logger logger;

    //得到订单数量
    public int getOrderNumber() {
        return orderNumber;
    }

    //订单数量
    protected static int orderNumber;

    /**
     * 初始化CSVFileIO
     */
    public CSVFileIO(String resultDirPath, String pathT
            , String pathH, String pathM, String pathB
            , String pathI , String pathS) throws IOException {
        // 初始化路径
        // 输出结果路径
        this.resultDirPath = resultDirPath;
        // 机票订单相关数据csv文件路径
        csvPaths[TICKET] = pathT;
        // 酒店相关数据csv文件路径
        csvPaths[HOTEL] = pathH;
        // 餐食相关数据csv文件路径
        csvPaths[MEAL] = pathM;
        // 行李相关数据csv文件路径
        csvPaths[BAGGAGE] = pathB;
        // 保险相关数据csv文件路径
        csvPaths[INSURANCE] = pathI;
        // 座位相关数据csv文件路径
        csvPaths[SEAT] = pathS;
        // 初始化类型与索引的映射
        for (int i = 0; i < types.length; i++) {
            type2index.put(types[i], i);
        }
        //首先读取Ticket订单相关信息方便建立订单和属性之间的映射
        ticketMap = CSVFileIO.read(csvPaths[TICKET], "T");
        //训练用机票订单
        testTicketMap = getTestMap();
        //测试用机票订单
        trainingTicketsMap = getTrainingMap();
        // 初始化日志
        logger = Logger.getLogger(getClass().getName());

    }

    public void csv2DB() throws IOException {
        for(int i = 0; i < types.length; i++) {
            singleTypeCsv2database(i);
        }
    }

    public void singleTypeCsv2database(int type) throws IOException {
        //订单与属性之间的映射map
        Map<String, List<List<String>>> attributeMap;
        if (type != TICKET) {
            // 当读取的csv文件不是Ticket时，直接处理
            attributeMap = CSVFileIO.read(csvPaths[type], types[type]);
            ordersMap2DB(attributeMap, type);
        } else {
            ordersMap2DB(getTestMap(), type);
        }
    }

    /**
     * Convert a CSV file to a Dataset<Row>.
     */
    public Dataset<Row> singelTypeCsv2dataset(int type) throws IOException {
        //订单与属性之间的映射map
        Map<String, List<List<String>>> attributeMap;
        if (type != TICKET) {
            // 当读取的csv文件不是Ticket时，直接处理
            attributeMap = CSVFileIO.read(csvPaths[type], types[type]);
            // 创建数据集
            List<Row> data = new ArrayList<>();
            //筛选出能和机票订单号匹配的订单数据
            // 遍历机票的订单号，只将已经有对应的机票订单号的订单数据添加到data中
            for (Iterator<String> iterator = trainingTicketsMap.keySet().iterator(); iterator.hasNext(); ) {
                String key = iterator.next();
                // 得到属性列表
                List<List<String>> attributeLists = attributeMap.get(key);
                // 如果temp不为空，则将temp添加到data中
                // 若是为空则说明没有商品可以和该订单匹配，则跳过
                if (attributeLists != null) {
                    for(List<String> attributeList : attributeLists) {
                        attributeList.addAll(trainingTicketsMap.get(key).get(0));
                        data.add(RowFactory.create(attributeList));
                    }
                }
            }
            // 创建DataFrame，并指定模式，将List<Row>数据转换为Dataset<Row>
            logger.info("正在创建DataFrame");
            return getDataFrame(data);
        } else {
            // 当读取的csv文件是Ticket时
            List<Row> data = new ArrayList<>();
            // 暂时不会有等于TICKET的情况，若是以后有，则可以添加在这个地方
            return getDataFrame(data);
        }
    }

    public List<List<String>> singleTypeCsv2lists(int type) throws IOException{
        return dataset2stringlist(singelTypeCsv2dataset(type));
    }

    /**
     * 将频繁项集转换为CSV文件
     * @param dataSet 频繁项集
     * @param type 商品类型
     */
    public void freItemSet2CSV(Dataset<Row> dataSet, int type) {
        // 创建 CSV Writer 对象, 参数说明（写入的文件路径，分隔符，编码格式)
        CsvWriter csvWriter = new CsvWriter(resultDirPath + "\\FreqItemSet" + FULL_NAMES[type] + ".csv", ',', StandardCharsets.UTF_8);
        try {
            // 定义 header 头
            String[] headers = {"Ticket", FULL_NAMES[type], "freq"};
            // 写入 header 头
            csvWriter.writeRecord(headers);
            for (Row r : dataSet.collectAsList()) {
                //遍历r.getList(0)
                String temp;
                String[] columns = new String[3];
                for (int i = 0; i < 3; i++) {
                    columns[i] = "";
                }
                for (Object s : r.getList(0)) {
                    temp = s.toString() + "; ";
                    columns[temp.charAt(0) == 'T' ? 0 : 1] += temp;
                }
                if (columns[0].isEmpty() || columns[1].isEmpty()) {
                    continue;
                }
                columns[2] = r.get(1).toString();
                csvWriter.writeRecord(columns);
            }
        } catch (Exception e) {
            logger.info("freItemSet2CSV error");
        } finally {
            csvWriter.close();
        }
    }

    /**
     * 将关联规则写入到CSV文件
     * @param rules 关联规则
     * @param type  商品类型
     */
    public void rules2CSV(Dataset<Row> rules, int type) throws IOException {
        // 定义 header 头
        String[] headers = {"TICKET", FULL_NAMES[type], "confidence"};
        // 创建 CSV Writer 对象, 参数说明（写入的文件路径，分隔符，编码格式)
        CsvWriter csvWriter = new CsvWriter(resultDirPath + "\\" + "AssociationRules" + FULL_NAMES[type] + ".csv", ',', StandardCharsets.UTF_8);
        // 写入 header 头
        csvWriter.writeRecord(headers);
        // 遍历 rules
        for (Row r : rules.collectAsList()) {
            // 获取 Row 的内容
            String[] columns = row2rule(r);
            if(columns.length != 0) {
                //若是不为空则说明有效，写入CSV文件
                csvWriter.writeRecord(columns);
            }
        }
        // 关闭 CSV Writer
        csvWriter.close();
    }


    public Map<String, List<List<String>>> read(int type) throws IOException {
        return CSVFileIO.read(csvPaths[type], types[type]);
    }

    /**
     * 读取CSV文件
     * @param path 文件路径
     * @param type 商品类型
     * @return 返回订单号和订单对应的商品属性之间的键值对 Map<String, List<String>>
     */
    public static Map<String, List<List<String>>> read(String path, String type) throws IOException {
        // 创建HashMap，key为订单号，value为订单对应的属性键值对
        HashMap<String, List<List<String>>> map = new HashMap<>();
        // 第一参数：读取文件的路径 第二个参数：分隔符 第三个参数：字符集
        CsvReader csvReader = new CsvReader(path, ',', StandardCharsets.UTF_8);
        // 读取表头
        csvReader.readHeaders();
        //通过type判断调用哪个方法
        //将表头存入HeaderStorage之中，方便后续存入数据库
        itemAttributesStorage[type2index.get(type)] = new ItemAttributesStorage();
        //得到对应的属性头类
        ItemAttributesStorage header = itemAttributesStorage[type2index.get(type)];
        for(int i=1;i<csvReader.getHeaderCount();i++) {
            //将表头存入HeaderStorage之中，方便后续存入数据库
            header.addAttribute(csvReader.getHeader(i));
        }
        switch (type) {
            case "M":
                //处理餐食数据
                while (csvReader.readRecord()) {
                    //订单数量计数
                    orderNumber++;
                    dealM(csvReader, map);
                }
                break;
            case "T":
                //特殊处理机票数据的属性
                //添加处理后得到的属性头
                header.addAttribute("MONTH");
                header.addAttribute("TO");
                header.addAttribute("FROM");
                header.addAttribute("HAVE_CHILD");
                //读取csv文件时会将一些不需要的属性头删读入，这里需要删除
                //删去多余的属性头
                header.removeAttribute("T_VOYAGE");
                header.removeAttribute("T_PASSENGER");
                //遍历csv每一行中的内容
                while (csvReader.readRecord()) {
                    //订单数量计数
                    orderNumber++;
                    dealT(csvReader, map);
                }
                break;
            case "B":
                //处理行李数据
                while (csvReader.readRecord()) {
                    //订单数量计数
                    orderNumber++;
                    dealB(csvReader, map);
                }
                break;
            case "H":
                //删除入住和退房时间属性头
                header.removeAttribute("IN_DATE");
                header.removeAttribute("OUT_DATE");
                //处理酒店数据
                while (csvReader.readRecord()) {
                    //订单数量计数
                    orderNumber++;
                    dealH(csvReader, map);
                }
                break;
            case "I":
                //处理保险数据
                while (csvReader.readRecord()) {
                    //订单数量计数
                    orderNumber++;
                    dealI(csvReader, map);
                }
                break;
            case "S":
                //处理座位数据
                while (csvReader.readRecord()) {
                    //订单数量计数
                    orderNumber++;
                    dealS(csvReader, map);
                }
                break;
            default:
                break;
        }
        return map;
    }

    //为了将训练集和测试集分开，读取csv中的训练集keys
    public static Set<String> getTrainingKeys() throws IOException {
        Set<String> keys = new HashSet<>();
        String filePath = "C:\\Users\\mille\\Desktop\\数据集\\ticket - 副本.csv";
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                int commaIndex = line.indexOf(',');
                // 对每一行进行处理，这里只是简单地打印出来
                String result = line.substring(commaIndex+1) + ",";
                keys.add(result);
            }
        }
        return keys;
    }

    //得到训练集
    public static Map<String, List<List<String>>> getTrainingMap() throws IOException {
        Set<String> keys = getTrainingKeys();
        Map<String, List<List<String>>> trainingMap = new HashMap<>();
        //遍历ticketMap
        for(Map.Entry<String, List<List<String>>> entry : ticketMap.entrySet()) {
            //如果key在keys中，则加入到trainingMap中
            if(keys.contains(generateItemKeyFromAttributes(entry.getValue().get(0)))) {
                trainingMap.put(entry.getKey(), entry.getValue());
            }
        }
        return trainingMap;
    }

    //得到测试集
    public static Map<String, List<List<String>>> getTestMap() throws IOException {
        Set<String> keys = getTrainingKeys();
        Map<String, List<List<String>>> testMap = new HashMap<>();
        //遍历ticketMap
        for(String key : ticketMap.keySet()) {
            String itemKey = generateItemKeyFromAttributes(ticketMap.get(key).get(0));
            if(key.equals("202203010403035972")){
                System.out.println("wa");
            }

            //如果key在keys中，则加入到trainingMap中
            if(!keys.contains(itemKey)) {

                testMap.put(key, ticketMap.get(key));
            }
        }
        if(testMap.containsValue("202202202003284774")) {
            System.out.println("bad");
        }

        return testMap;
    }

    public static String generateItemKeyFromAttributes(List<String> attributes){
        StringBuilder stringBuilder=new StringBuilder();
        for(int i = 0; i < attributes.size()-1;i++){
            stringBuilder.append(attributes.get(i).split(":")[2]).append(",");
        }
        return stringBuilder.toString();
    }
}
