package bundle_system.memory_query_system.lru_pool;

import bundle_system.memory_query_system.*;

import java.util.*;

public class RandomLRUPool {
    //用于lru时间查询的map
    private final Map<List<String>,Long> timeMap=new HashMap<>();
    //用于通过机票条件进行缓存结果查询的map
    private final Map<List<String>,Map<String, AttrValueConfidencePriority>> resultMap =new HashMap<>();
    //使用ArrayListList提高访问时随机淘汰的效率
    private final List<List<String>> keys=new ArrayList<>();
    //指向keys的指针用于随机访问
    private int keysPtr=0;
    private static Long time= 0L;
    private int max;

    /**
     * 构造函数
     * @param max 最大容量，必须为8的倍数
     */
    public RandomLRUPool(int max) {this.max=max;}
    public Map<String, AttrValueConfidencePriority> tryGet(List<String> key){
        Map<String, AttrValueConfidencePriority> res=resultMap.getOrDefault(key,null);
        if(res!=null) timeMap.put(key,time++);
        return res;
    }
    public void add(List<String> key, HashMap<String, AttrValueConfidencePriority> value){
        if(value==null||value.isEmpty()) return;
        if(resultMap.size()==max) {
            //从所有key中随机选择8个key，然后比较这8个key对应的value，选择最小的value，然后删除这个key
            renew(key);
        }else{
            keys.add(key);
        }
        resultMap.put(key,value);
        timeMap.put(key,time++);
    }

    public void renew(List<String> key){
        if(keysPtr==max){
            keysPtr=0;
            //其实可以用Collections.shuffle将表打乱
            //Collections.shuffle(keys);
        }
        //类似于打表，这样快
        //其实还可以存储8个key的value，避免多次查询，进一步优化
        //一共拿出8个元素比较，得到8个中最小的
        //这里是两两之间的第一层比较
        int l11=(timeMap.get(keys.get(keysPtr))>timeMap.get(keys.get(keysPtr+1)))?keysPtr+1:keysPtr;
        int l12=(timeMap.get(keys.get(keysPtr+2))>timeMap.get(keys.get(keysPtr+3)))?keysPtr+3:keysPtr+2;
        int l13=(timeMap.get(keys.get(keysPtr+4))>timeMap.get(keys.get(keysPtr+5)))?keysPtr+5:keysPtr+4;
        int l14=(timeMap.get(keys.get(keysPtr+6))>timeMap.get(keys.get(keysPtr+7)))?keysPtr+7:keysPtr+6;
        //第二层比较
        int l21=(timeMap.get(keys.get(l11))>timeMap.get(keys.get(l12)))?l12:l11;
        int l22=(timeMap.get(keys.get(l13))>timeMap.get(keys.get(l14)))?l14:l13;
        //第三层比较
        int l3=(timeMap.get(keys.get(l21))>timeMap.get(keys.get(l22)))?l22:l21;
        //淘汰掉l3对应的key
        resultMap.remove(keys.get(l3));
        timeMap.remove(keys.get(l3));
        //将新的key插入到keys的l3位置
        keys.set(l3,key);
        keysPtr+=8;
    }

}
