package bundle_system.io.sql;

import bundle_system.io.*;

import java.io.*;
import java.sql.*;
import java.util.*;

import static bundle_system.io.SharedAttributes.*;

@SuppressWarnings("SqlNoDataSourceInspection")
public class SQLUtils {

    public static final String INSERT_INTO = "INSERT INTO ";
    public static final String END_TIME = "endTime";
    public static final String DID = "did";

    private Connection con;

    public final String[] typeNames = new String[getFullNames().length];//全小写


    /**
     * 参数化构造函数，用于生产环境中的数据库连接。
     * 接受数据库 URL、用户名和密码作为参数，并尝试建立与数据库的连接。
     * 如果连接成功，则调用 createTablesForMemQueryIfNotExist 方法创建必要的表。
     * 初始化 typeNames 数组，将除 HOTEL 类型外的所有类型的名称转换为小写形式。
     *
     * @param url      数据库连接的 URL
     * @param username 连接数据库的用户名
     * @param password 连接数据库的密码
     * @throws ClassNotFoundException 如果找不到 MySQL 驱动类
     * @throws SQLException           如果连接数据库时发生错误
     */
    public SQLUtils(String url, String username, String password) throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        con = DriverManager.getConnection(url, username, password);
        createTablesForMemQueryIfNotExist();
        // 初始化 TypeNames 数组
        for (int i = 0; i < getFullNames().length; ++i) {
            // 酒店由于实际生产中没有使用，所以这里直接跳过
            if (i == HOTEL) continue;
            typeNames[i] = getFullNames()[i].toLowerCase();
        }
    }

    /**
     * 默认构造函数，用于测试目的。
     * 尝试从配置文件 "sql.properties" 中读取数据库连接信息，
     * 并尝试建立与数据库的连接。如果连接失败，则记录一条日志信息。
     * 初始化 typeNames 数组，将所有类型的名称转换为小写形式。
     */
    public SQLUtils() {
        try {
            Properties properties = new Properties();
            properties.load(SQLUtils.class.getClassLoader().getResourceAsStream("sql.properties"));
            // 在配置文件中修改成自己的数据库
            String url = properties.getProperty("url");
            String username = properties.getProperty("username");
            String password = properties.getProperty("password");
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(url, username, password);
            createTablesForMemQueryIfNotExist();
        } catch (IOException | ClassNotFoundException | SQLException ignored) {
            logger.info("数据库连接失败，请不要用默认构造函数，这是为测试用的");
        }
        // 初始化 TypeNames 数组
        for (int i = 0; i < getFullNames().length; i++) {
            typeNames[i] = getFullNames()[i].toLowerCase();
        }
    }


///////////////////////////////////////////////////////////////////////////////////////////
    //训练数据的文件表

    /**
     * 获取训练数据表名称
     *
     * @param type 种类代码
     * @return String 训练数据表名称
     */
    public String getTrainDataTableName(int type) {
        //getFullNames()[type]返回的是种类名称，如"Insurance"
        //由于数据库中都是小写，所以这里需要将种类名称转换为小写
        return "train_data_" + getFullNames()[type].toLowerCase();
    }

    /**
     * 获取训练数据表名称
     *
     * @param typeName 种类代码
     * @return String 训练数据表名称
     */
    public String getTrainDataTableName(String typeName) {
        return "train_data_" + typeName;
    }

    /**
     * 插入训练数据记录到指定类型的数据库表中。
     * 根据传入的文件名、上传时间和类型名称构建 SQL 插入语句，并执行插入操作。
     * 插入成功后，通过文件名和上传时间查询刚刚插入的记录的 ID，并返回格式化的 ID 字符串。
     *
     * @param fileName   要插入的文件名
     * @param uploadTime 文件的上传时间
     * @param typeName   训练数据的类型名称
     * @return 格式化的训练数据 ID 字符串（typeName-id），如果插入失败则返回 null
     * @throws SQLException 如果在执行 SQL 操作时发生错误
     */
    public String insertTrainDataRecord(String fileName, String uploadTime, String typeName) throws SQLException {
        //构建插入语句，插入文件名和上传时间到指定类型的数据库表中
        String sql = INSERT_INTO + getTrainDataTableName(typeName) + "(file_name, upload_time) VALUES (?, ?)";
        // 执行插入操作，这里用的是PreparedStatement来防止SQL注入攻击
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, fileName);
            stmt.setString(2, uploadTime);
            //执行插入操作
            stmt.executeUpdate();
            // 通过文件名和上传时间，查询刚刚插入的记录的id
            String sql2 = "SELECT did FROM " + getTrainDataTableName(typeName) +
                    " WHERE file_name=? AND upload_time=?";
            //查询刚刚插入的记录的id，其实这里可以通过自增id直接查到，但是为了兼容测试的版本，这里暂时还是用文件名和上传时间查询
            try (PreparedStatement stmt2 = con.prepareStatement(sql2)) {
                stmt2.setString(1, fileName);
                stmt2.setString(2, uploadTime);
                try (ResultSet rs = stmt2.executeQuery()) {
                    if (rs.next()) {
                        int id = rs.getInt("did");
                        //typeName-id格式化后的字符串
                        return getTrainDataIdForFrontByIdAndTypeName(id, typeName);
                    } else {
                        //如果查询不到，则返回null
                        return null;
                    }
                }
            }
        }
    }

    /**
     * 根据训练数据的 ID 和类型名称生成格式化的 ID 字符串。
     * 格式化的 ID 字符串由类型名称和 ID 组成，中间用连字符分隔。
     *
     * @param id       训练数据的 ID
     * @param typeName 训练数据的类型名称
     * @return 格式化的训练数据 ID 字符串（typeName-id）
     */
    public String getTrainDataIdForFrontByIdAndTypeName(int id, String typeName) {
        return typeName + "-" + id;
    }


    /**
     * 获取一个表中所有训练数据记录
     */
    public List<TrainDataRecord> getTrainDataRecordsByTypeName(String typeName) throws SQLException {
        String sql = "SELECT * FROM " + getTrainDataTableName(typeName) + " ORDER BY did DESC";
        try (Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            List<TrainDataRecord> trainDataRecords = new ArrayList<>();
            while (rs.next()) {
                TrainDataRecord trainDataRecord = new TrainDataRecord(
                        rs.getInt(DID),
                        rs.getString("file_name"),
                        rs.getString("upload_time"),
                        typeName);
                trainDataRecords.add(trainDataRecord);
            }
            return trainDataRecords;
        }
    }

    /**
     * 根据训练数据id获取训练数据记录
     *
     * @param did      训练数据id（训练数据的编号）
     * @param typeName 种类名称
     * @return TrainDataRecord
     * @throws SQLException SQL异常
     */
    public TrainDataRecord getTrainDataRecordByDid(int did, String typeName) throws SQLException {
        String sql = "SELECT * FROM " + getTrainDataTableName(typeName) + " WHERE did=?";
        try (PreparedStatement pStmt = con.prepareStatement(sql)) {
            pStmt.setInt(1, did);
            try (ResultSet rs = pStmt.executeQuery()) {
                if (rs.next()) {
                    return new TrainDataRecord(
                            rs.getInt(DID),
                            rs.getString("file_name"),
                            rs.getString("upload_time"),
                            typeName);
                }
            }
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
    public List<Map<String, String>> getTrainRecordMaps() throws SQLException {
        try (Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM train_record ORDER BY tid DESC")) {
            List<Map<String, String>> records = new ArrayList<>();
            while (rs.next()) {
                Map<String, String> recordMap = new HashMap<>();
                // 这里用Map暂存是为了符合sonarlint的规则
                int tid = rs.getInt("tid");
                recordMap.put("train_id", Integer.toString(tid));
                trainRecordQueryResToMap(rs, recordMap);
                records.add(recordMap);
            }
            return records;
        }
    }

    /**
     * 根据tid获取训练记录
     *
     * @param tid 训练记录id
     * @return 训练记录的map
     */
    public Map<String, String> getTrainRecordMapByTid(int tid) throws SQLException {
        String sql = "SELECT * FROM train_record WHERE tid=?";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setInt(1, tid);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, String> recordMap = new HashMap<>();
                    // 这里用Map暂存是为了符合sonarlint的规则
                    recordMap.put("train_id", Integer.toString(tid));
                    trainRecordQueryResToMap(rs, recordMap);
                    return recordMap;
                } else {
                    return null;
                }
            }
        }
    }

    /**
     * 根据训练任务 ID 获取训练状态。
     * 通过传入的训练任务 ID 查询数据库中的 train_record 表，
     * 查找对应的 endTime 字段，并根据其值返回相应的训练状态字符串。
     *
     * @param tid 训练任务的 ID
     * @return 训练状态字符串：
     *         - "processing"：如果 endTime 为空或未设置
     *         - "error handling"：如果 endTime 为 "error" 或查询结果不存在
     *         - "processing completed"：如果 endTime 设置且不为 "error"
     * @throws SQLException 如果在执行 SQL 操作时发生错误
     */
    public String getTrainStatusByTid(int tid) throws SQLException {
        String sql = "SELECT endTime FROM train_record WHERE tid=?";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setInt(1, tid);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String endTime = rs.getString(END_TIME);
                    if (endTime == null || endTime.isEmpty()) {
                        return "processing";
                    } else if (endTime.equals("error")) {
                        return "error handling";
                    } else {
                        return "processing completed";
                    }
                } else {
                    // 如果没有找到对应的记录，则认为是错误处理状态
                    return "error handling";
                }
            }
        }
    }


    private void trainRecordQueryResToMap(ResultSet rs, Map<String, String> recordMap) throws SQLException {
        recordMap.put("startTime", rs.getString("startTime"));
        recordMap.put(END_TIME, rs.getString(END_TIME));
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
    public String insertTrainRecord(TrainRecord trainRecord) throws SQLException {
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
                    return generatedKeys.getInt(1) + ""; // 返回生成的 tid的字符串形式
                } else {
                    throw new SQLException("Creating train record failed, no ID obtained.");
                }
            }
        }
    }

    /**
     * 将训练记录写入到数据库中
     *
     * @param startTime     开始时间
     * @param endTime       结束时间
     * @param orderNumber   总订单数量
     * @param comments      评论
     * @param minSupport    最小支持度
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
                    return generatedKeys.getInt(1) + ""; // 返回生成的 tid的字符串形式
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
     *
     * @param tid     训练记录id
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
     * 创建训练记录表。
     * 如果 train_record 表不存在，则创建该表，包含 tid、startTime、endTime、orderNumber、comments、minSupport 和 minConfidence 字段。
     *
     * @throws SQLException 如果在执行 SQL 操作时发生错误
     */
    public void createTrainRecordTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS train_record (" +
                "tid INT AUTO_INCREMENT PRIMARY KEY, " +
                "startTime VARCHAR(64), " +
                "endTime VARCHAR(64), " +
                "orderNumber VARCHAR(64), " +
                "comments VARCHAR(256), " +
                "minSupport VARCHAR(64), " +
                "minConfidence VARCHAR(64) " +
                ")";
        try (Statement stmt = con.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    /**
     * 创建训练数据表。
     * 根据不同的品类（TICKET 到 SEAT），如果相应的表不存在，则创建该表，包含 did、file_name 和 upload_time 字段。
     * 注意：HOTEL 类型的表不会被创建。
     *
     * @throws SQLException 如果在执行 SQL 操作时发生错误
     */
    public void createTrainDataTables() throws SQLException {
        String sql;
        try (Statement stmt = con.createStatement()) {
            for (int i = TICKET; i <= SEAT; i++) {
                if (i == HOTEL) continue;
                sql = "CREATE TABLE IF NOT EXISTS " + getTrainDataTableName(i) + " (" +
                        "did INT AUTO_INCREMENT PRIMARY KEY, " +
                        "file_name VARCHAR(512), " +
                        "upload_time VARCHAR(256) " +
                        ")";
                stmt.executeUpdate(sql);
            }
        }
    }

    /**
     * 创建所有的规则表，包括 MEAL、BAGGAGE、INSURANCE 和 SEAT 几个品类，并创建对应的训练记录表。
     *
     * @throws SQLException 如果在执行 SQL 操作时发生错误
     */
    public void createTablesForMemQueryIfNotExist() throws SQLException {
        try (Statement stmt = con.createStatement()) {
            // 使用 for 循环创建规则表，由于 HOTEL 和 TICKET 的表都不用建，所以从 MEAL 开始创建
            for (int i = MEAL; i <= SEAT; i++) {
                String sql = "CREATE TABLE IF NOT EXISTS " + getRuleTableName(i) + " (" +
                        "rid INT AUTO_INCREMENT PRIMARY KEY, " +
                        "ate VARCHAR(1024), " +
                        "cons VARCHAR(512), " +
                        "conf VARCHAR(256), " +
                        "tid INT" +
                        ")";
                stmt.executeUpdate(sql);
            }
        }
        createTrainRecordTable();
        createTrainDataTables();
    }

    /**
     * 删除所有的表，包括规则表、训练记录表和训练数据表。
     *
     * @throws SQLException 如果在执行 SQL 操作时发生错误
     */
    public void dropTables() throws SQLException {
        Statement stmt = con.createStatement();
        dropRulesTables(stmt);
        dropTrainRecordTable(stmt);
        dropTrainDataTables(stmt);
    }

    /**
     * 删除所有规则表。
     *
     * @param stmt 用于执行 SQL 操作的 Statement 对象
     * @throws SQLException 如果在执行 SQL 操作时发生错误
     */
    public void dropRulesTables(Statement stmt) throws SQLException {
        for (int i = MEAL; i <= SEAT; i++) {
            String sql = "DROP TABLE IF EXISTS " + getRuleTableName(i);
            stmt.executeUpdate(sql);
        }
    }

    /**
     * 删除训练记录表。
     *
     * @param stmt 用于执行 SQL 操作的 Statement 对象
     * @throws SQLException 如果在执行 SQL 操作时发生错误
     */
    public void dropTrainRecordTable(Statement stmt) throws SQLException {
        stmt.executeUpdate("DROP TABLE IF EXISTS train_record");
    }

    /**
     * 删除所有训练数据表。
     *
     * @param stmt 用于执行 SQL 操作的 Statement 对象
     * @throws SQLException 如果在执行 SQL 操作时发生错误
     */
    public void dropTrainDataTables(Statement stmt) throws SQLException {
        for (int i = TICKET; i <= SEAT; i++) {
            String sql = "DROP TABLE IF EXISTS " + getTrainDataTableName(i);
            stmt.executeUpdate(sql);
        }
    }

    /**
     * 重新创建所有的表，包括规则表、训练记录表和训练数据表。
     * 首先删除所有表，然后重新创建它们。
     *
     * @throws SQLException 如果在执行 SQL 操作时发生错误
     */
    public void renewTables() throws SQLException {
        dropTables();
        createTablesForMemQueryIfNotExist();
    }

    /**
     * 重新创建所有规则表。
     * 首先删除所有规则表，然后重新创建它们。
     *
     * @throws SQLException 如果在执行 SQL 操作时发生错误
     */
    public void renewRulesTables() throws SQLException {
        dropRulesTables(con.createStatement());
        createTablesForMemQueryIfNotExist();
    }

///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 将规则存入数据库中的方法。
     * 这里和之前的 associationRulesMining 接口得到的数据格式一样，
     * 可以将 associationRulesMining 训练好的数据直接拿来存入。
     *
     * @param type     品类序号
     * @param itemRule 规则列表，必须包含三个元素：ate, cons, conf
     * @param tid      训练编号
     * @throws IllegalArgumentException 如果 itemRule 为空或不包含三个元素
     * @throws RuntimeException       如果在执行 SQL 操作时发生错误
     */
    private void insertRule(int type, List<String> itemRule, int tid) throws SQLException {
        // 检查 itemRule 是否为空或长度是否不等于3
        if (itemRule == null || itemRule.size() != 3) {
            throw new IllegalArgumentException("itemRule must contain exactly three elements.");
        }
        // 获取对应类型的规则表名
        String tableName = getRuleTableName(type);

        // 构建 SQL 插入语句
        String sql = INSERT_INTO + tableName + "(ate, cons, conf, tid) VALUES (?, ?, ?, ?)";

        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            // 设置 SQL 参数 ate
            pstmt.setString(1, itemRule.get(0));

            // 设置 SQL 参数 cons
            pstmt.setString(2, itemRule.get(1));

            // 设置 SQL 参数 conf
            pstmt.setString(3, itemRule.get(2));

            // 设置 SQL 参数 tid
            pstmt.setInt(4, tid);

            // 执行插入操作
            pstmt.executeUpdate();
        }
    }


    /**
     * 将规则批量存入数据库中的方法，这里和之前的associationRulesMining接口得到的数据格式一样
     * ，可以将associationRulesMining训练好的数据直接拿来存入
     *
     * @param type          品类序号
     * @param itemRulesList 规则列表
     * @param tid           训练编号
     */
    public void insertRules(int type, List<List<String>> itemRulesList, int tid) throws SQLException {
        //因为这里性能完全足够，因此不考虑优化，之后可以使用批量插入进行优化
        for (List<String> itemRule : itemRulesList) {
            insertRule(type, itemRule, tid);
        }
    }

    public void insertRules(int type, List<List<String>> itemRulesList, int tid, int limit) throws SQLException {
        //因为这里性能完全足够，因此不考虑优化，之后可以使用批量插入进行优化
        for (int i = 0; i < limit && i < itemRulesList.size(); i++) {
            insertRule(type, itemRulesList.get(i), tid);
        }
    }

    /**
     * 从关联规则数据库中加载规则。
     * 根据传入的类型和训练任务 ID 查询数据库中的规则表，
     * 并返回包含规则信息的列表。
     *
     * @param type 规则的类型
     * @param tid  训练任务的 ID
     * @return 包含规则信息的二维列表，每个子列表包含三个字符串：ate, cons, conf
     * @throws SQLException 如果在执行 SQL 操作时发生错误
     */
    public List<List<String>> loadRules(int type, int tid) throws SQLException {
        // 构建 SQL 查询语句，根据类型获取相应的规则表名，并按 tid 过滤
        String sql = "SELECT ate, cons, conf FROM " + getRuleTableName(type) + " WHERE tid=?";
        // 创建 PreparedStatement 对象以执行 SQL 查询
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            // 设置查询参数 tid
            ps.setInt(1, tid);
            // 执行查询并获取 ResultSet 结果集
            try (ResultSet rs = ps.executeQuery()) {
                // 创建一个二维列表用于存储查询结果
                List<List<String>> result = new ArrayList<>();
                // 遍历 ResultSet 中的每一行数据
                while (rs.next()) {
                    // 创建一个新的列表来存储当前行的数据
                    List<String> list = new ArrayList<>();
                    // 获取列 "ate" 的值并添加到列表中
                    list.add(rs.getString("ate"));
                    // 获取列 "cons" 的值并添加到列表中
                    list.add(rs.getString("cons"));
                    // 获取列 "conf" 的值并添加到列表中
                    list.add(rs.getString("conf"));
                    // 将当前行的数据列表添加到结果列表中
                    result.add(list);
                }
                // 返回包含所有规则信息的二维列表
                return result;
            }
        }
    }


    /**
     * 统一获得表名的方法
     *
     * @param type 品类序号
     * @return 表名
     */
    private String getRuleTableName(int type) {
        return "rules_" + SharedAttributes.getFullNames()[type].toLowerCase();
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////

}
