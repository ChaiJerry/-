package mid_tests;

import io.*;
import org.junit.*;

import java.io.*;
import java.util.*;

import static io.IOMonitor.*;
import static org.junit.Assert.*;
import static query_system.QuerySystem.*;

public class TestQuerySystem {
    @Test
    public void testGetTicketAttributeValuesList() throws IOException {
        CSVFileIO fileIO = new CSVFileIO(resultDirPath, pathT, pathH, pathM, pathB, pathI, pathS);
        List<List<String>> ticketAttributeValuesList = getTicketAttributeValuesList(fileIO,0);
        assertTrue(ticketAttributeValuesList.size() > 200);
        for (List<String> ticketAttributeValues : ticketAttributeValuesList) {
            assertTrue(ticketAttributeValues.size() == 6);
        }
    }

}
