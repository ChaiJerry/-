package query_system;

import java.util.*;

public class Evaluator {
    private Map<String, ItemPack> itemPackMap;
    private double averageAccuracy = 0;
    private double averageRecallRate = 0;
    private int correctRateDiv = itemPackMap.size();
    private int recallRateDiv = itemPackMap.size();
    public Evaluator(Map<String, ItemPack> itemPackMap) {
        this.itemPackMap = itemPackMap;
        for(ItemPack itemPack : itemPackMap.values()) {
            double aRate = itemPack.calculateAccuracy();
            double rRate = itemPack.calculateRecallRate();
            //-1说明是无效值，应当将相应的div减1
            if(aRate == -1) {
                correctRateDiv--;
            }else{
                averageAccuracy += aRate;
            }
            if(rRate == -1) {
                recallRateDiv--;
            }else {
                averageRecallRate += rRate;
            }
        }
        averageAccuracy /= correctRateDiv;
        averageRecallRate /= recallRateDiv;
    }

    public double getAverageAccuracy() {
        return averageAccuracy;
    }

    public double getAverageRecallRate() {
        return averageRecallRate;
    }


}
