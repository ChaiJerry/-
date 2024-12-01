package bundle_system.io.sql;

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

    public String toJson(){
        return "{" +
                "\"data_id\": \""+this.getDataIdForFrontEnd()+"\"," +
                "\"file_name\": \""+this.fileName +"\"," +
                "\"upload_time\": \""+this.uploadTime +"\"" +
                "}";
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
