package xml_parser;

import bundle_system.data_processer.*;
import org.w3c.dom.*;

import javax.xml.xpath.*;
import java.util.*;

import static bundle_system.data_processer.DataConverter.*;
import static bundle_system.io.SharedAttributes.*;

public class XMLParser {
    private final XPath xpath;

    public List<ParseMethod> getParseMethods() {
        return parseMethodMethods;
    }

    private final List<ParseMethod> parseMethodMethods = new ArrayList<>();

    public XMLParser(XPath xpath) {
        parseMethodMethods.add(null);
        parseMethodMethods.add(null);
        parseMethodMethods.add(this::parseMeal);
        parseMethodMethods.add(this::parseBaggage);
        parseMethodMethods.add(this::parseInsurances);
        parseMethodMethods.add(this::parseSeat);
        this.xpath = xpath;
    }

    /**
     * 解析XML文件，得到comboSource
     *
     * @param root XML文件的根节点
     * @return comboSource中的航班属性
     */
    public Map<String, BundleItem> parseComboSource(Element root) throws XPathExpressionException {
        // 创建一个HashMap来存储解析后的结果,key为航段，value为存储航班属性的item
        // 可以用List<BundleItem>而不是直接用BundleItem是和后面的方法统一格式，这样可以方便后续的多线程优化
        Map<String, BundleItem> comboSourceMap = new HashMap<>();
        long startTime = System.currentTimeMillis();

        String ODXpath = "/OJ_ComboSearchRS/ComboSource/PricedItinerary/AirItinerary/OriginDestinationOptions/OriginDestinationOption";
        NodeList OriginDestinationOption = (NodeList) xpath.evaluate(ODXpath, root, XPathConstants.NODESET);
        for (int i = 0; i < OriginDestinationOption.getLength(); i++) {
            // 得到RPH
            Element originDestinationOption = (Element) OriginDestinationOption.item(i);
            String rph = originDestinationOption.getAttribute("RPH");
            // 创建BundleItem对象
            BundleItem bundleItem = new BundleItem(rph);
            // 将bundleItem添加到map中
            comboSourceMap.put(rph, bundleItem);
            // 得到出发时间
            Element flightSegment = getElementByRelativePath(originDestinationOption, "FlightSegment");
            String month = flightSegment.getAttribute("ArrivalDateTime").split("-")[1];
            bundleItem.addAttributeNameValuePair("MONTH", month);
            // 得到出发地和目的地
            Element departureAirport = getElementByRelativePath(flightSegment, "DepartureAirport");
            String departureLocationCode = departureAirport.getAttribute("LocationCode");
            // 添加到bundleItem中（为了和训练中的属性名称一致，这里用FROM和TO）
            bundleItem.addAttributeNameValuePair("FROM", departureLocationCode);
            Element arrivalAirport = getElementByRelativePath(flightSegment, "ArrivalAirport");
            String arrivalLocationCode = arrivalAirport.getAttribute("LocationCode");
            bundleItem.addAttributeNameValuePair("TO", arrivalLocationCode);
        }
        //如果要极优化，可以储存之前访问过的节点，然后用相对路径来访问
        String AirItineraryPricingInfoXpath = "/OJ_ComboSearchRS/ComboSource/PricedItinerary/AirItineraryPricingInfo";
        NodeList airItineraryPricingInfos = (NodeList) xpath.evaluate(AirItineraryPricingInfoXpath, root, XPathConstants.NODESET);
        for (int i = 0; i < airItineraryPricingInfos.getLength(); i++) {
            Element airItineraryPricingInfo = (Element) airItineraryPricingInfos.item(i);
            // 判断其子节点是否为空，若非空则开始解析，否则跳过
            if (airItineraryPricingInfo.hasChildNodes()) {
                // 得到旅客类型
                String passengerTypeQuantityXpath = "PTC_FareBreakdowns/PTC_FareBreakdown/PassengerTypeQuantity";
                Element typeElement = (Element) xpath.evaluate(passengerTypeQuantityXpath, airItineraryPricingInfo, XPathConstants.NODE);
                String isChild = typeElement.getAttribute("Code").equals("ADT") ? "0" : "1";
                for (BundleItem bundleItem : comboSourceMap.values()) {
                    // 加入旅客类型属性，可以为字符串0或1以匹配训练数据
                    bundleItem.addAttributeNameValuePair("HAVE_CHILD", isChild);
                }
                // 得到价格
                String fareInfoXpath = "FareInfos/FareInfo";
                NodeList fareInfoElement = (NodeList) xpath.evaluate(fareInfoXpath, airItineraryPricingInfo, XPathConstants.NODESET);
                for (int j = 0; j < fareInfoElement.getLength(); j++) {
                    Element fareInfo = (Element) fareInfoElement.item(j);
                    // 得到航段RPH
                    String flightSegmentRPH = fareInfo.getAttribute("FlightSegmentRPH");
                    BundleItem bundleItem = comboSourceMap.get(flightSegmentRPH);
                    if (bundleItem == null) {
                        throw new RuntimeException("ComboSource中前后航段信息不匹配！");
                    }

                    // 得到舱位等级
                    Element fareReference = getElementByRelativePath(fareInfo, "FareReference");
                    String grade = ticketGrade2Specific(fareReference.getAttribute("CabinCode"));
                    bundleItem.addAttributeNameValuePair("T_GRADE", grade);

                    // 得到折扣
                    Element info = getElementByRelativePath(fareInfo, "FareInfo");
                    String discount = info.getAttribute("DisCount");
                    String promotionRateGrade = (((int) (100 - Double.parseDouble(discount) * 100 + LITTLE_DOUBLE)) / 10) + "";

                    bundleItem.addAttributeNameValuePair("PROMOTION_RATE", promotionRateGrade);

                    // 得到折扣前价格
                    Element fare = getElementByRelativePath(info, "Fare");
                    String baseCabinClassAmount = fare.getAttribute("BaseCabinClassAmount");
                    Integer priceGrade = DataParser.floatStr2Attribute(baseCabinClassAmount, 1000);
                    bundleItem.addAttributeNameValuePair("T_FORMER", priceGrade + "");
                }
                break;
            }
        }
        //System.out.println("时间（ms）："+(System.currentTimeMillis() - startTime));
        return comboSourceMap;
    }

    /**
     * 解析XML文件，得到保险信息
     * @param root XML文件的根节点
     * @return comboWith中的保险信息（包括属性和Element），map中的key为null（因为保险按理来说覆盖两边）
     * ，对应的值为列表对应着一个需要单独打包的BundleItem序列，这里这么做是为了方便统一遍历
     */
    public Map<String,List<BundleItem>> parseInsurances(Element root) throws XPathExpressionException {
        List<BundleItem> bundleItems = new ArrayList<>();
        String planForQuoteRSXpath = "/OJ_ComboSearchRS/ComboWith/Insurance/PlanForQuoteRS";
        NodeList planForQuoteRSXs = (NodeList) xpath.evaluate(planForQuoteRSXpath, root, XPathConstants.NODESET);
        for (int i = 0; i < planForQuoteRSXs.getLength(); i++) {
            Element planForQuoteRSX = (Element) planForQuoteRSXs.item(i);
            BundleItem bundleItem = new BundleItem(null, planForQuoteRSX.cloneNode(true));
            bundleItems.add(bundleItem);
            // 得到保险的名称
            String name = planForQuoteRSX.getAttribute("Name");
            bundleItem.addAttributeNameValuePair("INSUR_PRO_NAME", name);

            // 得到保险的金额
            Element PlanCost = getElementByRelativePath(planForQuoteRSX, "PlanCost");
            String amount = Integer.parseInt(PlanCost.getAttribute("Amount").split("\\.")[0]) + ".00";
            bundleItem.addAttributeNameValuePair("INSUR_AMOUNT", amount);

            //得到保险的公司代码
            String providerCompanyXpath = "QuoteDetail/ProviderCompany";
            Element providerCompany = (Element) xpath.evaluate(providerCompanyXpath, planForQuoteRSX, XPathConstants.NODE);
            String companyCode = providerCompany.getAttribute("Code");
            bundleItem.addAttributeNameValuePair("INSURANCE_COMPANYCODE", companyCode);
        }
        Map<String,List<BundleItem>> bundleItemsMap = new HashMap<>();
        bundleItemsMap.put(null, bundleItems);
        return bundleItemsMap;
    }

    /**
     * 解析XML文件，得到行李信息
     * @param root XML文件的根节点
     * @return comboWith中的行李信息（包括属性和Element），map中的key为航段，对应的值为列表对应着一个需要单独打包的BundleItem序列
     */
    public Map<String,List<BundleItem>> parseBaggage(Element root) throws XPathExpressionException {
        Map<String,List<BundleItem>> bundleItemsMap = new HashMap<>();
        String baggageServiceXpath = "/OJ_ComboSearchRS/ComboWith/Ancillary[1]/Baggage/OriginDestination/Service";
        NodeList services = (NodeList) xpath.evaluate(baggageServiceXpath, root, XPathConstants.NODESET);
        for (int i = 0; i < services.getLength(); i++) {
            Element service = (Element) services.item(i);
            // 得到航段
            String baggageXpath = "Baggage";
            Element baggage = (Element) xpath.evaluate(baggageXpath, service, XPathConstants.NODE);
            String SegmentRef = baggage.getAttribute("SegmentRef");
            BundleItem bundleItem = new BundleItem(SegmentRef, service.cloneNode(true));
            // 判断是否航段对应的列表已经存在
            // 若不存在则新建一个列表添加到map中
            if (!bundleItemsMap.containsKey(SegmentRef)) {
                List<BundleItem> bundleItems = new ArrayList<>();
                bundleItems.add(bundleItem);
                bundleItemsMap.put(SegmentRef, bundleItems);
            }else{
                bundleItemsMap.get(SegmentRef).add(bundleItem);
            }
            // 得到重量
            String maxWeightXpath = "MaxWeight";
            Element maxWeight = (Element) xpath.evaluate(maxWeightXpath, baggage, XPathConstants.NODE);
            String weight = maxWeight.getTextContent() + "KG";
            bundleItem.addAttributeNameValuePair("BAGGAGE_SPECIFICATION", weight);

            //得到Amount
            String totalXpath = "Prices/Price/Total";
            Element total = (Element) xpath.evaluate(totalXpath, baggage, XPathConstants.NODE);
            String amount = DataParser.floatStr2Attribute(total.getAttribute("Amount"), 200) + "";
            bundleItem.addAttributeNameValuePair("PAYMENTAMOUNT", amount);
        }
        return bundleItemsMap;
    }

    /**
     * 解析XML文件，得到餐食信息
     * @param root XML文件的根节点
     * @return comboWith中的餐食信息（包括属性和Element），返回的map中的key为航段，对应的值为列表对应着一个需要单独打包的BundleItem序列
     */
    public Map<String,List<BundleItem>> parseMeal(Element root) throws XPathExpressionException {
        Map<String,List<BundleItem>> bundleItemsMap = new HashMap<>();
        String ancillaryXpath = "/OJ_ComboSearchRS/ComboWith/Ancillary[2]/BoundProducts/AncillaryProducts/Ancillary";
        NodeList ancillaries = (NodeList) xpath.evaluate(ancillaryXpath, root, XPathConstants.NODESET);
        for (int i = 0; i < ancillaries.getLength(); i++) {
            Element ancillary = (Element) ancillaries.item(i);
            String segmentRef = ancillary.getAttribute("SegmentRef");
            BundleItem bundleItem = new BundleItem(segmentRef, ancillary.cloneNode(true));
            if (!bundleItemsMap.containsKey(segmentRef)) {
                List<BundleItem> bundleItems = new ArrayList<>();
                bundleItems.add(bundleItem);
                bundleItemsMap.put(segmentRef, bundleItems);
            }else{
                bundleItemsMap.get(segmentRef).add(bundleItem);
            }
            // 得到餐食代码
            String supplierProductCode = ancillary.getAttribute("SupplierProductCode");
            bundleItem.addAttributeNameValuePair("MEAL_CODE", supplierProductCode);
            // 得到餐食价格
            String baseXpath = "Prices/Price/Base";
            Element base = (Element) xpath.evaluate(baseXpath, ancillary, XPathConstants.NODE);
            String amount = DataParser.floatStr2Attribute(base.getAttribute("Amount"), 4) + "";
            bundleItem.addAttributeNameValuePair("PM_PRICE", amount);
        }
        return bundleItemsMap;
    }

    /**
     * 解析XML文件，得到选座信息
     * @param root XML文件的根节点
     * @return comboWith中的选座信息（包括属性和Element），map中的key为"航段|subType"，对应的值为列表对应着一个需要单独打包的BundleItem序列
     */
    public Map<String,List<BundleItem>> parseSeat(Element root) throws XPathExpressionException {
        Map<String,List<BundleItem>> bundleItemsMap = new HashMap<>();
        String seatMapResponseXpath = "/OJ_ComboSearchRS/ComboWith/OJ_AirSeatMapRS/Product/SeatMapResponse";
        NodeList seatMapResponses = (NodeList) xpath.evaluate(seatMapResponseXpath, root, XPathConstants.NODESET);
        // 得到座位信息，另外作为需要重构xml的数据格式
        //<!-- 座位返回 -->
        //使用XSD中的Tag放入推荐标识? 推荐标识应在同一类型的航段下唯
        //座位组包信息由下面三个字段联合主键返回
        //  SegmentRef：航段序号 1或2.对应请求xpath(OJ_AirSeatMapRS/Product/SeatMapResponse/@REF)
        //  SubType：85代表是靠窗，71代表是前排，3代表过道，17代表紧急出口，空串""代表普通座位.对应请求xpath(OJ_AirSeatMapRS/Product/SeatMapResponse/AAM_SeatMap/Cabin/Row/Block/@Characteristics)
        //  SupplierProductCode：价格级别 L或Q. 对应请求xpath(OJ_AirSeatMapRS/Product/SeatMapResponse/AAM_SeatMap/Cabin/Row/Block/@ProductCode)
        //每个航段的每个SubType类型，都需要返回完整的价格级别数据，
        //航段1 靠窗 SupplierProductCode="L"
        //航段1 靠窗 SupplierProductCode="Q" 以此类推
        for (int i = 0; i < seatMapResponses.getLength(); i++) {
            Element seatMapResponse = (Element) seatMapResponses.item(i);
            String segmentRef = seatMapResponse.getAttribute("REF");
            // 座位价格和货币代码的map，key为座位等级，对应的值为价格和货币代码,String[0]为Amount、String[0]为CurrencyCode
            Map<String, String[]> priceAndCurrencyCode = parseSeatPriceAndCurrencyCode(seatMapResponse);
            if (priceAndCurrencyCode.isEmpty()) {
                continue;
            }
            // 避免重复添加，这里存储的是主键
            Set<String> haveVisited = new HashSet<>();
            // 解析座位图
            NodeList Rows = getElementsByRelativePath(seatMapResponse, "AAM_SeatMap/Cabin/Row");
            for (int j = 0; j < Rows.getLength(); j++) {
                Element Row = (Element) Rows.item(j);
                String subType = Row.getAttribute("Characteristics");
                NodeList Blocks = getElementsByRelativePath(Row, "Block");
                for (int k = 0; k < Blocks.getLength(); k++) {
                    Map<String,String> xmlAttributes = new HashMap<>();
                    Element Block = (Element) Blocks.item(k);
                    String supplierProductCode = Block.getAttribute("ProductCode");
                    if (supplierProductCode.isEmpty() || haveVisited.contains(subType+"|"+supplierProductCode)) {
                        continue;
                    }

                    //优化点： 不用每次都查询，只有需要组包的5个座位才查询
                    String[] amountAndCurrencyCode = priceAndCurrencyCode.get(supplierProductCode);
                    String seatNo = Block.getAttribute("Number");
                    BundleItem bundleItem = new BundleItem(segmentRef, seatMapResponse.cloneNode(true));
                    //TODO 处理null值情况，判断avail是否为3（是否可用）
                    putBundleItemToMap(segmentRef + "|" + subType, bundleItem, bundleItemsMap);
                    // 座位等级
                    bundleItem.addAttributeNameValuePair("SEAT_NO", seatNo);
                    // 加入用于组包的属性
                    xmlAttributes.put("SubType", subType);
                    xmlAttributes.put("SegmentRef", segmentRef);
                    xmlAttributes.put("SupplierProductCode", supplierProductCode);
                    xmlAttributes.put("Amount", amountAndCurrencyCode[0]);
                    xmlAttributes.put("CurrencyCode", amountAndCurrencyCode[1]);
                    bundleItem.setXmlAttributes(xmlAttributes);
                    // 将已经访问过的主键添加到集合中
                    haveVisited.add(subType+"|"+supplierProductCode);
                }
            }
        }
        return bundleItemsMap;
    }

    /**
     * 将BundleItem放入map中
     * @param key 键（可以是航段或者"航段|subType"等）
     * @param bundleItem 需要加入的BundleItem
     * @param map 对应的map
     */
    private void putBundleItemToMap(String key, BundleItem bundleItem, Map<String, List<BundleItem>> map) {
        if (!map.containsKey(key)) {
            List<BundleItem> bundleItems = new ArrayList<>();
            bundleItems.add(bundleItem);
            map.put(key, bundleItems);
        } else {
            map.get(key).add(bundleItem);
        }
    }

    /**
     * 解析XML文件，得到座位价格和货币代码
     * @param seatMapResponse 包含座位信息的上级节点
     * @return 座位价格和货币代码，map中的key为座位等级，对应的值为价格和货币代码,String[0]为Amount、String[0]为CurrencyCode
     */
    public Map<String,String[]> parseSeatPriceAndCurrencyCode(Element seatMapResponse) throws XPathExpressionException {
        NodeList ancillaryProducts = getElementsByRelativePath(seatMapResponse, "AncillaryProduct");
        Map<String,String[]> seatPriceMap = new HashMap<>();
        for (int i = 0; i < ancillaryProducts.getLength(); i++) {
            Element ancillaryProduct = (Element) ancillaryProducts.item(i);
            //得到座位等级
            String supplierProductCode = ancillaryProduct.getAttribute("SupplierProductCode");
            //得到座位价格
            String baseXpath = "Prices/Price/Total";
            Element total = (Element) xpath.evaluate(baseXpath, ancillaryProduct, XPathConstants.NODE);
            String amount = total.getAttribute("Amount");
            String currency = total.getAttribute("CurrencyCode");
            seatPriceMap.put(supplierProductCode, new String[]{amount, currency});
        }
        return seatPriceMap;
    }


    /**
     * 重新建立comboWith的节点
     *
     * @param root      XML文件的根节点
     * @param comboWith 需要重建的comboWith节点
     */
    public void renewComboWith(Element root, Element comboWith) throws XPathExpressionException {
        Element oldComboWith = getElementByRelativePath(root, "ComboWith");
        root.removeChild(oldComboWith);
        root.appendChild(comboWith);
    }

    /**
     * 通过相对路径从给定的 Element 节点中获取子节点。
     *
     * @param element  输入的父节点 Element。
     * @param relativePath 相对路径字符串。
     * @return 返回匹配的 Element 节点。
     * @throws XPathExpressionException 如果 XPath 表达式无效。
     */
    public Element getElementByRelativePath(Element element, String relativePath) throws XPathExpressionException {
        return (Element) xpath.evaluate(relativePath, element, XPathConstants.NODE);
    }

    /**
     * 通过相对路径从给定的 Element 节点中获取子节点列表。
     *
     * @param element  输入的父节点 Element。
     * @param relativePath 相对路径字符串。
     * @return 返回匹配的 NodeList。
     * @throws XPathExpressionException 如果 XPath 表达式无效。
     */
    public NodeList getElementsByRelativePath(Element element, String relativePath) throws XPathExpressionException {
        return (NodeList) xpath.evaluate(relativePath, element, XPathConstants.NODESET);
    }
}
