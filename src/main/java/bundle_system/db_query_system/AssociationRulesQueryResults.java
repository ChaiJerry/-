package bundle_system.db_query_system;

public class AssociationRulesQueryResults {
    private String consequence;
    private double confidence;

    public AssociationRulesQueryResults(String consequence,double confidence) {
        this.consequence = consequence;
        this.confidence = confidence;
    }

    public String getConsequence() {
        return consequence;
    }

    public void setConsequence(String consequence) {
        this.consequence = consequence;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }
}
