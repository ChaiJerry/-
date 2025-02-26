package bundle_service_for_backend.xml_parser;

import org.w3c.dom.*;

/**
 * 继承BundleItem类，专门用于解析酒店套餐信息。
 *
 */
public class BundleItemForHotel extends BundleItem {
    // 定义酒店套餐相关的属性
    // 服务类型
    private String serviceType;

    /**
     * 获取服务代码(酒店代码)
     * @return 服务代码
     */
    public String getServiceCode() {
        return serviceCode;
    }

    // 服务代码
    private String serviceCode;
    // 子代码
    private String subCode;
    // 关联的行程段ID引用
    private String segmentIDRef;
    // 有效期开始时间
    private String notValidBefore;
    // 有效期结束时间
    private String notValidAfter;
    // 服务地点描述文本
    private String serviceLocation;
    // 信息文本数组
    private String[] infoTexts;
    // 总金额
    private String totalAmount;
    // 货币代码
    private String currencyCode;

    public BundleItemForHotel(String key) {
        super(key);
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public void setSubCode(String subCode) {
        this.subCode = subCode;
    }

    public void setSegmentIDRef(String segmentIDRef) {
        this.segmentIDRef = segmentIDRef;
    }

    public void setNotValidBefore(String notValidBefore) {
        this.notValidBefore = notValidBefore;
    }

    public void setNotValidAfter(String notValidAfter) {
        this.notValidAfter = notValidAfter;
    }

    public void setServiceLocation(String serviceLocation) {
        this.serviceLocation = serviceLocation;
    }

    public void setInfoTexts(String[] infoTexts) {
        this.infoTexts = infoTexts;
    }

    public void setTotalAmount(String totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    /**
     * 子代码的getter方法，用于过滤同类酒店
     * @return 酒店subCode(roomTypeCode)字段值。
     */
    public String getSubCode() {
        return subCode;
    }


    /**
     * 通过实例化后的诸多属性构建酒店房间级商品的XML元素
     * ，用于在报文之中返回。
     * @param doc XML文档对象
     * @return Element 对象，即构建的酒店房间级商品的XML元素
     */
    public Node buildHotelElement(Document doc) {
        // 创建Service元素，构建xml结构
        Element serviceElement = doc.createElement("Service");
        serviceElement.setAttribute("ServiceType", serviceType);
        serviceElement.setAttribute("ServiceCode", serviceCode);
        serviceElement.setAttribute("SubCode", subCode);
        serviceElement.setAttribute("SegmentIDRef", segmentIDRef);
        serviceElement.setAttribute("NotValidBefore", notValidBefore);
        serviceElement.setAttribute("NotValidAfter", notValidAfter);

        Element serviceLocationElement = doc.createElement("ServiceLocation");
        serviceLocationElement.appendChild(doc.createTextNode(serviceLocation));
        serviceElement.appendChild(serviceLocationElement);

        Element infoElement = doc.createElement("Info");
        // 添加多个文本信息
        for (String text : infoTexts) {
            Element textElement = doc.createElement("Text");
            textElement.appendChild(doc.createTextNode(text));
            infoElement.appendChild(textElement);
        }
        serviceElement.appendChild(infoElement);

        Element pricesElement = doc.createElement("Prices");
        Element priceElement = doc.createElement("Price");

        Element totalElement = doc.createElement("Total");
        totalElement.setAttribute("Amount", totalAmount);
        totalElement.setAttribute("CurrencyCode", currencyCode);
        priceElement.appendChild(totalElement);

        pricesElement.appendChild(priceElement);
        serviceElement.appendChild(pricesElement);
        return serviceElement;
    }
}



