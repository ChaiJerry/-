package bundle_system.memory_query_system;

/**
 * 用于存储关联规则的后件和置信度
 * 包括了后件的属性名和属性值，便于后续的查询
 * 这是用于关联规则存储之中，都是常量值，不会改变
 * 和下面类中同一个包之中的AVCP类区别在于是该类用于恒定的存储，不会改变
 */
public class AssociationRuleConsResult {

    //后件，格式为 属性名：属性值
    private final String consequence;
    //后件中的属性名
    private final String attributeName;
    //后件中的属性值
    private final String attributeValue;
    //置信度
    private final double confidence;
    /**
     * 表示关联规则的后件和置信度
     * @param consequence 后件，输入格式为 属性名：属性值
     * @param confidence 置信度
     */
    public AssociationRuleConsResult(String consequence, double confidence) {
        this.consequence = consequence;
        this.confidence = confidence;
        String[] split = consequence.split(":");
        this.attributeName = split[1];
        this.attributeValue = split[2];
    }
    public String getConsequence() {
        return consequence;
    }

    public double getConfidence() {
        return confidence;
    }
    public String getAttributeName() {
        return attributeName;
    }
    public String getAttributeValue() {
        return attributeValue;
    }

}
