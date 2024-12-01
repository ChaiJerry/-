package bundle_system.io;

import com.csvreader.*;
import org.apache.spark.sql.*;

import java.io.*;
import java.nio.charset.*;
import java.util.*;
//import java.util.logging.*;

import static bundle_system.data_processer.DataConverter.*;
import static bundle_system.data_processer.DataParser.*;
import static bundle_system.data_generating_system.FPGrowth.*;
import static bundle_system.io.SharedAttributes.*;

public class CSVFileIO {

    //csv文件结果输出的目录路径
    private String resultDirPath;
    //csv文件读取的路径，数组长度为types.length，数组下标与types对应
    private final String[] csvPaths = new String[FULL_NAMES.length + 2];
    protected Map<String, List<List<String>>> trainTicketsMap;
    // 初始化日志记录器
//    private static final Logger logger = Logger.getLogger(CSVFileIO.class.getName());

    //得到订单数量
    public int getOrderNumber() {
        return orderNumber;
    }

    //订单数量
    protected int orderNumber;

    /**
     * 初始化CSVFileIO
     */
    public CSVFileIO(String resultDirPath, String pathT
            , String pathH, String pathM, String pathB
            , String pathI, String pathS) throws IOException {
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
        //首先读取Ticket订单相关信息方便建立订单和属性之间的映射
        //测试用机票订单
        SharedAttributes.testTicketsMap = read(PATH_TEST_T, TEST_TICKET);
        //训练用机票订单
        trainTicketsMap = read(PATH_TRAIN_T, TRAIN_TICKET);
    }

    /**
     * 初始化CSVFileIO，用于实际生产环境的构造方法
     * @param pathT 机票订单csv文件路径
     * @param pathH 酒店csv文件路径，如果不用就写成null
     * @param pathM 餐食csv文件路径
     * @param pathB 行李csv文件路径
     * @param pathI 保险csv文件路径
     * @param pathS 选座csv文件路径
     */
    public CSVFileIO(String pathT
            , String pathH, String pathM, String pathB
            , String pathI, String pathS) throws IOException {
        // 初始化路径
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
        //首先读取Ticket订单相关信息方便建立订单和属性之间的映射
        //训练用机票订单
        trainTicketsMap = read(csvPaths[TICKET], TRAIN_TICKET);
    }

    /**
     * 将csv存入MongoDB数据库的方法
     */
    public void csv2DB() throws IOException {
        for (int i = 1; i < 6; i++) {
            singleTypeCsv2database(i);
        }
    }

    public void singleTypeCsv2database(int type) throws IOException {
        //订单与属性之间的映射map
        Map<String, List<List<String>>> attributeMap;
        if (type != SharedAttributes.TICKET) {
            // 当读取的csv文件不是Ticket时，直接处理
            attributeMap = read(csvPaths[type], type);
            MongoUtils.ordersMap2DB(attributeMap, type);
        } else {
            // 当读取的csv文件是Ticket时
            // 将测试集放入数据库
            MongoUtils.ordersMap2DB(SharedAttributes.getTestTicketsMap(), type);
        }
    }

    /**
     * Convert a CSV file to a Dataset<Row>.
     */
    public Dataset<Row> singleTypeCsv2dataset(int type,int eva) throws IOException {
        //订单与属性之间的映射map
        Map<String, List<List<String>>> attributeMap;
        if (type != SharedAttributes.TICKET) {
            // 当读取的csv文件不是Ticket时，直接处理
            attributeMap = read(csvPaths[type], type);
            // 创建数据集
            List<Row> data = new ArrayList<>();
            //筛选出能和机票订单号匹配的订单数据
            // 遍历机票的订单号，只将已经有对应的机票订单号的订单数据添加到data中
            for (Iterator<String> iterator = trainTicketsMap.keySet().iterator(); iterator.hasNext(); ) {
                String key = iterator.next();
                // 得到属性列表
                List<List<String>> attributeLists = attributeMap.get(key);
                // 如果temp不为空，则将temp添加到data中
                // 若是为空则说明没有商品可以和该订单匹配，则跳过
                if (attributeLists != null) {
                    for (List<String> attributeList : attributeLists) {
                        //加入共现的机票订单属性数据
                        attributeList.add(trainTicketsMap.get(key).get(0).get(eva));
                        data.add(RowFactory.create(attributeList));
                    }
                }
            }
            // 创建DataFrame，并指定模式，将List<Row>数据转换为Dataset<Row>
//            logger.info("正在创建DataFrame");
            return getDataFrame(data);
        } else {
            // 当读取的csv文件是Ticket时
            List<Row> data = new ArrayList<>();
            // 暂时不会有等于TICKET的情况，若是以后有，则可以添加在这个地方
            return getDataFrame(data);
        }
    }

    /**
     * Convert a CSV file to a Dataset<Row>.
     */
    public Dataset<Row> singleTypeCsv2dataset(int type) throws IOException {
        //订单与属性之间的映射map
        Map<String, List<List<String>>> attributeMap;
        if (type != SharedAttributes.TICKET) {
            // 当读取的csv文件不是Ticket时，直接处理
            attributeMap = read(csvPaths[type], type);
            // 创建数据集
            List<Row> data = new ArrayList<>();
            //筛选出能和机票订单号匹配的订单数据
            // 遍历机票的订单号，只将已经有对应的机票订单号的订单数据添加到data中
            for (Iterator<String> iterator = trainTicketsMap.keySet().iterator(); iterator.hasNext(); ) {
                String key = iterator.next();
                // 得到属性列表
                List<List<String>> attributeLists = attributeMap.get(key);
                // 如果temp不为空，则将temp添加到data中
                // 若是为空则说明没有商品可以和该订单匹配，则跳过
                if (attributeLists != null) {
                    for (List<String> attributeList : attributeLists) {
                        //加入共现的机票订单属性数据
                        attributeList.addAll(trainTicketsMap.get(key).get(0));
                        data.add(RowFactory.create(attributeList));
                    }
                }
            }
            // 创建DataFrame，并指定模式，将List<Row>数据转换为Dataset<Row>
           // logger.info("正在创建DataFrame");
            return getDataFrame(data);
        } else {
            // 当读取的csv文件是Ticket时
            List<Row> data = new ArrayList<>();
            // 暂时不会有等于TICKET的情况，若是以后有，则可以添加在这个地方
            return getDataFrame(data);
        }
    }

    /**
     * 读取csv文件转换为List<List<String>>主要用于测试api
     * @param type 商品类型
     * @return 属性列表的列表List<List<String>>，其中每个List<String>共同出现在某个订单中的机票和商品属性
     */
    public List<List<String>> singleTypeCsv2ListOfAttributeList(int type) throws IOException {
        List<List<String>> listOfAttributeList = new ArrayList<>();
        //订单与属性之间的映射map
        Map<String, List<List<String>>> attributeMap;
        // 读取商品类型，这里type不能为Ticket，因为一般不需要通过ticket推荐ticket自己
        attributeMap = read(csvPaths[type], type);
        //筛选出能和机票订单号匹配的订单数据
        // 遍历机票的订单号，只将已经有对应的机票订单号的订单数据添加到listOfAttributeList中
        for (Iterator<String> iterator = trainTicketsMap.keySet().iterator(); iterator.hasNext(); ) {
            String key = iterator.next();
            // 得到属性列表
            List<List<String>> attributeLists = attributeMap.get(key);
            // 如果temp不为空，则将temp添加到data中
            // 若是为空则说明没有商品可以和该订单匹配，则跳过
            if (attributeLists != null) {
                for (List<String> attributeList : attributeLists) {
                    //加入共现的机票订单属性数据
                    attributeList.addAll(trainTicketsMap.get(key).get(0));
                    listOfAttributeList.add(attributeList);
                }
            }
        }
        return listOfAttributeList;
    }


    /**
     * 将频繁项集转换为CSV文件
     *
     * @param dataSet 频繁项集
     * @param type    商品类型
     */
    public void freItemSet2CSV(Dataset<Row> dataSet, int type) {
        // 创建 CSV Writer 对象, 参数说明（写入的文件路径，分隔符，编码格式)
        CsvWriter csvWriter = new CsvWriter(resultDirPath + "\\FreqItemSet" + SharedAttributes.FULL_NAMES[type] + ".csv", ',', StandardCharsets.UTF_8);
        try {
            // 定义 header 头
            String[] headers = {"Ticket", SharedAttributes.FULL_NAMES[type], "freq"};
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
            //logger.info("freItemSet2CSV error");
        } finally {
            csvWriter.close();
        }
    }

    /**
     * 将关联规则写入到CSV文件
     *
     * @param rules 关联规则
     * @param type  商品类型
     */
    public void rules2CSV(Dataset<Row> rules, int type) throws IOException {
        // 定义 header 头
        String[] headers = {"TICKET", SharedAttributes.FULL_NAMES[type], "confidence"};
        // 创建 CSV Writer 对象, 参数说明（写入的文件路径，分隔符，编码格式)
        CsvWriter csvWriter = new CsvWriter(resultDirPath + "\\" + "AssociationRules" + SharedAttributes.FULL_NAMES[type] + ".csv", ',', StandardCharsets.UTF_8);
        // 写入 header 头
        csvWriter.writeRecord(headers);
        // 遍历 rules
        for (Row r : rules.collectAsList()) {
            // 获取 Row 的内容
            String[] columns = row2rule(r);
            if (columns.length != 0) {
                //若是不为空则说明有效，写入CSV文件
                csvWriter.writeRecord(columns);
            }
        }
        // 关闭 CSV Writer
        csvWriter.close();
    }

    public Map<String, List<List<String>>> read(int type) throws IOException {
        return read(csvPaths[type], type);
    }


    /**
     * 读取CSV文件
     *
     * @param path 文件路径
     * @param type 商品类型代码
     * @return 返回订单号和订单对应的商品属性之间的键值对 Map<String, List<String>>
     */
    public Map<String, List<List<String>>> read(String path, int type) throws IOException {
        // 创建HashMap，key为订单号，value为订单对应的属性键值对
        HashMap<String, List<List<String>>> map = new HashMap<>();
        // 第一参数：读取文件的路径 第二个参数：分隔符 第三个参数：字符集
        CsvReader csvReader = new CsvReader(path, ',', StandardCharsets.UTF_8);
        // 读取表头
        csvReader.readHeaders();
        //通过type判断调用哪个方法
        //将表头存入AttributeStorage之中，方便后续存入数据库
        SharedAttributes.itemAttributesStorage[type] = new ItemAttributesStorage();
        switch (type) {
            case MEAL:
                while (csvReader.readRecord()) {
                    //订单数量计数
                    orderNumber++;
                    dealM(csvReader, map);
                }
                break;
            case BAGGAGE:
                while (csvReader.readRecord()) {
                    //订单数量计数
                    orderNumber++;
                    dealB(csvReader, map);
                    //dealCommon(csvReader, map, B_SIGN, BAGGAGE_ATTRIBUTES[0]);
                }
                break;
            case HOTEL:
                //处理酒店数据
                while (csvReader.readRecord()) {
                    //订单数量计数
                    orderNumber++;
                    dealH(csvReader, map);
                }
                break;
            case INSURANCE:
                while (csvReader.readRecord()) {
                    //订单数量计数
                    orderNumber++;
                    dealI(csvReader, map);
                }
                break;
            case SEAT:
                //处理座位数据
                while (csvReader.readRecord()) {
                    //订单数量计数
                    orderNumber++;
                    dealS(csvReader, map);
                }
                break;
            case TICKET:
            case TRAIN_TICKET:
                //用于字段作用评估
                while (csvReader.readRecord()) {
                    //订单数量计数
                    orderNumber++;
                    dealT(csvReader, map);
                }
                break;
            case TEST_TICKET:
                //这是用于评估的测试数据类型，实际生产环境不需要 使用这个地方
                //得到对应的属性头实例
                ItemAttributesStorage attributesStorage = SharedAttributes.itemAttributesStorage[type];
                //添加处理后得到的属性头
                attributesStorage.addAttribute("MONTH",0);
                attributesStorage.addAttribute("FROM",1);
                attributesStorage.addAttribute("TO",2);
                attributesStorage.addAttribute("T_GRADE",3);
                attributesStorage.addAttribute("HAVE_CHILD",4);
                attributesStorage.addAttribute("PROMOTION_RATE",5);
                attributesStorage.addAttribute("T_FORMER",6);
                attributesStorage.addAttribute("T_CARRIER",7);
                //读取csv文件时会将一些不需要的属性头删读入，这里需要删除
                //删去多余的属性头

                //用于字段作用评估
                while (csvReader.readRecord()) {
                    //订单数量计数
                    orderNumber++;
                    dealTest(csvReader, map);
                }
                break;
            default:
                break;
        }
        return map;
    }
}
