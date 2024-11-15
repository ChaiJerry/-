package packing_system.io;

public class AssociationRuleResult {
    public String getConsequence() {
        return consequence;
    }

    public double getConfidence() {
        return confidence;
    }
    //后件，格式为属性名：属性值
    private String consequence;
    //后件中的属性名
    private String attributeName;
    public String getAttributeName() {
        return attributeName;
    }
    //后件中的属性值
    private String attributeValue;
    public String getAttributeValue() {
        return attributeValue;
    }

    /**
     * 表示关联规则的后件和置信度
     * @param consequence 后件，输入格式为属性名：属性值
     * @param confidence 置信度
     */
    public AssociationRuleResult(String consequence, double confidence) {
        this.consequence = consequence;
        this.confidence = confidence;
        String[] split = consequence.split(":");
        this.attributeName = split[1];
        this.attributeValue = split[2];
    }

    private double confidence;
}
