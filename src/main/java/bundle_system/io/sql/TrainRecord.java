package bundle_system.io.sql;

import org.jetbrains.annotations.*;

import java.util.*;

public class TrainRecord implements Comparable<TrainRecord>{
    // 训练记录的id
    private final int tid;
    // 训练开始时间
    private final String startTime;
    // 训练结束时间
    private final String endTime;
    // 订单数量
    private final String orderNumber;
    // 备注信息
    private final String comments;
    // 最小支持度
    private final String minSupport;
    // 最小置信度
    private final String minConfidence;
    private final Map<String,String> trainingRecordMap;

    /**
     * 构造一个训练记录对象，但不包含tid字段
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param orderNumber 订单数量
     * @param comments 备注信息
     * @param minSupport 最小支持度
     * @param minConfidence 最小置信度
     * @param trainingRecordMap 训练记录的map信息
     */
    public TrainRecord(String startTime, String endTime
            , String orderNumber, String comments, String minSupport
            , String minConfidence, Map<String, String> trainingRecordMap) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.orderNumber = orderNumber;
        this.comments = comments;
        this.minSupport = minSupport;
        this.minConfidence = minConfidence;
        this.trainingRecordMap = trainingRecordMap;
        this.tid = -1;
    }


    /**
     * @param trainingRecordMap 包含训练记录信息的map
     * @param tid 训练记录的tid
     */
    public TrainRecord(Map<String,String> trainingRecordMap,int tid){
        this.startTime = trainingRecordMap.get("startTime");
        this.endTime = trainingRecordMap.get("endTime");
        this.orderNumber = trainingRecordMap.get("orderNumber");
        this.comments = trainingRecordMap.get("comments");
        this.minSupport = trainingRecordMap.get("minSupport");
        this.minConfidence = trainingRecordMap.get("minConfidence");
        this.trainingRecordMap = trainingRecordMap;
        this.tid = tid;
    }

    /**
     * 将内部的tid转换为更直观的train-id
     * @param id 训练记录的tid
     * @return train-id字符串
     */
    public static String getTrainIdFromId(int id) {
        return "train-" + id;
    }

    /** 按照训练记录的train_id进行排序的方法
     * @param trainRecords 需要排序的列表
     */
    public static List<TrainRecord> sortById(List<TrainRecord> trainRecords) {
        Collections.sort(trainRecords);
        return trainRecords;
    }

    /**
     * 将训练记录列表转换为map列表，方便发送到前端进行展示
     * @param trainRecords 需要转换的列表
     * @return map列表
     */
    public static List<Map<String,String>> toMapList(List<TrainRecord> trainRecords){
        List<Map<String,String>> mapList=new ArrayList<>();
        for(TrainRecord trainRecord:trainRecords){
            mapList.add(trainRecord.getTrainingRecordMap());
        }
        return mapList;
    }

    public Map<String, String> getTrainingRecordMap() {
        return trainingRecordMap;
    }


    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public String getComments() {
        return comments;
    }

    public String getMinSupport() {
        return minSupport;
    }

    public String getMinConfidence() {
        return minConfidence;
    }

    /**
     * 将训练记录转换为json格式的map
     * 方便前端返回
     * @return 训练数据的json格式
     */
    public Map<String,String> toJson() {
        Map<String, String> map = new HashMap<>();
        map.put("train_id", ""+tid);
        map.put("startTime", startTime);
        map.put("endTime", endTime);
        map.put("orderNumber", orderNumber);
        map.put("comments", comments);
        map.put("minSupport", minSupport);
        map.put("minConfidence", minConfidence);
        return map;
    }

    /**
     * 将训练记录转换为json格式的字符串
     * @return 训练数据的json格式
     */
    @Override
    public String toString() {
        return String.format("{%n" +
                        "  \"train_id\": \"%s\",%n" +
                        "  \"startTime\": \"%s\",%n" +
                        "  \"endTime\": \"%s\",%n" +
                        "  \"orderNumber\": \"%s\",%n" +
                        "  \"comments\": \"%s\",%n" +
                        "  \"minSupport\": \"%s\",%n" +
                        "  \"minConfidence\": \"%s\"%n" +
                        "}",
                this.tid,
                this.startTime,
                this.endTime,
                this.orderNumber,
                this.comments,
                this.minSupport,
                this.minConfidence);
    }

    @Override
    public int compareTo(@NotNull TrainRecord o) {
        //通过train_id中如"train-1"中的数字序号来比较
        //使用快速排序后，train_id中数字序号越大，排序越靠前（降序）
        return tid - o.tid;
    }


}
