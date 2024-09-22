package packing_system.io;
import java.util.*;
import org.bson.*;


public class ItemAttributesStorage {
    public List<String> getAttributeNames() {
        return attributeNames;
    }

    //属性头列表
    private final List<String> attributeNames = new ArrayList<>();

    //添加属性头
    public void addAttribute(String header) {
        //如果属性头在属性头列表之中或是订单号，则跳过
        if (attributeNames.contains(header) || header.equals("ORDER_NO")) {
            return;
        }
        attributeNames.add(header);
    }

    public void addAttribute(String header,int index) {
        //如果属性头在属性头列表之中或是订单号，则跳过
        if (attributeNames.contains(header) || header.equals("ORDER_NO")) {
            return;
        }
        attributeNames.add(index,header);
    }

    //删除属性头
    public void removeAttribute(String header) {
        attributeNames.remove(header);
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
        for(String Header: attributeNames){
            //如果属性在属性头列表之中不存在，则添加一个空值
            doc.append(Header, headerMap.getOrDefault(Header,null));
        }
        return true;
    }

    public boolean getRulesDocument(List<Object> list , Document doc ,int eva) {
        Map<String, String> headerMap = new HashMap<>();
        String temp;
        for (int i = 0 ; i <list.size() ; i++) {
            Object s= list.get(i);
            temp = s.toString();
            //如果前件之中包含有一个非机票类型的属性，则跳过
            if (temp.charAt(0) != 'T') {
                return false;
            }
            String key= temp.split(":")[1];
            String value = temp.split(":")[2];
            headerMap.put(key, value);
        }
        String evaValue= headerMap.getOrDefault(attributeNames.get(eva),null);
        if(evaValue== null){
            return false;
        }else{
            doc.append(attributeNames.get(eva), evaValue);
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
        for(String Header: attributeNames){
            //如果属性在属性头列表之中不存在，则添加一个空值
            //如果属性在关联规则属性列表之中存在，则添加其对应的属性值
            doc.append(Header, headerMap.getOrDefault(Header,null));
        }
        return doc;
    }

    /**
     * 获得订单属性列表的有序且去除无用属性后的属性值列表
     * @param attributeList 订单属性列表，其中的String格式是"商品类型标识:属性名:属性值"
     * @return 返回属性值列表
     */
    public List<String> getOrderedAttributeValueList(List<String> attributeList) {
        Map<String, String> headerMap = new HashMap<>();
        List<String> attributeValueLists = new ArrayList<>();
        //遍历属性列表，获得属性和属性值
        for (String s : attributeList) {
            //划分属性名和属性值
            //这里的字符串格式是"商品类型标识:属性名:属性值"
            String key= s.split(":")[1];
            String value = s.split(":")[2];
            headerMap.put(key, value);
        }
        for(String Header: attributeNames){
            //如果属性在属性头列表之中不存在，则添加一个空值
            //如果属性在关联规则属性列表之中存在，则添加其对应的属性值
            attributeValueLists.add(headerMap.getOrDefault(Header,null));
        }
        return attributeValueLists;
    }

    /**
     * 从getOrderedAttributeValueList产生的有序属性值列表之中生成有属性名以及属性值的列表
     * @param attributeValueList 订单属性值列表，其中的String格式是"属性值"
     * @return 返回属性列表，其中的String格式是"属性名:属性值"
     */
    public List<String> generateOrderedAttributeListFromAttributeValueList(List<String> attributeValueList) {
        List<String> attributeLists = new ArrayList<>();
        //遍历属性值列表，为属性值列表添加属性名成为属性列表
        for(int i = 0;i < attributeValueList.size();i++){
            //按照顺序添加属性名和属性值
            attributeLists.add(attributeNames.get(i) + ":" + attributeValueList.get(i));
        }
        return attributeLists;
    }

    //！！！专为属性性能测试使用
    public List<String> generateOrderedAttributeListFromAttributeValueListForEva(List<String> attributeValueList) {
        List<String> attributeLists = new ArrayList<>();
        //遍历属性值列表，为属性值列表添加属性名成为属性列表
        for(int i = 0;i < attributeValueList.size()-1;i++){
            //按照顺序添加属性名和属性值
            attributeLists.add(attributeNames.get(i) + ":" + attributeValueList.get(i));
        }
        return attributeLists;
    }

    public List<String> generateOrderedAttributeListFromAttributeValueList(List<String> attributeValueList,int eva) {
        List<String> attributeLists = new ArrayList<>();
        attributeLists.add(attributeNames.get(eva) + ":" + attributeValueList.get(eva));
        return attributeLists;
    }

    public void clear(){
        attributeNames.clear();
    }
}
