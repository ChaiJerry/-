package packing_system.data_generating_system;

public class ConsequenceUnit {
    private String consequence;

    public String getConsequence() {
        return consequence;
    }

    public double getConfidence() {
        return confidence;
    }

    private double confidence;
    public ConsequenceUnit(String consequence,double confidence) {
        this.consequence = consequence;
        this.confidence = confidence;
    }

}
