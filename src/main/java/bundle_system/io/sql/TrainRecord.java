package bundle_system.io.sql;

import org.jetbrains.annotations.*;

import java.util.*;

public class TrainRecord implements Comparable<TrainRecord>{

    private String trainId;
    private String startTime;
    private String endTime;
    private String orderNumber;
    private String comments;
    private String minSupport;
    private String minConfidence;
    private final Map<String,String> trainingRecordMap;
    public TrainRecord(Map<String,String> trainingRecordMap){
        this.trainId = trainingRecordMap.get("train_id");
        this.startTime = trainingRecordMap.get("startTime");
        this.endTime = trainingRecordMap.get("endTime");
        this.orderNumber = trainingRecordMap.get("orderNumber");
        this.comments = trainingRecordMap.get("comments");
        this.minSupport = trainingRecordMap.get("minSupport");
        this.minConfidence = trainingRecordMap.get("minConfidence");
        this.trainingRecordMap = trainingRecordMap;
    }

    /** 按照训练记录的train_id进行排序的方法
     * @param trainRecords 需要排序的列表
     */
    public static List<TrainRecord> sortByTrainId(List<TrainRecord> trainRecords) {
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


    @Override
    public String toString() {
        /*
    训练记录字段返回示例
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
        return "{\n" +
                "\"train_id\": \""+this.trainId +"\",\n" +
                "\"startTime\": \""+this.startTime+"\",\n" +
                "\"endTime\": \""+this.endTime+"\",\n" +
                "\"orderNumber\": \""+this.orderNumber+"\",\n" +
                "\"comments\": \""+this.comments+"\",\n" +
                "\"minSupport\": \""+this.minSupport+"\",\n" +
                "\"minConfidence\": \""+this.minConfidence+"\"\n"+
                "}";
    }

    /**
     * Compares this object with the specified object for order.  Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.
     *
     * <p>The implementor must ensure {@link Integer#signum
     * signum}{@code (x.compareTo(y)) == -signum(y.compareTo(x))} for
     * all {@code x} and {@code y}.  (This implies that {@code
     * x.compareTo(y)} must throw an exception if and only if {@code
     * y.compareTo(x)} throws an exception.)
     *
     * <p>The implementor must also ensure that the relation is transitive:
     * {@code (x.compareTo(y) > 0 && y.compareTo(z) > 0)} implies
     * {@code x.compareTo(z) > 0}.
     *
     * <p>Finally, the implementor must ensure that {@code
     * x.compareTo(y)==0} implies that {@code signum(x.compareTo(z))
     * == signum(y.compareTo(z))}, for all {@code z}.
     *
     * @param o the object to be compared.
     * @return a negative integer, zero, or a positive integer as this object
     * is less than, equal to, or greater than the specified object.
     * @throws NullPointerException if the specified object is null
     * @throws ClassCastException   if the specified object's type prevents it
     *                              from being compared to this object.
     * @apiNote It is strongly recommended, but <i>not</i> strictly required that
     * {@code (x.compareTo(y)==0) == (x.equals(y))}.  Generally speaking, any
     * class that implements the {@code Comparable} interface and violates
     * this condition should clearly indicate this fact.  The recommended
     * language is "Note: this class has a natural ordering that is
     * inconsistent with equals."
     */
    @Override
    public int compareTo(@NotNull TrainRecord o) {
        //通过train_id中如"train-1"中的数字序号来比较
        //使用快速排序后，train_id中数字序号越大，排序越靠前（降序）
        return Integer.parseInt(this.trainId.split("-")[1]) - Integer.parseInt(o.trainId.split("-")[1]);
    }

    public String getTrainId() {
        return trainId;
    }

    public String getNextTrainId() {
        return trainId.split("-")[0] + "-" + (Integer.parseInt(trainId.split("-")[1])+1);
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
}
