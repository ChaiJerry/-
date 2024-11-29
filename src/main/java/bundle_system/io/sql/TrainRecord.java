package bundle_system.io.sql;

import java.util.*;

public class TrainRecord {
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
    private String train_id;
    private String startTime;
    private String endTime;
    private String orderNumber;
    private String comments;
    private String minSupport;
    private String minConfidence;
    public TrainRecord(Map<String,String> trainingRecordMap){
        this.train_id = trainingRecordMap.get("train_id");
        this.startTime = trainingRecordMap.get("startTime");
        this.endTime = trainingRecordMap.get("endTime");
        this.orderNumber = trainingRecordMap.get("orderNumber");
        this.comments = trainingRecordMap.get("comments");
        this.minSupport = trainingRecordMap.get("minSupport");
        this.minConfidence = trainingRecordMap.get("minConfidence");
    }

}
