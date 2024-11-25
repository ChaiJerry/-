package mid_tests;

import bundle_system.io.*;

import java.io.*;
import java.util.*;

import static bundle_system.api.API.*;
import static bundle_system.data_generating_system.FPGrowth.*;
import static org.junit.Assert.*;
import static bundle_system.io.SharedAttributes.*;

public class TestAPI {

    @org.junit.Test
    public void test() throws IOException {
        CSVFileIO fileIO = getFileIO();
        List<List<String>> listOfAttributeList = fileIO.singleTypeCsv2ListOfAttributeList(HOTEL);
        List<List<String>> itemTicketFreqItemSets = new ArrayList<>();
        List<List<String>> itemTicketRules = new ArrayList<>();
        associationRulesMining(listOfAttributeList, false, false, itemTicketFreqItemSets, itemTicketRules, 0.07,0);
        assertTrue(itemTicketFreqItemSets.isEmpty());
        assertTrue(itemTicketRules.isEmpty());

        associationRulesMining(listOfAttributeList, true, true, itemTicketFreqItemSets, itemTicketRules, 0.07,0);
        assertFalse(itemTicketFreqItemSets.isEmpty());
        assertFalse(itemTicketRules.isEmpty());
        assertEquals(2679, itemTicketRules.size());
    }
}
