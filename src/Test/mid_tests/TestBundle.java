package mid_tests;

import bundle_service_for_backend.*;
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
        Assertions.assertEquals(415, rulesStorages.get(SharedAttributes.HOTEL).getSize());
        Assertions.assertEquals(246, rulesStorages.get(SharedAttributes.MEAL).getSize());
        Assertions.assertEquals(240, rulesStorages.get(SharedAttributes.BAGGAGE).getSize());
        Assertions.assertEquals(380, rulesStorages.get(SharedAttributes.INSURANCE).getSize());
        Assertions.assertEquals(120, rulesStorages.get(SharedAttributes.SEAT).getSize());
    }
}
