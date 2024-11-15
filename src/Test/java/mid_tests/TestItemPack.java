package mid_tests;

import org.junit.*;
import packing_system.db_query_system.*;


import java.util.*;

import static org.junit.Assert.*;

public class TestItemPack {
    @Test
    public void testCalculation() {
        ItemPack itemPack = new ItemPack();
        itemPack.addOrderItem(Arrays.asList("1", "2"), 1);
        itemPack.addRecommendedItem("1", 1);
        double averageAccuracy = itemPack.calculateAverageAccuracy();
        double averageRecallRate = itemPack.calculateAverageRecallRate();
        assertEquals(1.0, averageAccuracy, 0.0);
        assertEquals(0.5, averageRecallRate, 0.0);

        itemPack.addRecommendedItem("3", 1);
        averageAccuracy = itemPack.calculateAverageAccuracy();
        averageRecallRate = itemPack.calculateAverageRecallRate();
        assertEquals(0.5, averageAccuracy, 0.0);
        assertEquals(0.5, averageRecallRate, 0.0);

    }
}
