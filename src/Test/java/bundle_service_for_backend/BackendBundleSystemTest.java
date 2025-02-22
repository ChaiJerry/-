package bundle_service_for_backend;

import static org.junit.jupiter.api.Assertions.*;

import bundle_system.memory_query_system.*;
import org.junit.*;

import java.io.*;
import java.util.*;

import static bundle_system.io.SharedAttributes.*;
public class BackendBundleSystemTest {
    @Test
    public void testLoadingRules() throws IOException, InterruptedException {
        BackendBundleSystem backendBundleSystem = new BackendBundleSystem();
        List<RulesStorage> rulesStorages = backendBundleSystem.getRulesStorages();
        assertEquals(415, rulesStorages.get(HOTEL).getSize());
        assertEquals(246, rulesStorages.get(MEAL).getSize());
        assertEquals(240, rulesStorages.get(BAGGAGE).getSize());
        assertEquals(380, rulesStorages.get(INSURANCE).getSize());
        assertEquals(120, rulesStorages.get(SEAT).getSize());
        backendBundleSystem.test();
    }
}