package bundle_system.io.sql;

import java.util.*;

public class TrainDataRecord {

    private int dataId;
    private String fileName;
    private String uploadTime;
    private String type;
    public TrainDataRecord(int dataId, String fileName, String uploadTime, String type) {
        this.dataId = dataId;
        this.fileName = fileName;
        this.uploadTime = uploadTime;
        this.type = type;
    }

    private String getDataIdForFrontEnd() {
        return  type+"_"+dataId;
    }

    public Map<String,String> toJson(){
        Map<String, String> map = new HashMap<>();
        map.put("data_id", this.getDataIdForFrontEnd());
        map.put("file_name", this.fileName);
        map.put("upload_time", this.uploadTime);
        return map;
    }

    public static List<Map<String,String>> getJsonList(List<TrainDataRecord> list){
        List<Map<String, String>> jsons = new ArrayList<>();
        for (TrainDataRecord trainDataRecord : list) {
            jsons.add(trainDataRecord.toJson());
        }
        return jsons;
    }

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
