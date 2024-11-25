package bundle_system.memory_query_system.lru_pool;

import java.util.*;

public class LRUUnit {
    private LRUUnit prev=null;
    private final List<String> key;
    private LRUUnit next=null;
    private Map<String,String> value;

    public List<String> getKey() {
        return key;
    }

    public LRUUnit(List<String> key, Map<String, String> value, LRUUnit next){
        this.key = key;
        this.next = next;
        this.value = value;
    }
    public LRUUnit getPrev() {
        return prev;
    }
    public LRUUnit getNext() {
        return next;
    }
    public Map<String, String> getValue() {
        return value;
    }
    public void setPos(LRUUnit prev,LRUUnit next){
        this.prev = prev;
        this.next = next;
    }

    public void setPrev(LRUUnit prev) {
        this.prev = prev;
    }

    public void setNext(LRUUnit next) {
        this.next = next;
    }

}
