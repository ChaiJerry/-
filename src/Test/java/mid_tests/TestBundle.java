package mid_tests;

import bundle_service_for_backend.*;
import bundle_system.io.sql.*;
import bundle_system.memory_query_system.*;
import org.junit.*;

import java.io.*;
import java.util.*;

import static bundle_system.io.SharedAttributes.*;
import static org.junit.jupiter.api.Assertions.*;
public class TestBundle {
    @Test
    public void test() throws InterruptedException, IOException {
        SQLUtils sqlUtils = new SQLUtils();
        BackendBundleSystem backendBundleSystem = new BackendBundleSystem();
        List<RulesStorage> rulesStorages = backendBundleSystem.getRulesStorages();
        assertEquals(rulesStorages.get(MEAL).getSize(), 246);
        assertEquals(rulesStorages.get(BAGGAGE).getSize(), 240);
        assertEquals(rulesStorages.get(INSURANCE).getSize(), 380);
        assertEquals(rulesStorages.get(SEAT).getSize(), 120);
        assertTrue(backendBundleSystem.test());
    }
}
