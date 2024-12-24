package com.bundling.service;

import bundle_service_for_backend.*;
import bundle_system.memory_query_system.*;
import com.bundling.dto.*;
import com.bundling.vo.*;
import org.jetbrains.annotations.*;
import org.springframework.stereotype.*;

import java.io.*;
import java.util.*;

import static bundle_system.io.ConstItemAttributes.*;
import static bundle_system.io.SharedAttributes.*;
import static com.bundling.Application.*;

/**
 * 后端打包服务类，用于处理与打包相关的业务逻辑。
 * 包括了打包系统的初始化、产品列表的获取，打包体验等功能
 */
@Service
public class BundleService {

    /**
     * 用于打包的知识库的训练批次 ID，默认值为 -1。
     */
    public static int trainIdForBundle = -1;

    /**
     * 默认的后端打包系统实例。
     */
    public static BackendBundleSystem defaultBundleSystem = null;

    /**
     * 存储不同训练ID对应的后端打包系统的映射。
     */
    private final Map<String, BackendBundleSystem> bundleSystemMap = new HashMap<>();

    /**
     * 线程数，默认值为 8。
     */
    public static int threadNum = 8;

    /**
     * 根据请求获取打包的产品列表。
     *
     * @param request 打包请求 DTO 对象。
     * @return 包含不同类型产品的列表集合。
     * @throws IOException 如果发生 IO 异常。
     */
    @NotNull
    public List<List<ProductVO>> getBundleItemLists(BundleDTO request) throws IOException {
        String trainId = request.getTrain_id();
        BackendBundleSystem bundleSystem;

        // 检查是否已存在对应的 BackendBundleSystem 实例
        if (bundleSystemMap.containsKey(trainId)) {
            bundleSystem = bundleSystemMap.get(trainId);
        } else {
            // 创建新的 BackendBundleSystem 实例并存储到映射中
            bundleSystem = getBundleSystem(Integer.parseInt(trainId));
            bundleSystemMap.put(trainId, bundleSystem);
        }

        // 获取规则存储列表
        List<RulesStorage> rulesStorages = bundleSystem.getRulesStorages();

        // 构建查询属性
        Map<String, String> ateAttributes = new HashMap<>();
        // 设置机票的月份属性
        ateAttributes.put(MONTH, request.getMonth());
        // 设置机票的出发地和目的地属性
        ateAttributes.put(FROM, request.getFrom());
        ateAttributes.put(TO, request.getTo());
        // 设置机票的等级和是否有儿童属性
        ateAttributes.put(T_GRADE, request.getGrade());
        ateAttributes.put(HAVE_CHILD, request.getIs_child());
        // 设置促销率和机票原价属性（此处为了展示使用 "******" 作为占位符，实际值应根据请求动态获取）
        ateAttributes.put(PROMOTION_RATE, "******");
        ateAttributes.put(T_FORMER, "******");
        // 返回到前端的打包产品列表
        List<List<ProductVO>> productVOList = new ArrayList<>();
        // 获取演示使用的产品库中产品列表的副本
        List<List<Product>> productLists = Product.getProductListsCopy();

        // 遍历不同类型的产品（餐食、座位等）
        for (int type = MEAL; type <= SEAT; type++) {
            // 根据类型获取对应的规则存储对象（餐食、座位等）
            RulesStorage rulesStorage = rulesStorages.get(type);
            // 查询最佳规则并获取推荐属性
            Map<String, AttrValueConfidencePriority> stringAttrValueConfidencePriorityMap
                    = rulesStorage.queryBestRules(ateAttributes);
            // 去掉查到的规则中置信度等信息，只保留属性值
            Map<String, String> commendedAttributes = convertVCPToAttributes(stringAttrValueConfidencePriorityMap);
            // 将产品列表通过查询到的推荐属性过滤
            // 转换为 ProductVO 对象并添加到结果列表中
            productVOList.add(Product.getProductVOList(productLists.get(type), commendedAttributes));
        }
        // 返回给前端的打包产品列表集合
        return productVOList;
    }

    /**
     * 提交查询任务。
     *
     * @param str 查询字符串。
     * @return 查询结果字符串。
     * @throws Exception 如果发生异常。
     */
    public String api1(String str) throws Exception {
        return defaultBundleSystem.submitQueryTaskInStr(str);
    }

    /**
     * 提交打包任务。
     *
     * @param str 打包字符串。
     * @return 打包结果字符串。
     * @throws Exception 如果发生异常。
     */
    public String api2(String str) throws Exception {
        return defaultBundleSystem.submitBundleTaskInStr(str);
    }

    /**
     * 将 AttrValueConfidencePriority 映射转换为属性映射。
     *
     * @param vcpMap AttrValueConfidencePriority 映射。
     * @return 属性映射。
     */
    private Map<String, String> convertVCPToAttributes(Map<String, AttrValueConfidencePriority> vcpMap) {
        Map<String, String> map = new HashMap<>();
        // 使用 entrySet 遍历 vcpMap
        for (Map.Entry<String, AttrValueConfidencePriority> entry : vcpMap.entrySet()) {
            map.put(entry.getKey(), entry.getValue().getAttributeValue());
        }
        return map;
    }

    /**
     * 获取指定训练 ID 的 BackendBundleSystem 实例。
     *
     * @param tid 训练 ID。
     * @return BackendBundleSystem 实例。
     */
    public static BackendBundleSystem getBundleSystem(int tid) {
        // 初始化 BackendBundleSystem，并设置线程数为 8（建议为 CPU 核心数，可根据实际情况调整），也可以不设置使用默认值 16
        // 将 csvFileIO 和 sqlUtils 传入，然后随便写一个数字作为训练 id（这个数字是 trainId，在之后的后端打包系统中会用到，但这里用不到，不为 null 即可）
        return new BackendBundleSystem(threadNum, sqlUtils, tid);
    }

    /**
     * 设置默认的训练 ID 并初始化默认的 BackendBundleSystem 实例。
     *
     * @param train_id 训练 ID 字符串。
     * @return 成功或错误信息。
     */
    public static String setDefaultTrainId(String train_id) {
        try {
            trainIdForBundle = Integer.parseInt(train_id);
            defaultBundleSystem = getBundleSystem(trainIdForBundle);
            return "success";
        } catch (Exception e) {
            return "errorSetting";
        }
    }

    /**
     * 设置默认的 BackendBundleSystem 实例。
     *
     * @param bundleSystem BackendBundleSystem 实例。
     */
    public static void setDefaultBundleSystem(BackendBundleSystem bundleSystem) {
        defaultBundleSystem = bundleSystem;
    }
}



