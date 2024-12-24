package com.bundling.controllers;

import bundle_system.io.sql.*;
import com.bundling.dto.*;
import com.bundling.service.*;
import com.bundling.vo.*;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.text.*;
import java.util.*;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.bundling.Application.*;

@RestController

@RequestMapping("/api")
/*
  训练有关的接口
 */
public class TrainController {
    //训练id字段名，用于返回给前端训练的唯一标识符
    public static final String TRAIN_ID = "train_id";
    //训练结果预览服务
    private final ResultPreviewService resultPreviewService = new ResultPreviewService();
    //项目根目录路径，用于保存上传的文件
    public static final Path projectRootDir = Paths.get("").toAbsolutePath();
    //上传文件目录路径，用于保存上传的文件
    public static final Path uploadsPath = projectRootDir.resolve("uploads");
    //训练服务
    private TrainService trainService = new TrainService();
    //日志记录器
    static Logger logger = LoggerFactory.getLogger(TrainController.class);

    static {
        try {
            // 创建uploads目录，如果不存在则创建
            Files.createDirectories(uploadsPath);
            // 记录日志，提示uploads目录的位置
            String msg = "uploads目录存在于：" + uploadsPath;
            logger.info(msg);
        } catch (IOException e) {
            // 如果创建uploads目录失败，则记录错误日志
            String msg = "uploads目录不存在且创建失败";
            logger.error(msg, e);
        }
    }

    /**
     * 训练所有类型的数据
     *
     * @param request 包含训练所需数据的DTO对象
     * @return ResponseEntity 包含训练结果的响应实体
     * 该方法用于处理训练所有类型数据的请求。
     * 首先，它会验证请求参数是否完整，如果缺少必要的字段，则返回400 Bad Request响应，提示缺少必要的字段。
     * 如果请求参数完整，则获取当前时间戳，并调用sqlUtils.insertTrainRecord方法插入一条训练记录，获取生成的训练ID。
     * 然后，调用trainService.train方法进行训练，并传入请求参数和训练ID。
     * 如果训练成功，将训练ID添加到响应中，并返回200 OK响应。
     * 如果在训练过程中发生异常，则捕获异常，将异常信息添加到响应中，并返回500 Internal Server Error响应。
     */
    @PostMapping("/train/all")
    public ResponseEntity<Map<String, String>> trainAll(@RequestBody TrainDTO request) {
        Map<String, String> response = new HashMap<>();
        try {
            // 验证请求参数
            if (request.getTicket_data_id() == null || request.getMeal_data_id() == null ||
                    request.getBaggage_data_id() == null || request.getInsurance_data_id() == null ||
                    request.getSeat_data_id() == null || request.getMinSupport() == null || request.getMinConfidence() == null) {
                response.put("message", "Missing required fields");
                response.put(TRAIN_ID, null);
                return ResponseEntity.badRequest().body(response);
            }

            // 获取当前时间戳
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
            String startTime = sdf.format(new Date());

            // 插入训练记录，获取生成的train_id
            String tid = sqlUtils.insertTrainRecord(startTime, null, null
                    , request.getComment(), request.getMinSupport()
                    , request.getMinConfidence());
            // 训练数据，并返回结果
            trainService.train(request, Integer.parseInt(tid));
            // 添加训练ID到响应中
            response.put(TRAIN_ID, tid);
            // 返回结果
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // 捕获异常，返回错误信息
            response.put("message", e.getMessage());
            response.put(TRAIN_ID, null);
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 根据数据ID获取数据文件的路径
     *
     * @param dataId   数据ID
     * @param sqlUtils SQL工具类对象，用于从数据库中查询数据记录
     * @return 数据文件的路径
     * @throws SQLException 如果在查询数据库记录时发生SQL异常
     *                      该方法首先根据传入的数据ID和分隔符"-"或"_"进行分割，获取数据类型和记录ID。
     *                      然后，使用SQL工具类对象sqlUtils从数据库中查询对应的数据记录。
     *                      如果查询结果为空，则抛出SQLException异常，提示“数据库中记录的数据在本地不存在”。
     *                      如果查询成功，则从返回的数据记录中获取文件名和上传时间，并构造数据文件的完整路径。
     *                      最后，返回数据文件的路径。
     */
    public static String getDataFilePath(String dataId, SQLUtils sqlUtils) throws SQLException {
        String[] split = dataId.split("-");
        String msg = "dataId:" + dataId;
        // 记录日志，提示dataId的值
        logger.info(msg);
        if (split.length < 2) {
            split = dataId.split("_");
        }
        // 根据数据类型和记录ID查询数据库中的数据记录
        TrainDataRecord trainDataRecord = sqlUtils.getTrainDataRecordByDid(Integer.parseInt(split[1]), split[0]);
        // 如果查询结果为空，则抛出SQLException异常
        if (trainDataRecord == null) throw new SQLException("数据库中记录的数据在本地不存在");
        // 从数据记录中获取文件名和上传时间，并构造数据文件的完整路径
        String fileName = trainDataRecord.getFileName();
        String uploadTime = trainDataRecord.getUploadTime();
        return uploadsPath.resolve(split[0]).resolve(uploadTime).resolve(fileName).toString();
    }

    /**
     * 获取训练状态
     *
     * @param train_id 训练任务的唯一标识符
     * @return ResponseEntity 包含训练状态的响应实体
     * @throws SQLException 如果在查询数据库时发生SQL异常
     *该方法用于获取指定训练任务的状态。
     *它通过@PathVariable注解获取路径变量train_id，然后调用服务层方法sqlUtils.getTrainStatusByTid获取训练状态。
     *最后，将训练状态封装在Map中，并通过ResponseEntity返回。
     */
    @GetMapping("/train/status/{train_id}")
    public ResponseEntity<Map<String, String>> getTrainStatus(@PathVariable String train_id) throws SQLException {
        // 调用服务层获取训练状态
        String status = sqlUtils.getTrainStatusByTid(getTid(train_id));
        Map<String, String> response = new HashMap<>();
        response.put("status", status);
        return ResponseEntity.ok(response);
    }

    /**
     * 预览结果，由于现在的规则比较少，所以不会分页或者限制数量
     *
     * @param trainId 训练id
     * @return 预览结果
     */
    @GetMapping("/result/preview/{trainId}")
    public AssociationRulesVO getPreview(@PathVariable String trainId) throws SQLException {
        return resultPreviewService.getPreviewByTrainId(getTid(trainId));
    }


    /**
     * 查询历史训练记录
     *
     * @return ResponseEntity 包含历史训练记录的响应实体
     * @throws SQLException 如果在查询数据库时发生SQL异常
     * 该方法用于获取所有历史训练记录。
     * 它通过调用sqlUtils.getTrainRecordMaps()方法从数据库中检索训练记录，并将结果封装在List<Map<String, String>>中返回。
     * 如果查询成功，将返回一个包含历史训练记录的响应实体；如果查询过程中发生SQL异常，将抛出SQLException。
     */
    @GetMapping("/history")
    public ResponseEntity<List<Map<String, String>>> queryHistory() throws SQLException {
        List<Map<String, String>> maps = sqlUtils.getTrainRecordMaps();
        return ResponseEntity.ok(maps);
    }

    /**
     * 从字符串中获取训练id
     * @param trainId 训练id字符串
     * @return 训练id
     */
    public static int getTid(String trainId) {
        return Integer.parseInt(trainId);
    }

}
