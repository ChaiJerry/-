package bundle_system.io.sql;

import java.util.*;

public class TrainDataRecord {

    private int data_id;
    private String file_name;
    private String upload_time;
    private String type;
    public TrainDataRecord(int data_id, String file_name, String upload_time, String type) {
        this.data_id = data_id;
        this.file_name = file_name;
        this.upload_time = upload_time;
        this.type = type;
    }

    @Override
    public String toString() {
        return "{\n" +
                "\"data_id\": \""+this.data_id+"\",\n" +
                "\"file_name\": \""+this.file_name+"\",\n" +
                "\"upload_time\": \""+this.upload_time+"\"\n" +
                "\"type\": \""+type+"\"\n" +
                "}";
    }
}
