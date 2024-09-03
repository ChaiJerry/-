package mid_tests;

import packing_system.io.*;

import java.io.*;
import java.util.*;

import static packing_system.api.API.*;
import static packing_system.data_generating_system.FPGrowth.*;
import static org.junit.Assert.*;
import static packing_system.io.SharedAttributes.*;

public class TestAPI {

    @org.junit.Test
    public void test() throws IOException {
        CSVFileIO fileIO = getFileIO();
        List<List<String>> listOfAttributeList = fileIO.singleTypeCsv2ListOfAttributeList(HOTEL);
        List<List<String>> itemTicketFreqItemSets = new ArrayList<>();
        List<List<String>> itemTicketRules = new ArrayList<>();
        associationRulesMining(listOfAttributeList, false, false, itemTicketFreqItemSets, itemTicketRules, 0.95F,0);
        assertTrue(itemTicketFreqItemSets.isEmpty());
        assertTrue(itemTicketRules.isEmpty());

        associationRulesMining(listOfAttributeList, true, true, itemTicketFreqItemSets, itemTicketRules, 0.95F,0);
        assertFalse(itemTicketFreqItemSets.isEmpty());
        assertFalse(itemTicketRules.isEmpty());
        assertEquals(2679, itemTicketRules.size());
    }
}
