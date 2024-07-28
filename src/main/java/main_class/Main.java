package main_class;

import io.*;

import java.io.*;
import java.util.*;

import static io.IOMonitor.*;
import static io.MongoUtils.*;
import static query_system.QuerySystem.*;

public class Main {
    public static void main(String[] args) throws IOException {
        List<String> strings = queryTest2();
        // 指定要写入的文件路径
        String filePath = "C:\\Users\\mille\\Desktop\\output.txt";

        // 使用try-with-resources语句来自动关闭资源
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            // 遍历List中的每个字符串
            for (String str : strings) {
                // 将字符串写入文件，并在每个字符串后添加换行符
                writer.write(str + System.lineSeparator());
            }

            // 实际上，在try-with-resources中，当退出try块时，writer会自动被关闭和刷新
            // 但为了明确性，这里也可以显式调用flush方法（虽然不是必需的）
            writer.flush();

            System.out.println("Successfully wrote strings to the file.");
        } catch (IOException e) {
            // 处理异常
            e.printStackTrace();
        }
    }
}
