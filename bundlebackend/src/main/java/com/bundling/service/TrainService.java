package com.bundling.service;

import com.bundling.dto.*;

import java.io.*;
import java.sql.*;
import java.util.concurrent.*;

/**
 * TrainService 类用于处理训练任务。
 * 该类使用线程池来异步执行训练任务。
 */
public class TrainService {
    // 创建一个固定大小的线程池，大小为4
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    /**
     * 提交训练任务到线程池。
     *
     * @param request 包含训练请求参数的 TrainDTO 对象。
     * @param tid 训练ID。
     */
    public void train(TrainDTO request, int tid) {
        // 提交一个新的 TrainTask 到线程池进行异步处理
        executor.submit(new TrainTask(request, tid));
    }
}