package packing_system.io;

import com.csvreader.*;
import org.apache.spark.sql.*;

import java.io.*;
import java.nio.charset.*;
import java.util.*;
import java.util.logging.*;

import static packing_system.data_processer.DataConverter.*;
import static packing_system.data_processer.DataParser.*;
import static packing_system.data_generating_system.FPGrowth.*;
import static packing_system.io.SharedAttributes.*;

public class CSVFileIO {

    //csv文件结果输出的目录路径
    private final String resultDirPath;
    //csv文件读取的路径，数组长度为types.length，数组下标与types对应
    private final String[] csvPaths = new String[SharedAttributes.types.length + 2];

    // 初始化日志记录器
    private static final Logger logger = Logger.getLogger(CSVFileIO.class.getName());

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
            , String pathI, String pathS) throws IOException {
        // 初始化路径
        // 输出结果路径
        this.resultDirPath = resultDirPath;
        // 机票订单相关数据csv文件路径
        csvPaths[SharedAttributes.TICKET] = pathT;
        // 酒店相关数据csv文件路径
        csvPaths[SharedAttributes.HOTEL] = pathH;
        // 餐食相关数据csv文件路径
        csvPaths[SharedAttributes.MEAL] = pathM;
        // 行李相关数据csv文件路径
        csvPaths[SharedAttributes.BAGGAGE] = pathB;
        // 保险相关数据csv文件路径
        csvPaths[SharedAttributes.INSURANCE] = pathI;
        // 座位相关数据csv文件路径
        csvPaths[SharedAttributes.SEAT] = pathS;
        // 初始化类型与索引的映射
        for (int i = 0; i < SharedAttributes.types.length; i++) {
            SharedAttributes.type2index.put(SharedAttributes.types[i], i);
        }
        //首先读取Ticket订单相关信息方便建立订单和属性之间的映射
        SharedAttributes.ticketMap = null;
        //训练用机票订单
        SharedAttributes.testTicketsMap = CSVFileIO.read(PATH_TEST_T, "Test");
        //测试用机票订单
        SharedAttributes.trainTicketsMap = CSVFileIO.read(PATH_TRAIN_T, "Train");
    }

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
            attributeMap = CSVFileIO.read(csvPaths[type], SharedAttributes.types[type]);
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
    public Dataset<Row> singleTypeCsv2dataset(int type, int eva) throws IOException {
        //订单与属性之间的映射map
        Map<String, List<List<String>>> attributeMap;
        if (type != SharedAttributes.TICKET) {
            // 当读取的csv文件不是Ticket时，直接处理
            attributeMap = CSVFileIO.read(csvPaths[type], SharedAttributes.types[type]);
            // 创建数据集
            List<Row> data = new ArrayList<>();
            //筛选出能和机票订单号匹配的订单数据
            // 遍历机票的订单号，只将已经有对应的机票订单号的订单数据添加到data中
            for (Iterator<String> iterator = SharedAttributes.trainTicketsMap.keySet().iterator(); iterator.hasNext(); ) {
                String key = iterator.next();
                // 得到属性列表
                List<List<String>> attributeLists = attributeMap.get(key);
                // 如果temp不为空，则将temp添加到data中
                // 若是为空则说明没有商品可以和该订单匹配，则跳过
                if (attributeLists != null) {
                    for (List<String> attributeList : attributeLists) {
                        //加入共现的机票订单属性数据
                        attributeList.add(SharedAttributes.trainTicketsMap.get(key).get(0).get(eva));
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

    /**
     * Convert a CSV file to a Dataset<Row>.
     */
    public Dataset<Row> singleTypeCsv2dataset(int type) throws IOException {
        //订单与属性之间的映射map
        Map<String, List<List<String>>> attributeMap;
        if (type != SharedAttributes.TICKET) {
            // 当读取的csv文件不是Ticket时，直接处理
            attributeMap = CSVFileIO.read(csvPaths[type], SharedAttributes.types[type]);
            // 创建数据集
            List<Row> data = new ArrayList<>();
            //筛选出能和机票订单号匹配的订单数据
            // 遍历机票的订单号，只将已经有对应的机票订单号的订单数据添加到data中
            for (Iterator<String> iterator = SharedAttributes.trainTicketsMap.keySet().iterator(); iterator.hasNext(); ) {
                String key = iterator.next();
                // 得到属性列表
                List<List<String>> attributeLists = attributeMap.get(key);
                // 如果temp不为空，则将temp添加到data中
                // 若是为空则说明没有商品可以和该订单匹配，则跳过
                if (attributeLists != null) {
                    for (List<String> attributeList : attributeLists) {
                        //加入共现的机票订单属性数据
                        attributeList.addAll(SharedAttributes.trainTicketsMap.get(key).get(0));
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

    /**
     * 读取csv文件转换为List<List<String>>主要用于测试api
     *
     * @param type 商品类型
     * @return 属性列表的列表List<List < String>>，其中每个List<String>共同出现在某个订单中的机票和商品属性
     */
    public List<List<String>> singleTypeCsv2ListOfAttributeList(int type) throws IOException {
        List<List<String>> listOfAttributeList = new ArrayList<>();
        //订单与属性之间的映射map
        Map<String, List<List<String>>> attributeMap;
        // 读取商品类型，这里type不能为Ticket，因为一般不需要通过ticket推荐ticket自己
        attributeMap = CSVFileIO.read(csvPaths[type], SharedAttributes.types[type]);
        //筛选出能和机票订单号匹配的订单数据
        // 遍历机票的订单号，只将已经有对应的机票订单号的订单数据添加到listOfAttributeList中
        for (Iterator<String> iterator = SharedAttributes.trainTicketsMap.keySet().iterator(); iterator.hasNext(); ) {
            String key = iterator.next();
            // 得到属性列表
            List<List<String>> attributeLists = attributeMap.get(key);
            // 如果temp不为空，则将temp添加到data中
            // 若是为空则说明没有商品可以和该订单匹配，则跳过
            if (attributeLists != null) {
                for (List<String> attributeList : attributeLists) {
                    //加入共现的机票订单属性数据
                    attributeList.addAll(SharedAttributes.trainTicketsMap.get(key).get(0));
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
            logger.info("freItemSet2CSV error");
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
        return CSVFileIO.read(csvPaths[type], SharedAttributes.types[type]);
    }

    /**
     * 读取CSV文件
     *
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
        SharedAttributes.itemAttributesStorage[SharedAttributes.type2index.get(type)] = new ItemAttributesStorage();
        //得到对应的属性头类
        ItemAttributesStorage header = SharedAttributes.itemAttributesStorage[SharedAttributes.type2index.get(type)];
        for (int i = 1; i < csvReader.getHeaderCount(); i++) {
            //将表头存入HeaderStorage之中，方便后续存入数据库
            header.addAttribute(csvReader.getHeader(i));
        }
        switch (type) {
            case "M":
                //处理餐食数据
                //删除餐食名字
                header.clear();
                for (String s : MEAL_ATTRIBUTES) {
                    header.addAttribute(s);
                }
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
                header.clear();
                //添加处理后得到的属性头
                for (String s : BAGGAGE_ATTRIBUTES) {
                    header.addAttribute(s);
                }
                while (csvReader.readRecord()) {
                    //订单数量计数
                    orderNumber++;
                    dealB(csvReader, map);
                    //dealCommon(csvReader, map, B_SIGN, BAGGAGE_ATTRIBUTES[0]);
                }
                break;
            case "H":
                header.clear();
                //添加属性名
                for (String s : HOTEL_ATTRIBUTES) {
                    header.addAttribute(s);
                }
                //处理酒店数据
                while (csvReader.readRecord()) {
                    //订单数量计数
                    orderNumber++;
                    dealH(csvReader, map);
                }
                break;
            case "I":
                //处理保险数据
                header.clear();
                for (String s : INSURANCE_ATTRIBUTES) {
                    header.addAttribute(s);
                }
                while (csvReader.readRecord()) {
                    //订单数量计数
                    orderNumber++;
                    dealI(csvReader, map);
                }
                break;
            case "S":
                header.clear();
                for (String s : SEAT_ATTRIBUTES) {
                    header.addAttribute(s);
                }
                //处理座位数据
                while (csvReader.readRecord()) {
                    //订单数量计数
                    orderNumber++;
                    dealS(csvReader, map);
                }
                break;
            case "Train":
                //特殊处理机票数据的属性
                header.clear();
                //添加处理后得到的属性头
                header.addAttribute("MONTH", 0);
                header.addAttribute("FROM", 1);
                header.addAttribute("TO", 2);
                header.addAttribute("T_GRADE", 3);
                header.addAttribute("HAVE_CHILD", 4);
                header.addAttribute("PROMOTION_RATE", 5);
                header.addAttribute("T_FORMER", 6);
                //读取csv文件时会将一些不需要的属性头删读入，这里需要删除
                //删去多余的属性头

                //用于字段作用评估
                while (csvReader.readRecord()) {
                    //订单数量计数
                    orderNumber++;
                    dealE(csvReader, map);
                }
                break;
            case "Test":
                //特殊处理机票数据的属性
                header.clear();
                //添加处理后得到的属性头
                header.addAttribute("MONTH", 0);
                header.addAttribute("FROM", 1);
                header.addAttribute("TO", 2);
                header.addAttribute("T_GRADE", 3);
                header.addAttribute("HAVE_CHILD", 4);
                header.addAttribute("PROMOTION_RATE", 5);
                header.addAttribute("T_FORMER", 6);
                header.addAttribute("T_CARRIER", 7);
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

    //得到训练集
    public static Map<String, List<List<String>>> getTrainingMap() {
        //得到训练集的keys
        Set<String> keys;
        try {
            keys = getTrainKeys();
        } catch (IOException e) {
            logger.info("无机票训练集数据");
            return new HashMap<>();
        }
        //得到训练集
        return getTargetMap(keys);
    }

    //得到测试集
    public static Map<String, List<List<String>>> getTestMap() {
        //得到测试集的keys
        Set<String> keys;
        try {
            keys = getTestKeys();
        } catch (IOException e) {
            logger.info("无机票测试集数据");
            return new HashMap<>();
        }
        //得到测试集
        return getTargetMap(keys);
    }

    //为了将训练集和测试集分开，读取csv中的训练集keys
    public static Set<String> getTrainKeys() throws IOException {
        Set<String> trainNumsSet = getNumsSet(
                "D:\\programms\\java_projects\\version_control\\测试数据2.0\\train_dataset.txt");
        return getKeys(trainNumsSet);
    }

    public static Set<String> getTestKeys() throws IOException {
        Set<String> testNumsSet = getNumsSet(
                "D:\\programms\\java_projects\\version_control\\测试数据2.0\\test_dataset.txt");
        return getKeys(testNumsSet);
    }

    //得到机票唯一标识符key的集合keys
    public static Set<String> getKeys(Set<String> numsSet) throws IOException {
        Set<String> keys = new HashSet<>();
        String csvFilePath = "D:\\programms\\java_projects\\version_control\\测试数据2.0\\ticket.csv";
        try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!numsSet.contains(line.split(",")[0])) {
                    continue;
                }
                int commaIndex = line.indexOf(',');
                String result = line.substring(commaIndex + 1) + ",";
                keys.add(result);
            }
        }
        return keys;
    }

    //从一个给定的txt文件中得到csv文件中所有订单号（训练集或测试集）
    public static Set<String> getNumsSet(String filePath) throws IOException {
        Set<String> nums = new HashSet<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                nums.add(line);
            }
        }
        return nums;
    }

    //得到结果集合（是上面两个方法直接套用的方法）
    public static Map<String, List<List<String>>> getTargetMap(Set<String> keys) {
        //返回的结果的map
        Map<String, List<List<String>>> targetMap = new HashMap<>();
        //遍历ticketMap
        for (Map.Entry<String, List<List<String>>> entry : SharedAttributes.ticketMap.entrySet()) {
            //得到key
            String key = entry.getKey();
            //得到ticketMap中属性列表的列表
            List<List<String>> listOfAttributeList = entry.getValue();
            for (List<String> attributeList : listOfAttributeList) {
                // 得到机票的key（机票唯一标识符）
                String itemKey = generateItemKeyFromAttributes(attributeList);
                // 如果机票的key在keys中，则将这个属性列表加入到targetMap的对应的属性列表的列表中
                if (keys.contains(itemKey)) {
                    // 如果targetMap中没有这个key，则新建一个属性列表的列表,否则直接获取这个key对应的属性列表的列表
                    List<List<String>> listOfAttributeListInDataSet = targetMap.getOrDefault(key, new ArrayList<>());
                    // 将这个属性列表加入到属性列表的列表中
                    listOfAttributeListInDataSet.add(attributeList);
                    // 将这个属性列表的列表加入或更新到targetMap中
                    targetMap.put(key, listOfAttributeListInDataSet);
                }
            }
        }
        return targetMap;
    }

    public static String generateItemKeyFromAttributes(List<String> attributes) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < attributes.size() - 1; i++) {
            stringBuilder.append(attributes.get(i).split(":")[2]).append(",");
        }
        return stringBuilder.toString();
    }

    public static void initializeItemCode() {

        for (int i = 1; i < 6; i++) {
            //读取csv文件
            try {
                CsvReader csvReader = new CsvReader(
                        "C:\\Users\\mille\\Desktop\\同步\\民航项目文档类\\数据集v2\\" + FULL_NAMES[i] + ".csv", ',', StandardCharsets.UTF_8);

                csvReader.readHeaders();
                Set<String> itemCodes = ITEM_CODE_SETS.get(i);
                while (csvReader.readRecord()) {
                    itemCodes.add(csvReader.get("ITEM_ID"));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
