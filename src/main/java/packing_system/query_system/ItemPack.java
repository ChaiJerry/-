package packing_system.query_system;

import java.util.*;

public class ItemPack {

    //根据原始订单中机票信息推荐系统推荐的其它品类打包产品
    private final List<Set<String>> recommendedItems = new ArrayList<>(6);

    //原始订单之中和机票同时出现的其它品类的产品
    //i=0时是机票信息，可以不填用来占据空位
    private final List<Set<String>> orderItemsList = new ArrayList<>(6);

    public ItemPack() {
        //初始化
        for(int i=0;i<6;i++){
            recommendedItems.add(new HashSet<>());
            orderItemsList.add(new HashSet<>());
        }
    }

    //添加原始订单中的产品
    public void addOrderItem(List<String> item, int type){
        orderItemsList.get(type).addAll(item);
    }

    //添加推荐系统推荐的其它品类打包产品
    public void addRecommendedItem(String item, int type){
        recommendedItems.get(type).add(item);
    }

    //计算打包系统正确率的函数
    public double calculateAverageAccuracy(){
        double div = 5;
        double averageAccuracy=0;
        for(int i=1;i<6;i++){
            double accuracy = calculateAccuracy(i);
            if(accuracy == -1){
                div -= 1;
            }else{
                averageAccuracy += accuracy;
            }
        }
        if(div == 0){
            return -1;
        }
        return averageAccuracy/div;
    }


    //计算打包系统召回率的函数
    public double calculateAverageRecallRate(){
        double averageRecallRate=0;
        double div = 5;
        for(int i=1; i< 6; i++){
            //计算召回率
            double recallRate = calculateRecallRate(i);
            if(recallRate == -1){
                div -= 1;
            }else{
                averageRecallRate += recallRate;
            }
        }
        if(div==0){
            return -1;
        }
        return averageRecallRate/div;
    }

    //计算
    public double calculateAccuracy(int type){
        return calculate(type, recommendedItems);
    }

    public double calculateRecallRate(int type){
        return calculate(type, orderItemsList);
    }

    private double calculate(int type, List<Set<String>> itemsList) {
        double common=0;
        for(String item:recommendedItems.get(type)){
            if(orderItemsList.get(type).contains(item)){
                common++;
            }
        }
        if(itemsList.get(type).isEmpty()){
            return -1;
        }
        return common/ itemsList.get(type).size();
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
