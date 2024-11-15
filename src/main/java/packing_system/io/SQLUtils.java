package packing_system.io;

import java.io.*;
import java.sql.*;
import java.util.*;

public class SQLUtils {

        private static String url;//在配置文件中修改成自己的数据库
        private static String username;
        private static String password;

        private static final String[] ateNames = {"MONTH","FROM","TO","T_GRADE","HAVE_CHILD","PROMOTION_RATE","T_FORMER"};

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
                }
                catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                con = DriverManager.getConnection(url,username,password);
            } catch (IOException e) {
                e.printStackTrace();
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
                    "confidence DOUBLE, "+
                    "train_number INT "
                    +")";
            stmt.executeUpdate(sql);
        }

        private static String getConditionCount(List<String> ates) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < ates.size()-1; i++) {
                sb.append("(a_").append(ateNames[i]).append(" = ").append("'").append(ates.get(i)).append("')").append("+");
            }
            sb.append("(a_").append(ateNames[ates.size()-1]).append(" = ").append("'").append(ates.get(ates.size()-1)).append("')");
            return sb.toString();
        }

        public static AssociationRuleResult searchRules(List<String> ates, int trainNumber) throws SQLException {
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
