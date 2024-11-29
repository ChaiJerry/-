package bundle_system.io.sql;

import bundle_system.io.*;

import java.io.*;
import java.sql.*;
import java.util.*;

import static bundle_system.io.SharedAttributes.*;

public class SQLUtils {

    private static String url;//在配置文件中修改成自己的数据库
    private static String username;
    private static String password;

    private static final String[] ateNames = {"MONTH", "FROM", "TO", "T_GRADE", "HAVE_CHILD", "PROMOTION_RATE", "T_FORMER"};

    public static Connection getCon() {
        return con;
    }

    public static Connection con;

    //将db.properties文件读取出来
    static {
        try {
            Properties properties = new Properties();
            properties.load(SQLUtils.class.getClassLoader().getResourceAsStream("sql.properties"));
            url = properties.getProperty("url");
            username = properties.getProperty("username");
            password = properties.getProperty("password");
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            con = DriverManager.getConnection(url, username, password);
            createTablesForMemQueryIfNotExist();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


       /*
    训练记录字段示例
        {
            "train_id": "train-1",
            "startTime": "2024-08-17 15:10:50",
            "endTime": "2024-08-17 15:11:14",
            "orderNumber": "1000",
            "comments": "null",
            "minSupport": "0.09000000357627869",
            "minConfidence": "0.800000011920929"
         }
     */

    /**
     * 创建训练记录表
     */
    public static void createTrainRecordTable() throws SQLException {
        Statement stmt = con.createStatement();
        String sql = "CREATE TABLE IF NOT EXISTS train_record (" +
                "rid INT AUTO_INCREMENT PRIMARY KEY, " +
                "train_id VARCHAR(128), " +
                "startTime VARCHAR(50), " +
                "endTime VARCHAR(50), " +
                "orderNumber INT, " +
                "comments VARCHAR(100), " +
                "minSupport DOUBLE, " +
                "minConfidence DOUBLE "
                + ")";
        stmt.executeUpdate(sql);
    }

    public static String getNextTrainId() {
        List<TrainRecord> trainRecords = TrainRecord.sortByTrainId(loadTrainRecords());
        if (trainRecords.isEmpty()) {
            return "train-0";
        }
        return trainRecords.get(0).getNextTrainId();
    }

    /**
     * 从数据库中读取所有的训练记录，并返回一个List<Map<String, String>>
     *     其中的记录按照"train-1"的"train-"后面的数字的顺序降序排列
     * @return 排好序的训练记录
     */
    public static List<TrainRecord> loadTrainRecords(){
        try {
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM train_record ORDER BY CAST(SUBSTRING(train_id, 6) AS UNSIGNED INTEGER) DESC");
            List<TrainRecord> records = new ArrayList<>();
            while (rs.next()) {
                Map<String, String> recordMap = new HashMap<>();
                //这里用Map暂存是为了符合sonarlint的规则
                recordMap.put("train_id", rs.getString("train_id"));
                recordMap.put("startTime", rs.getString("startTime"));
                recordMap.put("endTime", rs.getString("endTime"));
                recordMap.put("orderNumber", rs.getString("orderNumber"));
                recordMap.put("comments", rs.getString("comments"));
                recordMap.put("minSupport", rs.getString("minSupport"));
                recordMap.put("minConfidence", rs.getString("minConfidence"));
                TrainRecord record = new TrainRecord(recordMap);
                records.add(record);
            }
            return records;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * 将训练记录写入到数据库中
     *
     * @param trainRecord 一条训练记录，包含了训练id，开始时间，结束时间，订单数量，评论，最小支持度和最小置信度
     */
    public static void writeTrainRecordToDB(Map<String, Object> trainRecord) {
        Statement stmt;
        try {
            stmt = con.createStatement();
            String sql = "INSERT INTO train_record(train_id, startTime, endTime, orderNumber, comments, minSupport, minConfidence) VALUES ('" + trainRecord.get("train_id") +
                    "', '" + trainRecord.get("startTime") + "', '"
                    + trainRecord.get("endTime") + "', '"
                    + trainRecord.get("orderNumber") + "', '"
                    + trainRecord.get("comments") + "', '"
                    + trainRecord.get("minSupport") + "', '"
                    + trainRecord.get("minConfidence") + "')";
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void createTable() throws SQLException {
        Statement stmt = con.createStatement();
        String sql = "CREATE TABLE IF NOT EXISTS rules (" +
                "rid INT AUTO_INCREMENT PRIMARY KEY, " +
                "a_MONTH VARCHAR(10), " +
                "a_FROM VARCHAR(10), " +
                "a_TO VARCHAR(10), " +
                "a_T_GRADE VARCHAR(10), " +
                "a_HAVE_CHILD VARCHAR(10), " +
                "a_PROMOTION_RATE VARCHAR(10), " +
                "a_T_FORMER VARCHAR(10), " +
                "consequence VARCHAR(30), " +
                "confidence DOUBLE, " +
                "train_number INT "
                + ")";
        stmt.executeUpdate(sql);
    }

    /**
     * 创建所有的规则表，包括了MEAL，BAGGAGE,INSURANCE,SEAT几个品类
     * ，同时也创建了对应的训练记录表
     */
    public static void createTablesForMemQueryIfNotExist() throws SQLException {
        Statement stmt = con.createStatement();
        //用for循环创建表，由于hotel个ticket的表都不用建，所以从MEAL开始创建
        for (int i = MEAL; i <= SEAT; i++) {
            String sql = "CREATE TABLE IF NOT EXISTS " + getRuleTableName(i) + " (" +
                    "rid INT AUTO_INCREMENT PRIMARY KEY, " +
                    "ate VARCHAR(1024), " +
                    "cons VARCHAR(512), " +
                    "conf VARCHAR(256), " +
                    "train_id VARCHAR(128)"
                    + ")";
            stmt.executeUpdate(sql);
        }
        createTrainRecordTable();
    }

    public static void dropRuleTables() throws SQLException {
        Statement stmt = con.createStatement();
        for (int i = MEAL; i <= SEAT; i++) {
            String sql = "DROP TABLE IF EXISTS " + getRuleTableName(i);
            stmt.executeUpdate(sql);
        }
    }

    /**
     * 将规则存入数据库中的方法，这里和之前的associationRulesMining接口得到的数据格式一样
     * ，可以将associationRulesMining训练好的数据直接拿来存入
     *
     * @param type     品类序号
     * @param itemRule 规则
     * @param train_id 训练编号
     */
    private static void storeRule(int type, List<String> itemRule, String train_id) {
        Statement stmt;
        try {
            stmt = con.createStatement();

            String sql = "INSERT INTO " + getRuleTableName(type) + "(ate, cons, conf, train_id) VALUES ('" + itemRule.get(0) +
                    "', '" + itemRule.get(1) + "', '"
                    + itemRule.get(2) + "', '" + train_id + "')";
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 将规则批量存入数据库中的方法，这里和之前的associationRulesMining接口得到的数据格式一样
     * ，可以将associationRulesMining训练好的数据直接拿来存入
     *
     * @param type          品类序号
     * @param itemRulesList 规则列表
     * @param train_id      训练编号
     */
    public static void storeRules(int type, List<List<String>> itemRulesList, String train_id) {
        //因为这里性能完全足够，因此不考虑优化，之后可以使用批量插入进行优化
        for (List<String> itemRule : itemRulesList) {
            storeRule(type, itemRule, train_id);
        }
    }

    /**
     * 根据训练编号和品类加载出对应的规则
     *
     * @param type    品类序号
     * @param trainId 训练编号
     * @return 规则列表
     */
    public static List<List<String>> loadRules(int type, String trainId) {
        try {
            PreparedStatement ps = con.prepareStatement("select ate, cons, conf from " + getRuleTableName(type) + " where train_id=?");
            ps.setString(1, trainId);
            ResultSet rs = ps.executeQuery();
            List<List<String>> result = new ArrayList<>();
            while (rs.next()) {
                List<String> list = new ArrayList<>();
                list.add(rs.getString("ate"));
                list.add(rs.getString("cons"));
                list.add(rs.getString("conf"));
                result.add(list);
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }



    /**
     * 统一获得表名的方法
     *
     * @param type 品类序号
     * @return 表名
     */
    private static String getRuleTableName(int type) {
        return SharedAttributes.getFullNames()[type] + "_rules";
    }

    private static String getConditionCount(List<String> ates) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ates.size() - 1; i++) {
            sb.append("(a_").append(ateNames[i]).append(" = ").append("'").append(ates.get(i)).append("')").append("+");
        }
        sb.append("(a_").append(ateNames[ates.size() - 1]).append(" = ").append("'").append(ates.get(ates.size() - 1)).append("')");
        return sb.toString();
    }

    public static AssociationRuleConsResult searchRules(List<String> ates, int trainNumber) throws SQLException {
        Statement stmt = con.createStatement();
        String sql = "WITH FilteredRules AS (  " +
                "    SELECT " +
                "        consequence," +
                "        confidence," +
                "        train_number," +
                "        (  " +//计算满足条件的数量
                getConditionCount(ates) +
                "        ) AS condition_count " +
                "    FROM employees " +
                ") " +
                "SELECT *  " +
                "FROM FilteredRules  " +
                "ORDER BY condition_count DESC  " +
                "LIMIT 1;";//按满足条件的数量降序排序，并限制返回记录数量
        ResultSet rs = stmt.executeQuery(sql);
        while (rs.next()) {
            System.out.println(rs);
        }
        return null;
    }

    public static void test() throws SQLException {
        System.out.println("initialize");

        System.out.println("Database connected");

        //使用示例

        Statement stmt = con.createStatement();

        String sql = "CREATE TABLE IF NOT EXISTS students (" +
                "Sid INT AUTO_INCREMENT PRIMARY KEY, " +
                "Sname VARCHAR(50) NOT NULL, " +
                "Sgander VARCHAR(50) NOT NULL, " +
                "Sage INT NOT NULL)";
        stmt.executeUpdate(sql);
        sql = "INSERT INTO students (Sid,Sname,Sgander,Sage) VALUES (?,?,?,?)";
        PreparedStatement pStmt = con.prepareStatement(sql);
        pStmt.setInt(1, 22371285);
        pStmt.setString(2, "Yuntao Liu");
        pStmt.setString(3, "man");
        pStmt.setInt(4, 20);
        pStmt.executeUpdate();

        sql = "SELECT * FROM students";
        ResultSet rs = stmt.executeQuery(sql);
        while (rs.next()) {
            int id = rs.getInt("Sid");
            String name = rs.getString("Sname");
            String sgander = rs.getString("Sgander");
            int age = rs.getInt("Sage");
            System.out.println(id + " " + name + " " + sgander + " " + age);
        }
        rs.close();
        pStmt.close();
        stmt.close();

        con.close();
    }

}
