package query_system;

import java.util.*;

public class ItemPack {
    private String key = null;

    //根据原始订单中机票信息推荐系统推荐的其它品类打包产品
    private String[] recommendedItems = new String[6];

    //原始订单之中和机票同时出现的其它品类的产品
    private List<Set<String>> orderItemsList = new ArrayList<>();

    public ItemPack() {
        //i=0时是机票信息，可以不填用来占据空位
        for(int i=0;i<6;i++){
            orderItemsList.add(new HashSet<>());
        }
    }

    public ItemPack(String key) {
        this.key = key;
        for(int i=0;i<6;i++){
            orderItemsList.add(new HashSet<>());
        }
    }

    //添加原始订单中的产品
    public void addOrderItem(String item, int type){
        orderItemsList.get(type).add(item);
    }

    //添加推荐系统推荐的其它品类打包产品
    public void addRecommendedItem(String item, int type){
        recommendedItems[type] = item;
    }

    //计算打包系统正确率的函数
    public double calculateAccuracy(){
        double div = 5;
        double Accuracy=0;
        for(int i=1;i<6;i++){
            if ((orderItemsList.get(i).isEmpty())) {
                div-=1;
            }else if(orderItemsList.get(i).contains(recommendedItems[i])){
                Accuracy++;
            }
        }
        if(div == 0){
            return -1;
        }
        return Accuracy/div;
    }

    //计算打包系统召回率的函数
    public double calculateRecallRate(){
        double recallRate=0;
        double div = 5;
        for(int i=1; i< 6; i++){
            //计算召回率
            if(orderItemsList.get(i).isEmpty()){
                div -= 1.0;
            }else if(orderItemsList.get(i).contains(recommendedItems[i])){
                recallRate +=  1.0 /orderItemsList.get(i).size();
            }
        }
        if(div==0){
            return -1;
        }
        return recallRate/div;
    }

    //得到每个机票属性特征键
    public static String generateItemPackKey(List<String> attributeValues){
        StringBuilder stringBuilder=new StringBuilder();
        for(int i = 0; i < attributeValues.size()-1;i++){
            stringBuilder.append(attributeValues.get(i)).append(",");
        }
        return stringBuilder.toString();
    }

}
