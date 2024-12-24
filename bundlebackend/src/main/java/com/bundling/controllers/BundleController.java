package com.bundling.controllers;

import com.bundling.dto.BundleDTO;
import com.bundling.service.BundleService;
import com.bundling.vo.BundleVO;
import com.bundling.vo.ProductVO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

/**
 * BundleController 类是 Spring Boot 控制器，用于处理打包系统的 HTTP 请求。
 * 该控制器提供了多个接口来支持打包体验、查询可打包品类和查询打包结果等功能。
 */
@RestController
@RequestMapping("/api")
public class BundleController {
    // 注入服务层
    private final BundleService bundleService = new BundleService();

    /**
     * 打包体验接口。
     *
     * @param request 包含打包请求参数的 BundleDTO 对象。
     * @return 包含打包结果的 BundleVO 对象。
     * @throws IOException 如果在处理过程中发生 I/O 错误，则抛出此异常。
     */
    @PostMapping("/bundle")
    public BundleVO getBundle(@RequestBody BundleDTO request) throws IOException {
        List<List<ProductVO>> productsList = bundleService.getBundleItemLists(request);
        return new BundleVO(productsList);
    }

    /**
     * 查询可打包的品类接口。
     *
     * @param xmlData 包含查询请求的 XML 数据字符串。
     * @return 查询结果的字符串形式。
     * @throws Exception 如果在处理过程中发生任何异常，则抛出此异常。
     */
    @PostMapping("/bundle/search/rq")
    public String getBundleQuery(@RequestBody String xmlData) throws Exception {
        return bundleService.api1(xmlData);
    }

    /**
     * 查询打包结果接口。
     *
     * @param xmlData 包含查询请求的 XML 数据字符串。
     * @return 查询结果的字符串形式。
     * @throws Exception 如果在处理过程中发生任何异常，则抛出此异常。
     */
    @PostMapping("/bundle/search/rs")
    public String getBundleRes(@RequestBody String xmlData) throws Exception {
        return bundleService.api2(xmlData);
    }

    /**
     * 设置希望使用座位知识库的训练批次的 train_id 接口。
     *
     * @param trainId 训练批次的 ID。
     * @return 包含操作结果信息的 ResponseEntity 对象。
     */
    @PostMapping("/bundle/set_knowledgebase_for_bundle")
    public ResponseEntity<Map<String, String>> setKnowledgeBase(@RequestParam("train_id") String trainId) {
        if (trainId == null || trainId.isEmpty()) {
            Map<String, String> map = new HashMap<>();
            map.put("msg", "train_id is empty");
            return new ResponseEntity<>(map, HttpStatus.BAD_REQUEST);
        } else {
            Map<String, String> map = new HashMap<>();
            map.put("msg", BundleService.setDefaultTrainId(trainId));
            return new ResponseEntity<>(map, HttpStatus.OK);
        }
    }
}



