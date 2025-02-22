package bundle_system.io.sql;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TrainDataRecordTest {

    private TrainDataRecord trainDataRecord;

    @BeforeEach
    void setUp() {
        trainDataRecord = new TrainDataRecord(1, "testFile.txt", "2023-10-01T12:00:00Z", "electronics");
    }

    @Test
    void testGetFileName() {
        assertEquals("testFile.txt", trainDataRecord.getFileName());
    }

    @Test
    void testGetUploadTime() {
        assertEquals("2023-10-01T12:00:00Z", trainDataRecord.getUploadTime());
    }

    @Test
    void testToJson() {
        Map<String, String> expectedJson = new HashMap<>();
        expectedJson.put("data_id", "electronics-1");
        expectedJson.put("file_name", "testFile.txt");
        expectedJson.put("upload_time", "2023-10-01T12:00:00Z");

        assertEquals(expectedJson, trainDataRecord.toJson());
    }

    @Test
    void testGetJsonList() {
        List<TrainDataRecord> records = List.of(
                new TrainDataRecord(1, "testFile1.txt", "2023-10-01T12:00:00Z", "electronics"),
                new TrainDataRecord(2, "testFile2.txt", "2023-10-02T12:00:00Z", "clothing")
        );

        List<Map<String, String>> expectedJsonList = List.of(
                Map.of("data_id", "electronics-1", "file_name", "testFile1.txt", "upload_time", "2023-10-01T12:00:00Z"),
                Map.of("data_id", "clothing-2", "file_name", "testFile2.txt", "upload_time", "2023-10-02T12:00:00Z")
        );

        assertEquals(expectedJsonList, TrainDataRecord.getJsonList(records));
    }

    @Test
    void testToString() {
        String expectedString = "{\"data_id\": \"1\",\"file_name\": \"testFile.txt\",\"upload_time\": \"2023-10-01T12:00:00Z\"\"type\": \"electronics\"}";
        assertEquals(expectedString, trainDataRecord.toString());
    }
}



