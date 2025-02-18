package bundle_system.io.sql;

import java.util.*;

/**
 * 训练数据记录类
 * 该类用于封装训练数据记录，以便于训练时使用。
 */
public class TrainDataRecord {
    // 训练数据记录的唯一标识符
    private int dataId;
    // 文件名
    private String fileName;
    // 上传时间
    private String uploadTime;
    // 商品类型
    private String type;

    /**
     * 构造函数
     * @param dataId 训练数据id
     * @param fileName 文件名
     * @param uploadTime   上传时间
     * @param type 商品类型
     */
    public TrainDataRecord(int dataId, String fileName, String uploadTime, String type) {
        this.dataId = dataId;
        this.fileName = fileName;
        this.uploadTime = uploadTime;
        this.type = type;
    }

    /**
     * 获取前端展示的dataId，包含了品类
     * @return dataId
     */
    private String getDataIdForFrontEnd() {
        return  type+"-"+dataId;
    }

    /**
     * 获取文件名
     * @return 文件名
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * 获取上传时间
     * @return 上传时间
     */
    public String getUploadTime() {
        return uploadTime;
    }

    /**
     * 将训练数据记录转换为JSON格式的Map
     * @return JSON格式的Map
     */
    public Map<String,String> toJson(){
        Map<String, String> map = new HashMap<>();
        map.put("data_id", this.getDataIdForFrontEnd());
        map.put("file_name", this.fileName);
        map.put("upload_time", this.uploadTime);
        return map;
    }

    /**
     * 将训练数据记录列表转换为JSON格式的字符串列表
     * @param list 训练数据记录列表
     * @return JSON格式的字符串列表
     */
    public static List<Map<String,String>> getJsonList(List<TrainDataRecord> list){
        List<Map<String, String>> jsons = new ArrayList<>();
        for (TrainDataRecord trainDataRecord : list) {
            jsons.add(trainDataRecord.toJson());
        }
        return jsons;
    }

    /**
     * 将训练数据记录转换为字符串格式
     * @return 字符串格式的训练数据记录
     */
    @Override
    public String toString() {
        return "{" +
                "\"data_id\": \""+this.dataId +"\"," +
                "\"file_name\": \""+this.fileName +"\"," +
                "\"upload_time\": \""+this.uploadTime +"\"" +
                "\"type\": \""+type+"\"" +
                "}";
    }
}
