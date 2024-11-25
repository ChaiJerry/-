package bundle_system.memory_query_system.lru_pool;

import java.util.*;

public class LRUPool {

    private LRUUnit head=null;
    private LRUUnit tail=null;
    private final Map<List<String>,LRUUnit> resultMap =new HashMap<>();
    private int max;
    public LRUPool(int max) {this.max=max;}
    public void randomAdd(List<String> key,Map<String, String> value) {
        add(key,value);
    }

    public void add(List<String> key,Map<String, String> value) {
        if (resultMap.containsKey(key)) {
            moveToHead(resultMap.get(key));
        }else {
            LRUUnit unit = new LRUUnit(key,value, head);
            if (head != null) {
                head.setPrev(unit);
            }
            head = unit;
            if (tail == null) {
                tail = unit;
            }
            resultMap.put(key, unit);
            if (resultMap.size() > max) {
                removeTail();
            }
        }
    }

    public Map<String, String> tryGet(List<String> key) {
        if (!resultMap.containsKey(key)) return null;
        LRUUnit unit= resultMap.get(key);
        moveToHead(unit);
        return unit.getValue();
    }

    public void moveToHead(LRUUnit unit) {
        if(unit!=head && unit != tail) {
            unit.getPrev().setNext(unit.getNext());
            unit.getNext().setPrev(unit.getPrev());
        }else if (unit==tail) {
            tail=unit.getPrev();
            tail.setNext(null);
            unit.setPrev(null);
        }
        head=unit;
    }

    public void removeTail() {
        if (tail == null)
            return;
        resultMap.remove(tail.getKey());
        tail=tail.getPrev();
        tail.setNext(null);
    }
}
