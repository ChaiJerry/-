package com.bundling.service;

import bundle_system.api.*;
import bundle_system.io.*;
import com.bundling.dto.*;

import java.io.*;
import java.sql.*;
import java.text.*;
import java.util.*;
import java.util.Date;
import java.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static bundle_system.io.SharedAttributes.*;
import static com.bundling.Application.*;
import static com.bundling.controllers.TrainController.*;

/**
 * TrainTask 是一个实现了 Callable 接口的任务类，用于处理训练相关的数据挖掘任务。
 * 该任务的主要功能包括：
 * 1. 根据传入的 TrainDTO 对象获取各个数据文件的路径。
 * 2. 检查这些文件是否存在。
 * 3. 解析 CSV 文件并进行关联规则挖掘。
 * 4. 将挖掘结果存储到数据库中。
 * 5. 更新训练记录的相关信息（订单数量、结束时间等）。
 */
public class TrainTask implements Callable<Void> {
    // 包含任务所需的各种参数的 DTO 对象
    private final TrainDTO request;
    // 训练任务的唯一标识符
    private final int tid;
    // 日志记录器
    private static final Logger logger = LoggerFactory.getLogger(TrainTask.class);

    /**
     * 构造函数，初始化 TrainTask 实例。
     *
     * @param request 包含任务所需各种参数的 TrainDTO 对象。
     * @param tid     训练任务的唯一标识符。
     */
    public TrainTask(TrainDTO request, int tid) {
        this.request = request;
        this.tid = tid;
    }

    /**
     * 执行任务的主要逻辑。
     *
     * @return 返回 null 表示任务成功完成。
     * @throws SQLException 如果发生 SQL 相关错误，则抛出此异常。
     * @throws IOException  如果发生 I/O 相关错误，则抛出此异常。
     */
    @Override
    public Void call() throws SQLException, IOException {
        try {
            logger.info("train task start"); // 记录任务开始的日志信息
            String msg;

            // 获取机票类型数据集的路径
            String ticketFilePath = getDataFilePath(request.getTicket_data_id(), sqlUtils);
            msg = "ticketFilePath:" + ticketFilePath;
            logger.info(msg);

            // 获取餐食类型数据集的路径
            String mealFilePath = getDataFilePath(request.getMeal_data_id(), sqlUtils);
            msg = "mealFilePath:" + mealFilePath;
            logger.info(msg);

            // 获取行李类型数据集的路径
            String baggageFilePath = getDataFilePath(request.getBaggage_data_id(), sqlUtils);
            msg = "baggageFilePath:" + baggageFilePath;
            logger.info(msg);

            // 获取保险类型数据集的路径
            String insuranceFilePath = getDataFilePath(request.getInsurance_data_id(), sqlUtils);
            msg = "insuranceFilePath:" + insuranceFilePath;
            logger.info(msg);

            // 获取座位类型数据集的路径
            String seatFilePath = getDataFilePath(request.getSeat_data_id(), sqlUtils);
            msg = "seatFilePath:" + seatFilePath;
            logger.info(msg);

            // 将所有文件路径存入列表
            List<String> filePaths = new ArrayList<>();
            // 将所有文件路径添加到列表中
            filePaths.add(ticketFilePath);
            filePaths.add(mealFilePath);
            filePaths.add(baggageFilePath);
            filePaths.add(insuranceFilePath);
            filePaths.add(seatFilePath);

            // 检查文件是否存在，并打印检查结果
            List<Boolean> results = checkFilesExistence(filePaths);
            for (int i = 0; i < results.size(); i++) {
                msg = "File " + (i + 1) + " exists: " + results.get(i);
                logger.info(msg);
            }

            // 从 TrainDTO 中解析最小置信度和支持度阈值
            double minConfidence = Double.parseDouble(request.getMinConfidence());
            double minSupport = Double.parseDouble(request.getMinSupport());

            int orderNumber = 0; // 初始化订单编号计数器
            CSVFileIO csvFileIO = new CSVFileIO(ticketFilePath, null,
                    mealFilePath, baggageFilePath, insuranceFilePath, seatFilePath);

            // 遍历每种数据类型，进行关联规则挖掘
            for (int type = MEAL; type <= SEAT; type++) {
                msg = "正在处理type:" + getFullNames()[type];
                logger.info(msg);
                // 存储挖掘得到的关联规则
                List<List<String>> rules = new ArrayList<>();
                // 整理数据集，得到机票属性和其它附加产品的属性共现数据集
                List<List<String>> listOfAttributeList = csvFileIO.csv2ListOfAttributeListByType(type);
                // 计算订单数量
                orderNumber += listOfAttributeList.size();
                // 打印日志信息，表示 CSV 文件解析完成
                logger.info("csv文件解析完成");
                // 调用 API 进行关联规则挖掘，并将结果存储到数据库中
                API.associationRulesMining(listOfAttributeList, false, true, null, rules, minSupport, minConfidence);
                sqlUtils.insertRules(type, rules, tid);
            }
            // 打印总共订单数量信息
            msg = "orderNumber:" + orderNumber;
            logger.info(msg);

            // 更新训练记录中的订单数量
            sqlUtils.upDateTrainRecordOrderNumber(tid, orderNumber + "");

            // 获取当前时间戳并更新训练记录中的结束时间
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
            // 获取当前时间并格式化为字符串
            String endTime = sdf.format(new Date());
            // 更新训练记录的结束时间字段
            sqlUtils.updateTrainRecordEndTime(tid, endTime);
            // 返回 null 表示任务成功完成
            return null;
        } catch (Exception e) {
            // 若是出现异常，则更新训练记录的结束时间为 "error" 并打印错误日志。
            sqlUtils.updateTrainRecordEndTime(tid, "error"); // 记录错误状态
            logger.error("train task error", e); // 记录详细的错误日志
            throw e; // 抛出捕获到的异常
        }
    }

    /**
     * 检查指定路径下的文件是否存在。
     *
     * @param filePaths 包含多个文件路径的列表。
     * @return 包含每个文件存在与否布尔值的列表。
     */
    public List<Boolean> checkFilesExistence(List<String> filePaths) {
        // 创建一个空的布尔值列表来存储每个文件的检查结果
        List<Boolean> existenceResults = new ArrayList<>();
        // 遍历文件路径列表，检查每个文件的实际存在性
        for (String filePath : filePaths) {
            // 创建 File 对象，并检查文件是否存在
            File file = new File(filePath);
            // 将检查结果添加到布尔值列表中
            existenceResults.add(file.exists());
        }
        // 返回包含每个文件存在与否结果的列表
        return existenceResults;
    }
}



