package bundle_service_for_backend;

import bundle_service_for_backend.xml_parser.*;
import bundle_system.memory_query_system.*;
import org.w3c.dom.*;

import java.util.*;

public class BundleMethods {
    /**
     * 套餐/行李打包方法（测试版本），因为这两个比较像，所以合并了
     *
     * @param ticketInfo    机票航段 商品键值对
     * @param bundleItems   附加产品所属航段 附加产品键值对
     * @param rulesStorage  附加产品规则存储
     * @param fatherElement 父节点
     * @param doc           最终返回到Document，这里用来创造节点
     */
    public static Element bundleMeal(Map<String, BundleItem> ticketInfo
            , Map<String, List<BundleItem>> bundleItems
            , RulesStorage rulesStorage, Element fatherElement, Document doc) {
        Element ancillary = doc.createElement("Ancillary");
        Element boundProducts = doc.createElement("BoundProducts");
        Element ancillaryProducts = doc.createElement("AncillaryProducts");
        ancillary.appendChild(boundProducts);
        boundProducts.appendChild(ancillaryProducts);
        //遍历ticketInfo，得到其中的机票属性
        for (Map.Entry<String, BundleItem> entry : ticketInfo.entrySet()) {
            //根据机票属性查询附加产品规则，得到附加产品属性
            Map<String, AttrValueConfidencePriority> map = rulesStorage.queryItemAttributes(entry.getValue().getAttributes());
            //根据机票属性查询附加产品航段，得到附加产品航段的商品键值对
            List<BundleItem> bundleItemList = bundleItems.get(entry.getKey());
            //排序
            BackendBundleSystem.setPriorityAndSort(map, bundleItemList);
            //将排序好的附加产品添加到节点中
            for (int i = 0, size = bundleItemList.size(); i < size && i < 5; i++) {
                BundleItem bundleItem = bundleItemList.get(i);
                // 将附加产品添加到节点中（这里是用的是破坏性迁移，效率更高，会直接修改原来的doc）
                ancillaryProducts.appendChild(bundleItem.getElement());
            }
        }
        return ancillaryProducts;
    }

    /**
     * 套餐/行李打包方法（测试版本），因为这两个比较像，所以合并了
     *
     * @param ticketInfo    机票航段 商品键值对
     * @param bundleItems   附加产品所属航段 附加产品键值对
     * @param rulesStorage  附加产品规则存储
     * @param fatherElement fatherElement节点，大多数时候为null，主要是为了作为和餐食在一个父节点下设计的
     * @param doc           输出的Document
     */
    public static Element bundleBaggage(Map<String, BundleItem> ticketInfo
            , Map<String, List<BundleItem>> bundleItems
            , RulesStorage rulesStorage, Element fatherElement, Document doc) {
        Element ancillary = doc.createElement("Ancillary");
        Element baggage = doc.createElement("Baggage");
        ancillary.appendChild(baggage);
        Element originDestination = doc.createElement("OriginDestination");
        baggage.appendChild(originDestination);
        //遍历ticketInfo，得到其中的机票属性
        for (Map.Entry<String, BundleItem> entry : ticketInfo.entrySet()) {
            //根据机票属性查询附加产品规则，得到附加产品属性
            Map<String, AttrValueConfidencePriority> map = rulesStorage.queryItemAttributes(entry.getValue().getAttributes());
            //根据机票属性查询附加产品航段，得到附加产品航段的商品键值对
            List<BundleItem> bundleItemList = bundleItems.get(entry.getKey());
            //排序
            BackendBundleSystem.setPriorityAndSort(map, bundleItemList);
            //将排序好的附加产品添加到节点中
            for (int i = 0, size = bundleItemList.size(); i < size && i < 5; i++) {
                BundleItem bundleItem = bundleItemList.get(i);
                //将附加产品添加到节点中
                originDestination.appendChild(bundleItem.getElement());
            }
        }
        return ancillary;
    }

    /**
     * 套餐/行李打包方法（测试版本），因为这两个比较像，所以合并了
     *
     * @param ticketInfo    机票航段 商品键值对
     * @param bundleItems   附加产品所属航段 附加产品键值对，这里的键为 航段号|subtype
     * @param rulesStorage  附加产品规则存储
     * @param fatherElement fatherElement节点，大多数时候为null，主要是为了选座和餐食在一个父节点下设计的
     * @param doc           输出的Document
     */
    public static Element bundleSeat(Map<String, BundleItem> ticketInfo
            , Map<String, List<BundleItem>> bundleItems
            , RulesStorage rulesStorage, Element fatherElement, Document doc) {
        Map<String, Map<String, AttrValueConfidencePriority>> segAttributesmap = new HashMap<>();
        //遍历ticketInfo，得到其中的机票属性
        for (Map.Entry<String, BundleItem> entry : ticketInfo.entrySet()) {
            //根据机票属性查询附加产品规则，得到附加产品属性
            Map<String, AttrValueConfidencePriority> map = rulesStorage.queryItemAttributes(entry.getValue().getAttributes());
            //预处理属性map，将座位列和subType对应
            preProcessSeatNoTOSubType(map);
            segAttributesmap.put(entry.getKey(), map);
        }
        //遍历bundleItems，得到其中的附加产品属性
        for (Map.Entry<String, List<BundleItem>> entry : bundleItems.entrySet()) {
            //根据机票属性查询附加产品航段，得到附加产品航段的商品键值对
            List<BundleItem> bundleItemList = entry.getValue();
            String segRef = entry.getKey().split("\\|")[0];
            //排序
            BackendBundleSystem.setPriorityAndSort(segAttributesmap.get(segRef), bundleItemList);
            //将排序好的附加产品添加到节点中
            for (int i = 0, size = bundleItemList.size(); i < size; i++) {
                BundleItem bundleItem = bundleItemList.get(i);
                //将附加产品添加到节点中
                fatherElement.appendChild(BackendBundleSystem.buildSeatElement(bundleItem, doc));
            }
        }
        return (Element) fatherElement.getParentNode().getParentNode();
    }

    private static void preProcessSeatNoTOSubType(Map<String, AttrValueConfidencePriority> map) {
        String seatNo;
        AttrValueConfidencePriority attrValueConfidencePriority = map.get("SEAT_NO");
        map.remove("SEAT_NO");
        if (attrValueConfidencePriority == null) {
            return;
        }
        seatNo= attrValueConfidencePriority.getAttributeValue();
        map.put("SubType", attrValueConfidencePriority);
        //靠窗座位：A（左）, K（右）——适用于所有飞机；
        //
        //靠走廊座位：C, D, G, H ——适用于双通道飞机；C, H——适用于单通道飞机。
        if (seatNo.equals("K") || seatNo.equals("A")) {
            attrValueConfidencePriority.setAttributeValue("85");
        } else if (seatNo.equals("C") || seatNo.equals("D")
                || seatNo.equals("G") || seatNo.equals("H")) {
            attrValueConfidencePriority.setAttributeValue("3");
        } else {
            attrValueConfidencePriority.setAttributeValue("");
        }

    }

    /**
     * 保险打包方法（测试版本）
     *
     * @param ticketInfo   机票航段 商品键值对
     * @param bundleItems  保险所属航段 保险键值对
     * @param rulesStorage 保险规则存储
     * @return 返回打包后排序好的结果对应Insurance节点
     */
    public static Element testBundleInsurance(Map<String, BundleItem> ticketInfo
            , Map<String, List<BundleItem>> bundleItems
            , RulesStorage rulesStorage, Element fatherElement, Document doc) {
        Element insurance = doc.createElement("Insurance");
        //遍历ticketInfo，得到其中的机票属性
        for (Map.Entry<String, BundleItem> entry : ticketInfo.entrySet()) {
            //根据机票属性查询附加产品规则，得到附加产品属性
            Map<String, AttrValueConfidencePriority> map = rulesStorage.queryItemAttributes(entry.getValue().getAttributes());
            //根据机票属性查询附加产品航段，得到附加产品航段的商品键值对
            List<BundleItem> bundleItemList = bundleItems.get(null);
            //排序
            BackendBundleSystem.setPriorityAndSort(map, bundleItemList);
            for (int i = 0, size = bundleItemList.size(); i < size && i < 5; i++) {
                BundleItem bundleItem = bundleItemList.get(i);
                //将排序好的附加产品添加到节点中
                insurance.appendChild(bundleItem.getElement());
            }
            break;
        }
        return insurance;
    }
}
