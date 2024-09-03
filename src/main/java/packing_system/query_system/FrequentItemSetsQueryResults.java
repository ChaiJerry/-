package packing_system.query_system;

import java.util.*;

public class FrequentItemSetsQueryResults {
    private Map<String, String> attributes = new HashMap<>();

    private int frequency;

    public FrequentItemSetsQueryResults(Map<String, String> attributes, int frequency) {
        this.attributes = attributes;
        this.frequency = frequency;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

}
