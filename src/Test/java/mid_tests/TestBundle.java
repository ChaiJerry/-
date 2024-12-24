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
        BackendBundleSystem backendBundleSystem = new BackendBundleSystem();
        List<RulesStorage> rulesStorages = backendBundleSystem.getRulesStorages();
        assertEquals(246, rulesStorages.get(MEAL).getSize());
        assertEquals(240, rulesStorages.get(BAGGAGE).getSize());
        assertEquals(380, rulesStorages.get(INSURANCE).getSize());
        assertEquals(120, rulesStorages.get(SEAT).getSize());
        assertTrue(backendBundleSystem.test());
    }
}
