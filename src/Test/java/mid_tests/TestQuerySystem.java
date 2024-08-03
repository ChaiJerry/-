package mid_tests;

import com.mongodb.client.*;
import io.*;
import org.bson.*;
import org.junit.*;


import java.io.*;
import java.util.*;

import static io.SharedAttributes.*;
import static io.MongoUtils.*;
import static org.junit.Assert.*;
import static query_system.QuerySystem.*;

public class TestQuerySystem {
    @Test
    public void testGetTicketAttributeValuesList() throws IOException {
        CSVFileIO fileIO =
                new CSVFileIO(RESULT_DIR_PATH, PATH_T, PATH_H, PATH_M, PATH_B, PATH_I, PATH_S);
        List<List<String>> ticketAttributeValuesList = getTicketAttributeValuesList(fileIO, 0);
        assertTrue(ticketAttributeValuesList.size() > 200);
        for (List<String> ticketAttributeValues : ticketAttributeValuesList) {
            assertEquals(6, ticketAttributeValues.size());
        }
    }

    @Test
    public void testSingleAttributeFreqQuery() throws IOException {
        CSVFileIO fileIO = new CSVFileIO(RESULT_DIR_PATH, PATH_T, PATH_H, PATH_M, PATH_B, PATH_I, PATH_S);
        for (int i = 0; i < 6; i++) {
            fileIO.read(i);
        }
        int type = 1;
        MongoCollection<Document> FreqCollection = getFrequentItemSetsCollection(type);
        List<String> ticketAttributes = new ArrayList<>();
        ticketAttributes.add("Ticket:T_CARRIER" + ":HU");
        Document doc = singleAttributeFreqQuery(ticketAttributes, -1, FreqCollection);
        Map < String, String > ticketAttributesMap = new HashMap < > ();
        String ret = singleFreqQuery(doc, getOrdersCollection(type), type, ticketAttributesMap);
        assertFalse(ret.isEmpty());
        assertFalse(ticketAttributesMap.isEmpty());
    }


}
