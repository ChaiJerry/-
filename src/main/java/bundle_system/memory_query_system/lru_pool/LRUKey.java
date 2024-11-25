package bundle_system.memory_query_system.lru_pool;

import java.util.*;

public class LRUKey {
    private List<String> key;
    private long lruTime;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LRUKey lruKey)) return false;
        return key.equals(lruKey.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }
}
