package com.bundling.service;

import bundle_system.io.sql.*;
import com.bundling.vo.*;
import org.junit.jupiter.api.Test;

import static bundle_system.io.SharedAttributes.*;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.*;
import java.util.*;

/**
 * 测试类用于验证ResultPreviewService的服务功能，特别是与训练结果预览相关的服务。
 * 该类中的测试方法主要用于确保根据给定的列车ID加载到正确的关联规则预览，
 * 并且在没有关联规则的情况下也能够正确处理。
 */
class ResultPreviewServiceTest {

    /**
     * 测试当提供的训练ID对应的关联规则为空时，ResultPreviewService的行为。
     * 预期：对于每个类别（从MEAL到SEAT），应该存在一个空的关联规则列表。
     *
     * @throws SQLException 如果与数据库交互过程中发生SQL异常。
     */
    @Test
    void testResultPreviewServiceWithEmptyRules() throws SQLException {
        // 设置用于测试的SQL工具实例，以便模拟或控制数据库交互。
        ResultPreviewService.setSqlUtilsForTest(new SQLUtils());

        // 创建待测试的服务实例。
        ResultPreviewService resultPreviewService = new ResultPreviewService();

        // 获取由训练ID '1' 确定的关联规则预览。
        AssociationRulesVO previewByTrainId = resultPreviewService.getPreviewByTrainId(1);

        // 断言返回的预览对象非空。
        assertNotNull(previewByTrainId);

        // 获取所有关联规则。
        Map<String, List<AssociationRule>> associationRules = previewByTrainId.getAssociationRules();

        // 对于每一个类别（从MEAL到SEAT），检查其是否存在于映射中，并且其列表大小为0。
        for(int i=MEAL; i<=SEAT;i++){
            assertTrue(associationRules.containsKey(getFullNames()[i].toLowerCase()));
            assertEquals(0, associationRules.get(getFullNames()[i].toLowerCase()).size());
        }
    }

    /**
     * 测试ResultPreviewService以验证它能否正确地返回给定训练ID对应的非空关联规则。
     * 本测试假定已知特定训练ID '51' 的关联规则数量，以此来验证服务返回的数据准确性。
     *
     * @throws SQLException 如果与数据库交互过程中发生SQL异常。
     */
    @Test
    void testResultPreviewService() throws SQLException {
        // 设置用于测试的SQL工具实例，以便模拟或控制数据库交互。
        ResultPreviewService.setSqlUtilsForTest(new SQLUtils());

        // 定义每个类别（从索引0开始）预期的关联规则数目。
        int[] rulesNum ={0,0,246,240,380,120};

        // 创建待测试的服务实例。
        ResultPreviewService resultPreviewService = new ResultPreviewService();

        // 获取由训练ID '51' 确定的关联规则预览。
        AssociationRulesVO previewByTrainId = resultPreviewService.getPreviewByTrainId(51);

        // 断言返回的预览对象非空。
        assertNotNull(previewByTrainId);

        // 获取所有关联规则。
        Map<String, List<AssociationRule>> associationRules = previewByTrainId.getAssociationRules();

        // 对于每一个类别（从MEAL到SEAT），检查其是否存在于映射中，并且其列表大小等于预期的规则数。
        for(int i=MEAL; i<=SEAT;i++){
            assertTrue(associationRules.containsKey(getFullNames()[i].toLowerCase()));
            assertEquals(rulesNum[i], associationRules.get(getFullNames()[i].toLowerCase()).size());
        }
    }
}