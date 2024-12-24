package com.bundling.vo;

import java.util.List;
import java.util.Map;

/**
 * AssociationRulesVO 类用于封装一组关联规则。该类包含一个映射
 * ，其中键是一个小写的商品品类名标识符（如ticket)，
 * 值是与该标识符相关的关联规则列表。
 * 用于发送到前端展示的关联规则数据。
 */
public class AssociationRulesVO {
    /**
     * 关联规则的映射，键是一个小写的商品品类名标识符
     * ，值是与该标识符相关的关联规则列表。
     */
    private Map<String, List<AssociationRule>> associationRules;

    // Getters and Setters

    /**
     * 获取关联规则的映射。
     * @return 包含标识符和关联规则列表的映射。
     */
    public Map<String, List<AssociationRule>> getAssociationRules() {
        return associationRules;
    }

    /**
     * 设置关联规则的映射。
     *
     * @param associationRules 包含标识符和关联规则列表的映射。
     */
    public void setAssociationRules(Map<String, List<AssociationRule>> associationRules) {
        this.associationRules = associationRules;
    }
}



