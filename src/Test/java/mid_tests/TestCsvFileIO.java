package mid_tests;

import org.junit.*;

import java.io.*;
import java.util.*;

import static io.CSVFileIO.*;
import static io.SharedAttributes.*;
import static org.junit.Assert.*;

public class TestCsvFileIO {
    @Test
    public void test() throws IOException {
        assertEquals(fileIO.read(TICKET).size(), getTestMap().size()+getTrainingMap().size());
    }

    @Test
    public void testReadTicket() throws IOException {
        Map<String, List<List<String>>> testMap = fileIO.read(0);
        int count = 0;
        for(Map.Entry<String, List<List<String>>> entry : testMap.entrySet()) {
            List<List<String>> value = entry.getValue();
            if(value.size() == 2) {
                count++;
                for(int i= 0; i < value.get(1).size(); i++) {
                    if(i==2||i==3){
                        continue;
                    }
                    assertEquals(value.get(0).size(), value.get(1).size());
                }
            }
        }
        assertTrue(count >= 10);
    }

    @Test
    public void TestGetMaps() throws IOException {
        Map<String, List<List<String>>> testMap = getTestMap();
        assertFalse(testMap.isEmpty());
        Map<String, List<List<String>>> trainingMap = getTrainingMap();
        assertFalse(trainingMap.isEmpty());
    }
}
