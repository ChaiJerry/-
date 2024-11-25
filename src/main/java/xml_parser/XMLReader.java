package xml_parser;

import javax.xml.parsers.*;

import bundle_system.data_processer.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;

import javax.xml.xpath.*;

import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import static bundle_system.data_processer.DataConverter.*;

public class XMLReader {
    public XMLReader() {
    }

    public Document read() throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        File file = new File("src/main/resources/接口2-动态打包组包接口/4.动态打包信息查询请求.OJ_ComboSearchRS  (2).xml");
        return builder.parse(file);
    }

    public List<BundleItem> parseComboSource(Document doc) throws XPathExpressionException {
        try {
            List<BundleItem> bundleItems = new ArrayList<>();
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            Element root = doc.getDocumentElement();
            String ODXpath = "/OJ_ComboSearchRS/ComboSource/PricedItinerary/AirItinerary/OriginDestinationOptions/OriginDestinationOption";
            NodeList OriginDestinationOption = (NodeList) xpath.evaluate(ODXpath, root, XPathConstants.NODESET);
            for (int i = 0; i < OriginDestinationOption.getLength(); i++) {
                // 得到RPH
                Element originDestinationOption = (Element) OriginDestinationOption.item(i);
                String rph = originDestinationOption.getAttribute("RPH");
                // 创建BundleItem对象
                BundleItem bundleItem = new BundleItem(rph);
                // 将bundleItem添加到列表中
                bundleItems.add(bundleItem);
                // 得到出发时间
                Element flightSegment = (Element) originDestinationOption.getElementsByTagName("FlightSegment").item(0);
                String month = flightSegment.getAttribute("ArrivalDateTime").split("-")[1];
                bundleItem.addAttributeNameValuePair("MONTH", month);
                // 得到出发地和目的地
                Element departureAirport = (Element) flightSegment.getElementsByTagName("DepartureAirport").item(0);
                String departureLocationCode = departureAirport.getAttribute("LocationCode");
                // 添加到bundleItem中（为了和训练中的属性名称一致，这里用FROM和TO）
                bundleItem.addAttributeNameValuePair("FROM", departureLocationCode);
                Element arrivalAirport = (Element) flightSegment.getElementsByTagName("ArrivalAirport").item(0);
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
                    for (BundleItem bundleItem : bundleItems) {
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
                        BundleItem bundleItem = getBundleItemByFlightSegmentRPH(flightSegmentRPH, bundleItems);

                        // 得到舱位等级
                        Element fareReference = (Element) fareInfo.getElementsByTagName("FareReference").item(0);
                        String grade = ticketGrade2Specific(fareReference.getAttribute("CabinCode"));
                        bundleItem.addAttributeNameValuePair("GRADE", grade);

                        // 得到折扣
                        Element info = (Element) fareInfo.getElementsByTagName("FareInfo").item(0);
                        String discount = info.getAttribute("DisCount");
                        String promotionRateGrade = (((int)(100-Double.parseDouble(discount)*100))/10) + "";
                        bundleItem.addAttributeNameValuePair("PROMOTION_RATE", promotionRateGrade);

                        // 得到折扣前价格
                        Element fare = (Element) info.getElementsByTagName("Fare").item(0);
                        String baseCabinClassAmount = fare.getAttribute("BaseCabinClassAmount");
                        Integer priceGrade = DataParser.floatStr2Attribute(baseCabinClassAmount, 1000);
                        bundleItem.addAttributeNameValuePair("T_FORMER", priceGrade + "");

                    }
                    break;
                }

            }
            return bundleItems;
        } catch (XPathExpressionException e) {
            throw new XPathExpressionException(e);
        }
    }

    public BundleItem getBundleItemByFlightSegmentRPH(String rph, List<BundleItem> bundleItems) {
        for (BundleItem bundleItem : bundleItems) {
            if (bundleItem.getFlightSegmentRPH().equals(rph)) {
                return bundleItem;
            }
        }
        return null;
    }
}
