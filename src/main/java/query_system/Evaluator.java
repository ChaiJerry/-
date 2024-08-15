package query_system;

import java.util.*;

public class Evaluator {
    private double averageAccuracy = 0;
    private double averageRecallRate = 0;
    public Evaluator(Map<String, ItemPack> itemPackMap) {
        int correctRateDiv = itemPackMap.size();
        int recallRateDiv = itemPackMap.size();
        for(ItemPack itemPack : itemPackMap.values()) {
            double aRate = itemPack.calculateAverageAccuracy();
            double rRate = itemPack.calculateAverageRecallRate();
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
