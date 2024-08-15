package packing_system.io;
import java.util.*;
import org.bson.*;


public class ItemAttributesStorage {
    //属性头列表
    private List<String> attributes = new ArrayList<>();

    //添加属性头
    public void addAttribute(String header) {
        //如果属性头在属性头列表之中或是订单号，则跳过
        if (attributes.contains(header) || header.equals("ORDER_NO")) {
            return;
        }
        attributes.add(header);
    }

    //删除属性头
    public void removeAttribute(String header) {
        attributes.remove(header);
    }

    /**
     * 从关联规则属性列表，获得Document
     * @param list 关联规则属性列表
     * @param doc Document
     * @return 返回是否成功
     */
    public boolean getRulesDocument(List<Object> list , Document doc) {
        Map<String, String> headerMap = new HashMap<>();
        String temp;
        for (Object s : list) {
            temp = s.toString();
            //如果前件之中包含有一个非机票类型的属性，则跳过
            if (temp.charAt(0) != 'T') {
                return false;
            }
            String key= temp.split(":")[1];
            String value = temp.split(":")[2];
            headerMap.put(key, value);
        }
        for(String Header: attributes){
            //如果属性在属性头列表之中不存在，则添加一个空值
            doc.append(Header, headerMap.getOrDefault(Header,null));
        }
        return true;
    }

    /**
     * 获得频繁项集属性列表的Document
     * @param list 频繁项集属性列表
     * @return 返回Document
     */
    public Document getFrequentItemSetsDocument(List<String> list) {
        Map<String, String> headerMap = new HashMap<>();
        Document doc = new Document();
        //遍历属性列表，获得属性和属性值
        for (String s : list) {
            //划分属性名和属性值
            //这里的字符串格式是"商品类型标识:属性名:属性值"
            String key= s.split(":")[1];
            String value = s.split(":")[2];
            headerMap.put(key, value);
        }
        for(String Header: attributes){
            //如果属性在属性头列表之中不存在，则添加一个空值
            //如果属性在关联规则属性列表之中存在，则添加其对应的属性值
            doc.append(Header, headerMap.getOrDefault(Header,null));
        }
        return doc;
    }

    public List<String> getAttributeLists(List<String> list) {
        Map<String, String> headerMap = new HashMap<>();
        List<String> attributeLists = new ArrayList<>();
        //遍历属性列表，获得属性和属性值
        for (String s : list) {
            //划分属性名和属性值
            //这里的字符串格式是"商品类型标识:属性名:属性值"
            String key= s.split(":")[1];
            String value = s.split(":")[2];
            headerMap.put(key, value);
        }
        for(String Header: attributes){
            //如果属性在属性头列表之中不存在，则添加一个空值
            //如果属性在关联规则属性列表之中存在，则添加其对应的属性值
            attributeLists.add(headerMap.getOrDefault(Header,null));
        }
        return attributeLists;
    }

}
