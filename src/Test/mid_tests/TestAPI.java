package mid_tests;

import bundle_system.api.*;
import bundle_system.io.*;
import bundle_system.train_system.*;
import org.junit.*;

import java.io.*;
import java.util.*;

public class TestAPI {

    @org.junit.Test
    public void test() throws IOException {

        CSVFileIO fileIO = FPGrowth.getFileIO();
        List<List<String>> listOfAttributeList = fileIO.csv2ListOfAttributeListByType(SharedAttributes.INSURANCE);
        List<List<String>> itemTicketFreqItemSets = new ArrayList<>();
        List<List<String>> itemTicketRules = new ArrayList<>();
        API.associationRulesMining(listOfAttributeList, false, false, itemTicketFreqItemSets, itemTicketRules, 0.08,0);
        Assert.assertTrue(itemTicketFreqItemSets.isEmpty());
        Assert.assertTrue(itemTicketRules.isEmpty());

        API.associationRulesMining(listOfAttributeList, true, true, itemTicketFreqItemSets, itemTicketRules, 0.08,0);
        Assert.assertFalse(itemTicketFreqItemSets.isEmpty());
        Assert.assertFalse(itemTicketRules.isEmpty());
        Assert.assertEquals(380, itemTicketRules.size());
    }
}
