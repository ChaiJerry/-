package bundle_system.io.sql;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class TrainRecordTest {

    private TrainRecord record1;
    private TrainRecord record2;
    private TrainRecord record3;

    @BeforeEach
    public void setUp() {
        Map<String, String> trainingRecordMap1 = new HashMap<>();
        trainingRecordMap1.put("startTime", "2025-02-22 14:24:00");
        trainingRecordMap1.put("endTime", "2025-02-22 14:30:00");
        trainingRecordMap1.put("orderNumber", "100");
        trainingRecordMap1.put("comments", "Initial training run");
        trainingRecordMap1.put("minSupport", "0.1");
        trainingRecordMap1.put("minConfidence", "0.2");

        Map<String, String> trainingRecordMap2 = new HashMap<>(trainingRecordMap1);
        trainingRecordMap2.put("startTime", "2025-02-22 14:31:00");
        trainingRecordMap2.put("endTime", "2025-02-22 14:37:00");

        Map<String, String> trainingRecordMap3 = new HashMap<>(trainingRecordMap1);
        trainingRecordMap3.put("startTime", "2025-02-22 14:25:00");
        trainingRecordMap3.put("endTime", "2025-02-22 14:32:00");

        record1 = new TrainRecord(trainingRecordMap1, 1);
        record2 = new TrainRecord(trainingRecordMap2, 2);
        record3 = new TrainRecord(trainingRecordMap3, 3);
    }

    @Test
    public void testGetTrainIdFromId() {
        assertEquals("train-1", TrainRecord.getTrainIdFromId(1));
        assertEquals("train-2", TrainRecord.getTrainIdFromId(2));
        assertEquals("train-3", TrainRecord.getTrainIdFromId(3));
    }

    @Test
    public void testSortById() {
        List<TrainRecord> records = Arrays.asList(record2, record1, record3);
        List<TrainRecord> sortedRecords = TrainRecord.sortById(records);

        assertEquals(record1, sortedRecords.get(0)); // tid = 1
        assertEquals(record3, sortedRecords.get(1)); // tid = 3
        assertEquals(record2, sortedRecords.get(2)); // tid = 2
    }

    @Test
    public void testToMapList() {
        List<TrainRecord> records = Arrays.asList(record1, record2, record3);
        List<Map<String, String>> mapList = TrainRecord.toMapList(records);

        assertEquals(3, mapList.size());

        assertEquals("1", mapList.get(0).get("tid"));
        assertEquals("2", mapList.get(1).get("tid"));
        assertEquals("3", mapList.get(2).get("tid"));

        assertEquals("2025-02-22 14:24:00", mapList.get(0).get("startTime"));
        assertEquals("2025-02-22 14:31:00", mapList.get(1).get("startTime"));
        assertEquals("2025-02-22 14:25:00", mapList.get(2).get("startTime"));
    }

    @Test
    public void testToJson() {
        Map<String, String> expectedJson = new HashMap<>();
        expectedJson.put("train_id", "1");
        expectedJson.put("startTime", "2025-02-22 14:24:00");
        expectedJson.put("endTime", "2025-02-22 14:30:00");
        expectedJson.put("orderNumber", "100");
        expectedJson.put("comments", "Initial training run");
        expectedJson.put("minSupport", "0.1");
        expectedJson.put("minConfidence", "0.2");

        assertEquals(expectedJson, record1.toJson());
    }

    @Test
    public void testToString() {
        String expectedString = "{\n" +
                "  \"train_id\": \"1\",\n" +
                "  \"startTime\": \"2025-02-22 14:24:00\",\n" +
                "  \"endTime\": \"2025-02-22 14:30:00\",\n" +
                "  \"orderNumber\": \"100\",\n" +
                "  \"comments\": \"Initial training run\",\n" +
                "  \"minSupport\": \"0.1\",\n" +
                "  \"minConfidence\": \"0.2\"\n" +
                "}";

        assertEquals(expectedString, record1.toString());
    }

    @Test
    public void testCompareTo() {
        assertTrue(record1.compareTo(record2) < 0); // record1.tid (1) < record2.tid (2)
        assertTrue(record2.compareTo(record1) > 0); // record2.tid (2) > record1.tid (1)
        assertEquals(0, record1.compareTo(record1)); // record1.tid (1) == record1.tid (1)
    }
}



