package com.bundling.controllers;

import bundle_system.io.*;
import bundle_system.io.sql.*;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.*;

import static bundle_system.io.SharedAttributes.*;
import static com.bundling.Application.*;

/**
 * 和训练数据有关的接口
 */
@RestController
@RequestMapping("/api/data")
public class DataController {
    protected static final String[] typeNamesInLowerCase = {
            SharedAttributes.getFullNames()[TICKET].toLowerCase(),
            SharedAttributes.getFullNames()[MEAL].toLowerCase(),
            SharedAttributes.getFullNames()[BAGGAGE].toLowerCase(),
            SharedAttributes.getFullNames()[INSURANCE].toLowerCase(),
            SharedAttributes.getFullNames()[SEAT].toLowerCase()};

        /**
         * 上传数据文件
         *
         * @param fileName 文件名称
         * @param file     上传的文件
         * @param type     文件类型
         * @return ResponseEntity 包含上传结果的响应实体
         * 该方法用于处理文件上传请求，包括文件验证、文件存储和数据记录插入。
         * 如果文件为空，将返回400 Bad Request响应，提示“文件为空”。
         * 如果文件类型无效，将返回400 Bad Request响应，提示“无效的类型。必须是以下类型之一：[ticket, meal, baggage, insurance, seat]”。
         * 文件将保存在项目的“uploads”目录下，按文件类型和上传时间分文件夹存储。
         * 如果文件上传成功并插入数据库记录，将返回包含数据ID的JSON响应。
         * 如果在文件操作或数据库操作中发生异常，将返回500 Internal Server Error响应，数据ID为null。
         */
    @PostMapping("/upload-data")
    public ResponseEntity<String> uploadData(@RequestParam("file_name") String fileName,
                                             @RequestParam("file") MultipartFile file,
                                             @RequestParam("type") String type) {
        try {
            // 检查文件是否为空
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Failed to upload: File is empty.");
            }
            System.out.println("File name: " + fileName);
            System.out.println("Type: " + type);
            // 验证类型是否有效
            if (!isValidType(type)) {
                return ResponseEntity.badRequest().body("Invalid type. Must be one of [ticket, meal, baggage, insurance, seat]");
            }
            // 获取当前时间戳，并格式化为文件夹名称
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
            String uploadTime = sdf.format(new Date());
            // 获取项目根目录
            Path projectRootDir = Paths.get("").toAbsolutePath();
            Path uploadsDir = projectRootDir.resolve("uploads/"+type+"/"+uploadTime);
            // 创建 uploads 目录（如果它不存在）
            Files.createDirectories(uploadsDir);
            // 生成唯一的文件存储路径
            Path filePath = uploadsDir.resolve(fileName);
            // 保存文件
            file.transferTo(filePath.toFile());
            // 返回数据ID
            String dataId = sqlUtils.insertTrainDataRecord(fileName, uploadTime, type);
            return ResponseEntity.ok("{\"data_id\": \"" + dataId + "\"}");
        } catch (IOException | SQLException e) {
            return ResponseEntity.internalServerError().body("{\"data_id\": null}");
        }
    }



        /**
         * 获取历史数据
         *
         * @return ResponseEntity 包含历史数据的响应实体，其中数据以Map形式返回，键为类型名称，值为该类型下的历史数据列表
         * @throws SQLException 如果在数据库操作中发生SQL异常
         */
    @PostMapping("/history")
    public ResponseEntity<Map<String,List<Map<String,String>>>> getHistoryData() throws SQLException {
        // 创建一个用于存储历史数据的Map，键为类型名称，值为该类型下的历史数据列表
        Map<String,List<Map<String,String>>> dataMap = new HashMap<>();
        // 遍历所有类型名称
        for(String typeName: typeNamesInLowerCase) {
            // 根据类型名称获取该类型下的历史数据记录
            List<TrainDataRecord> trainDataRecords = sqlUtils.getTrainDataRecordsByTypeName(typeName);
            // 创建一个列表用于存储转换后的JSON对象
            List<Map<String,String>> trainDataRecordList = new ArrayList<>();
            // 遍历历史数据记录
            for (TrainDataRecord trainDataRecord : trainDataRecords) {
                // 将每条历史数据记录转换为JSON对象并添加到列表中
                trainDataRecordList.add(trainDataRecord.toJson());
            }

            // 将类型名称和对应的历史数据列表添加到dataMap中
            dataMap.put(typeName+"_data_list", trainDataRecordList);
        }
        // 返回包含历史数据的响应实体
        return ResponseEntity.ok(dataMap);
    }
        /**
         * 检查给定的类型是否为有效类型
         *
         * @param type 需要检查的类型
         * @return 如果给定的类型是有效类型，则返回true；否则返回false
         */
    private boolean isValidType(String type) {
        // 遍历 typeNamesInLowerCase 列表中的每个有效类型
        for (String validType : typeNamesInLowerCase) {
            // 如果当前遍历到的有效类型与传入的类型相等
            if (validType.equals(type)) {
                // 返回 true，表示类型有效
                return true;
            }
        }
        // 如果遍历完所有有效类型后仍未找到匹配的类型，则返回 false
        return false;
    }
}



