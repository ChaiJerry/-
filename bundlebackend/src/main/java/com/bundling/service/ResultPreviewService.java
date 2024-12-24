package com.bundling.service;


import bundle_system.io.sql.*;
import com.bundling.*;
import com.bundling.vo.*;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;

import static bundle_system.io.SharedAttributes.*;
import static com.bundling.Application.*;

/**
 * 用于获取训练结果预览的Service类
 */
@Service
public class ResultPreviewService {

    public static void setSqlUtilsForTest(SQLUtils sqlUtils) {
        Application.sqlUtils = sqlUtils;
    }

    /**
     * 根据训练ID获取预览结果
     * @param tid 训练ID
     * @return 预览结果VO
     * @throws SQLException SQL异常
     */
    public AssociationRulesVO getPreviewByTrainId(int tid) throws SQLException {
        // 创建 AssociationRulesVO 对象用于存储最终结果
        AssociationRulesVO response = new AssociationRulesVO();
        // 创建一个 Map 用于存储不同类型的数据关联规则
        Map<String, List<AssociationRule>> rulesMap = new HashMap<>();
        // 遍历从 MEAL 到 SEAT 的所有类型
        for (int type = MEAL; type <= SEAT; type++) {
            // 加载指定类型的规则列表
            List<List<String>> lists = sqlUtils.loadRules(type, tid);
            // 创建一个 List 用于存储当前类型的关联规则
            List<AssociationRule> rules = new ArrayList<>();
            // 遍历规则列表
            for (int i = 0; i < lists.size(); i++) {
                // 创建一个新的 AssociationRule 对象
                AssociationRule rule = new AssociationRule();
                // 获取当前规则列表
                List<String> list = lists.get(i);
                // 解析前件部分
                String[] ates = list.get(0).split("; ");
                Map<String, String> antecedent = new HashMap<>();
                for (String at : ates) {
                    String[] att = at.split(":");
                    // att[0] 为字符串 Ticket，att[1] 为属性名, att[2] 为属性值
                    String attributeName = att[1];
                    String attributeValue = att[2];
                    antecedent.put(attributeName, attributeValue);
                }
                // 设置关联规则的前件
                rule.setAntecedent(antecedent);
                // 解析后件部分
                String consequence = list.get(1);
                String[] split = consequence.split("; ")[0].split(":");
                // 设置关联规则的属性名
                rule.setAttributeName(split[1]);
                // 设置关联规则的属性值
                rule.setAttributeValue(split[2]);
                // 设置关联规则的置信度
                rule.setConfidence(list.get(2).split("::")[1]);
                // 将解析后的关联规则添加到规则列表中
                rules.add(rule);
                // 对规则列表进行排序
                Collections.sort(rules);
            }
            // 将当前类型的规则列表添加到规则映射中
            rulesMap.put(getFullNames()[type].toLowerCase(), rules);
        }
        // 设置 AssociationRulesVO 对象的关联规则映射
        response.setAssociationRules(rulesMap);
        // 返回包含预览结果的 AssociationRulesVO 对象
        return response;
    }




}