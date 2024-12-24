package com.bundling.vo;

import org.jetbrains.annotations.*;

import java.util.Map;

/**
 * AssociationRule 类表示数据挖掘中的关联规则，具体用于表示项目或属性之间的关系。
 * 该类包括前件、属性名、属性值、置信度以及置信度的双精度浮点数表示。
 */
public class AssociationRule implements Comparable<AssociationRule> {
    /**
     * 关联规则的前件部分，用一个键值对映射表示。
     * 例如，{"attribute1": "value1", "attribute2": "value2"}。
     */
    private Map<String, String> antecedent;

    /**
     * 后件部分涉及的属性名称。
     */
    private String attributeName;

    /**
     * 后件部分涉及的属性值。
     */
    private String attributeValue;

    /**
     * 关联规则的置信度，以字符串形式表示。置信度表示在前件成立的情况下后件成立的可能性。
     */
    private String confidence;

    /**
     * 关联规则的置信度，以双精度浮点数形式表示，便于比较和计算。
     */
    private double conf;

    // Getters and Setters

    /**
     * 获取关联规则的前件部分。
     *
     * @return 表示前件的键值对映射。
     */
    public Map<String, String> getAntecedent() {
        return antecedent;
    }

    /**
     * 设置关联规则的前件部分。
     *
     * @param antecedent 表示前件的键值对映射。
     */
    public void setAntecedent(Map<String, String> antecedent) {
        this.antecedent = antecedent;
    }

    /**
     * 获取后件部分涉及的属性名称。
     *
     * @return 属性名称字符串。
     */
    public String getAttributeName() {
        return attributeName;
    }

    /**
     * 设置后件部分涉及的属性名称。
     *
     * @param attributeName 属性名称字符串。
     */
    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    /**
     * 获取后件部分涉及的属性值。
     *
     * @return 属性值字符串。
     */
    public String getAttributeValue() {
        return attributeValue;
    }

    /**
     * 设置后件部分涉及的属性值。
     *
     * @param attributeValue 属性值字符串。
     */
    public void setAttributeValue(String attributeValue) {
        this.attributeValue = attributeValue;
    }

    /**
     * 获取关联规则的置信度（字符串形式）。
     *
     * @return 置信度字符串。
     */
    public String getConfidence() {
        return confidence;
    }

    /**
     * 设置关联规则的置信度（字符串形式），并更新其双精度浮点数表示。
     *
     * @param confidence 置信度字符串。
     */
    public void setConfidence(String confidence) {
        this.confidence = confidence;
        this.conf = Double.parseDouble(confidence);
    }

    /**
     * 根据置信度比较两个关联规则的顺序。
     * 返回负整数、零或正整数分别表示此对象的置信度大于、等于或小于指定对象的置信度。
     *
     * @param o 要比较的其他关联规则。
     * @return 表示相对顺序的整数。
     */
    @Override
    public int compareTo(@NotNull AssociationRule o) {
        return Double.compare(o.conf, this.conf);
    }
}



