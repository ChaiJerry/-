package bundle_system.io.sql;

import org.jetbrains.annotations.*;

import java.util.*;

public class TrainRecord implements Comparable<TrainRecord>{
    private final int tid;
    private final String startTime;
    private final String endTime;

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

    private final String orderNumber;
    private final String comments;
    private final String minSupport;
    private final String minConfidence;
    private final Map<String,String> trainingRecordMap;

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

    @Override
    public String toString() {
        return "{\n" +
                "\"train_id\": \""+this.tid +"\",\n" +
                "\"startTime\": \""+this.startTime+"\",\n" +
                "\"endTime\": \""+this.endTime+"\",\n" +
                "\"orderNumber\": \""+this.orderNumber+"\",\n" +
                "\"comments\": \""+this.comments+"\",\n" +
                "\"minSupport\": \""+this.minSupport+"\",\n" +
                "\"minConfidence\": \""+this.minConfidence+"\"\n"+
                "}";
    }

    @Override
    public int compareTo(@NotNull TrainRecord o) {
        //通过train_id中如"train-1"中的数字序号来比较
        //使用快速排序后，train_id中数字序号越大，排序越靠前（降序）
        return tid - o.tid;
    }


}
