package bundle_system.io.sql;

import bundle_system.io.*;

import java.io.*;
import java.sql.*;
import java.util.*;

import static bundle_system.io.SharedAttributes.*;

public class SQLUtils {

    private  String url ="";//在配置文件中修改成自己的数据库
    private String username = "";
    private String password = "";

    private Connection con;

    public final String[] typeNames = new String[getFullNames().length];//全小写

    //将db.properties文件读取出来
    public SQLUtils() {
        try {
            Properties properties = new Properties();
            properties.load(SQLUtils.class.getClassLoader().getResourceAsStream("sql.properties"));
//            url = properties.getProperty("url");
//            username = properties.getProperty("username");
//            password = properties.getProperty("password");
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
        //初始化TypeNames数组
        for(int i = 0; i < getFullNames().length; i++) {
            typeNames[i] = getFullNames()[i].toLowerCase();
        }
    }

    public SQLUtils(String url, String username, String password) {
        try {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            con = DriverManager.getConnection(url, username, password);
            createTablesForMemQueryIfNotExist();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        //初始化TypeNames数组
        for(int i = 0; i < getFullNames().length; ++i) {
            //酒店由于实际生产中没有使用，所以这里直接跳过
            if(i==HOTEL) continue;
            typeNames[i] = getFullNames()[i].toLowerCase();
        }
    }

///////////////////////////////////////////////////////////////////////////////////////////
    //训练数据的文件表

    /**
     * 获取训练数据表名称
     * @param type 种类代码
     * @return String 训练数据表名称
     */
    public String getTrainDataTableName(int type){
        //getFullNames()[type]返回的是种类名称，如"Insurance"
        //由于数据库中都是小写，所以这里需要将种类名称转换为小写
        return "train_data_" + getFullNames()[type].toLowerCase();
    }
    /**
     * 获取训练数据表名称
     * @param typeName 种类代码
     * @return String 训练数据表名称
     */
    public String getTrainDataTableName(String typeName){
        return "train_data_" + typeName;
    }

    //插入训练数据记录
    public String insertTrainDataRecord(String fileName, String uploadTime, String typeName) throws SQLException {
        String sql = "INSERT INTO " + getTrainDataTableName(typeName) + "(file_name, upload_time) VALUES (?, ?)";
        PreparedStatement stmt = con.prepareStatement(sql);
        stmt.setString(1, fileName);
        stmt.setString(2, uploadTime);
        stmt.executeUpdate();
        //通过文件名和上传时间，查询刚刚插入的记录的id
        //不使用查找最大id的原因是如果有两个文件同一时间上传，那么可能会查到错误的id
        String sql2 = "SELECT did FROM " + getTrainDataTableName(typeName) +
                " WHERE file_name=? AND upload_time=?";
        PreparedStatement stmt2 = con.prepareStatement(sql2);
        stmt2.setString(1, fileName);
        stmt2.setString(2, uploadTime);
        ResultSet rs = stmt2.executeQuery();
        if (rs.next()) {
            int id = rs.getInt("did");
            return getTrainDataIdForFrontByIdAndTypeName(id,typeName);
        } else {
            return null;
        }
    }

    public String getTrainDataIdForFrontByIdAndTypeName(int id,String typeName){
        return typeName+ "-" + id;
    }

    //插入训练数据记录SQ
    public void insertTrainDataRecord(String fileName, String uploadTime, int type) throws SQLException {
        String sql = "INSERT INTO "+ getTrainDataTableName(type) +
                "(file_name, upload_time) VALUES (?, ?)";
        PreparedStatement stmt = con.prepareStatement(sql);
        stmt.setString(1, fileName);
        stmt.setString(2, uploadTime);
        stmt.executeUpdate();
    }

    /**
     * 获取一个表中所有训练数据记录
     */
    public List<TrainDataRecord> getTrainDataRecordsByTypeName(String typeName) throws SQLException {
        Statement stmt = con.createStatement();
        //得到按did降序排列的所有记录
        String sql = "SELECT * FROM " + getTrainDataTableName(typeName) + " ORDER BY did DESC";
        ResultSet rs = stmt.executeQuery(sql);
        List<TrainDataRecord> trainDataRecords = new ArrayList<>();
        while (rs.next()) {
            TrainDataRecord record = new TrainDataRecord(
                    rs.getInt( "did"), rs.getString("file_name")
                    , rs.getString("upload_time"),typeName);
            trainDataRecords.add(record);
        }
        return trainDataRecords;
    }

    /**
     * 根据训练数据id获取训练数据记录
     * @param did 训练数据id（训练数据的编号）
     * @param typeName 种类名称
     * @return TrainDataRecord
     * @throws SQLException SQL异常
     */
    public TrainDataRecord getTrainDataRecordByDid(int did,String typeName) throws SQLException {
        String sql = "SELECT * FROM " + getTrainDataTableName(typeName) + " WHERE did=?";
        PreparedStatement pStmt = con.prepareStatement(sql);
        pStmt.setInt(1, did);
        ResultSet rs = pStmt.executeQuery();
        if (rs.next()) {
            return new TrainDataRecord(
                    rs.getInt("did"), rs.getString("file_name")
                    , rs.getString("upload_time"), typeName);
        }
        return null;
    }


////////////////////////////////////////////////////////////////////////////////////////////
//训练记录表操作




    /**
     * 从数据库中读取所有的训练记录，并返回一个List<Map<String, String>>
     * 其中的记录按照tid降序排列
     *
     * @return 排好序的训练记录
     */
    public List<Map<String, String>> getTrainRecordMaps() {
        try {
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM train_record ORDER BY tid DESC");
            List<Map<String, String>> records = new ArrayList<>();
            while (rs.next()) {
                Map<String, String> recordMap = new HashMap<>();
                //这里用Map暂存是为了符合sonarlint的规则
                int tid = rs.getInt("tid");
                recordMap.put("train_id", tid + "");
                trainRecordQueryResToMap(rs, recordMap);
                records.add(recordMap);
            }
            return records;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据tid获取训练记录
     * @param tid 训练记录id
     * @return 训练记录的map
     */
    public Map<String, String> getTrainRecordMapByTid(int tid) throws SQLException {
        String sql = "SELECT * FROM train_record WHERE tid=?";
        PreparedStatement pstmt = con.prepareStatement(sql);
        pstmt.setInt(1, tid);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
            Map<String, String> recordMap = new HashMap<>();
            //这里用Map暂存是为了符合sonarlint的规则
            recordMap.put("train_id", tid + "");
            trainRecordQueryResToMap(rs, recordMap);
            return recordMap;
        } else {
            return null;
        }
    }

    public String getTrainStatusByTid(int tid) throws SQLException {
        String sql = "SELECT endTime FROM train_record WHERE tid=?";
        PreparedStatement pstmt = con.prepareStatement(sql);
        pstmt.setInt(1, tid);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
            String endTime = rs.getString("endTime");
            if (endTime == null || endTime.isEmpty()) {
                return "processing";
            }else if (endTime.equals("error")){
                return "error handling";
            }else{
                return "processing completed";
            }
        } else {
            return "error handling";
        }
    }

    private void trainRecordQueryResToMap(ResultSet rs, Map<String, String> recordMap) throws SQLException {
        recordMap.put("startTime", rs.getString("startTime"));
        recordMap.put("endTime", rs.getString("endTime"));
        recordMap.put("orderNumber", rs.getString("orderNumber"));
        recordMap.put("comments", rs.getString("comments"));
        recordMap.put("minSupport", rs.getString("minSupport"));
        recordMap.put("minConfidence", rs.getString("minConfidence"));
    }

    /**
     * 将训练记录写入到数据库中
     *
     * @param trainRecord 一条训练记录，包含开始时间，结束时间，订单数量，评论，最小支持度和最小置信度
     */
    public String insertTrainRecord(TrainRecord trainRecord) {
        // 准备插入语句
        String sql = "INSERT INTO train_record(startTime, endTime, orderNumber, comments, minSupport, minConfidence) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, trainRecord.getStartTime());
            pstmt.setString(2, trainRecord.getEndTime());
            pstmt.setString(3, trainRecord.getOrderNumber());
            pstmt.setString(4, trainRecord.getComments());
            pstmt.setString(5, trainRecord.getMinSupport());
            pstmt.setString(6, trainRecord.getMinConfidence());

            // 执行插入操作
            int affectedRows = pstmt.executeUpdate();

            // 检查受影响的行数
            if (affectedRows == 0) {
                throw new SQLException("Creating train record failed, no rows affected.");
            }

            // 获取生成的键
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1)+""; // 返回生成的 tid的字符串形式
                } else {
                    throw new SQLException("Creating train record failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 将训练记录写入到数据库中
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param orderNumber 总订单数量
     * @param comments 评论
     * @param minSupport 最小支持度
     * @param minConfidence 最小置信度
     * @return tid的字符串形式
     */
    public String insertTrainRecord(String startTime, String endTime
            , String orderNumber, String comments
            , String minSupport, String minConfidence) {

        // 准备插入语句
        String sql = "INSERT INTO train_record(startTime, endTime, orderNumber, comments, minSupport, minConfidence) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, startTime);
            pstmt.setString(2, endTime);
            pstmt.setString(3, orderNumber);
            pstmt.setString(4, comments);
            pstmt.setString(5, minSupport);
            pstmt.setString(6, minConfidence);

            // 执行插入操作
            int affectedRows = pstmt.executeUpdate();

            // 检查受影响的行数
            if (affectedRows == 0) {
                throw new SQLException("Creating train record failed, no rows affected.");
            }

            // 获取生成的键
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1)+""; // 返回生成的 tid的字符串形式
                } else {
                    throw new SQLException("Creating train record failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 更新训练记录的结束时间，这里利用训练结束时间也顺便记录了训练状态，
     * 当训练记录正常结束时，结束时间会被更新为当前时间，而当训练异常结束时，写入"error"，当没有结束时，结束时间会为null
     * @param tid 训练记录id
     * @param endTime 结束时间
     * @throws SQLException SQL异常
     */
    public void updateTrainRecordEndTime(int tid, String endTime) throws SQLException {
        String sql = "UPDATE train_record SET endTime = ? WHERE tid = ?";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, endTime);
            pstmt.setInt(2, tid);
            pstmt.executeUpdate();
        }
    }

    public void upDateTrainRecordOrderNumber(int tid, String orderNumber) throws SQLException {
        String sql = "UPDATE train_record SET orderNumber = ? WHERE tid = ?";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, orderNumber);
            pstmt.setInt(2, tid);
            pstmt.executeUpdate();
        }
    }



///////////////////////////////////////////////////////////////////////////////////////////////////
//建表操作和删除表操作

    /**
     * 创建训练记录表
     */
    public void createTrainRecordTable() throws SQLException {
        Statement stmt = con.createStatement();
        String sql = "CREATE TABLE IF NOT EXISTS train_record (" +
                "tid INT AUTO_INCREMENT PRIMARY KEY, " +
                "startTime VARCHAR(64), " +
                "endTime VARCHAR(64), " +
                "orderNumber VARCHAR(64), " +
                "comments VARCHAR(256), " +
                "minSupport VARCHAR(64), " +
                "minConfidence VARCHAR(64) "
                + ")";
        stmt.executeUpdate(sql);
    }

    /**
     * 创建训练数据表
     */
    public void createTrainDataTables() throws SQLException {
        Statement stmt = con.createStatement();
        String sql;
        for (int i = TICKET; i <= SEAT; i++) {
            if(i==HOTEL) continue;
            sql = "CREATE TABLE IF NOT EXISTS "+getTrainDataTableName(i) + " (" +
                    "did INT AUTO_INCREMENT PRIMARY KEY, " +
                    "file_name VARCHAR(512), " +
                    "upload_time VARCHAR(256) "
                    + ")";
            stmt.executeUpdate(sql);
        }
    }
    /**
     * 创建所有的规则表，包括了MEAL，BAGGAGE,INSURANCE,SEAT几个品类
     * ，同时也创建了对应的训练记录表
     */
    public void createTablesForMemQueryIfNotExist() throws SQLException {
        Statement stmt = con.createStatement();
        //用for循环创建表，由于hotel个ticket的表都不用建，所以从MEAL开始创建
        for (int i = MEAL; i <= SEAT; i++) {
            String sql = "CREATE TABLE IF NOT EXISTS " + getRuleTableName(i) + " (" +
                    "rid INT AUTO_INCREMENT PRIMARY KEY, " +
                    "ate VARCHAR(1024), " +
                    "cons VARCHAR(512), " +
                    "conf VARCHAR(256), " +
                    "tid INT"
                    + ")";
            stmt.executeUpdate(sql);
        }
        createTrainRecordTable();
        createTrainDataTables();
    }

    public void dropTables() throws SQLException {
        Statement stmt = con.createStatement();
        dropRulesTables(stmt);
        dropTrainRecordTable(stmt);
        dropTrainDataTables(stmt);
    }

    public void dropRulesTables(Statement stmt) throws SQLException {
        for (int i = MEAL; i <= SEAT; i++) {
            String sql = "DROP TABLE IF EXISTS " + getRuleTableName(i);
            stmt.executeUpdate(sql);
        }
    }

    public void dropTrainRecordTable(Statement stmt) throws SQLException {
        stmt.executeUpdate("DROP TABLE IF EXISTS train_record");
    }

    public void dropTrainDataTables(Statement stmt) throws SQLException {
        for (int i = TICKET; i <= SEAT; i++) {
            String sql = "DROP TABLE IF EXISTS " + getTrainDataTableName(i);
            stmt.executeUpdate(sql);
        }
    }

    public void renewTables() throws SQLException {
        dropTables();
        createTablesForMemQueryIfNotExist();
    }
///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 将规则存入数据库中的方法，这里和之前的associationRulesMining接口得到的数据格式一样
     * ，可以将associationRulesMining训练好的数据直接拿来存入
     *
     * @param type     品类序号
     * @param itemRule 规则
     * @param tid 训练编号
     */
    private void insertRule(int type, List<String> itemRule, int tid) {
        if (itemRule == null || itemRule.size() != 3) {
            throw new IllegalArgumentException("itemRule must contain exactly three elements.");
        }

        String tableName = getRuleTableName(type);
        String sql = "INSERT INTO " + tableName + "(ate, cons, conf, tid) VALUES (?, ?, ?, ?)";

        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, itemRule.get(0));
            pstmt.setString(2, itemRule.get(1));
            pstmt.setString(3, itemRule.get(2));
            pstmt.setInt(4, tid);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to store rule", e);
        }
    }

    /**
     * 将规则批量存入数据库中的方法，这里和之前的associationRulesMining接口得到的数据格式一样
     * ，可以将associationRulesMining训练好的数据直接拿来存入
     *
     * @param type          品类序号
     * @param itemRulesList 规则列表
     * @param tid      训练编号
     */
    public void insertRules(int type, List<List<String>> itemRulesList, int tid) {
        //因为这里性能完全足够，因此不考虑优化，之后可以使用批量插入进行优化
        for (List<String> itemRule : itemRulesList) {
            insertRule(type, itemRule, tid);
        }
    }

    public void insertRules(int type, List<List<String>> itemRulesList, int tid, int limit) {
        //因为这里性能完全足够，因此不考虑优化，之后可以使用批量插入进行优化
        for(int i=0;i<limit&&i<itemRulesList.size();i++) {
            insertRule(type, itemRulesList.get(i), tid);
        }
    }

    /**
     * 根据训练编号和品类加载出对应的规则
     *
     * @param type    品类序号
     * @param tid 训练编号
     * @return 规则列表
     */
    public List<List<String>> loadRules(int type, int tid) {
        try {
            PreparedStatement ps = con.prepareStatement("select ate, cons, conf from " + getRuleTableName(type) + " where tid=?");
            ps.setInt(1, tid);
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
    private String getRuleTableName(int type) {
        return  "rules_"+SharedAttributes.getFullNames()[type].toLowerCase();
    }


    //////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 测试方法，这个方法用于测试数据库连接是否正常，它会尝试创建一个表并插入一条记录，然后查询该表并打印结果。
     */
    public void test() throws SQLException {
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
