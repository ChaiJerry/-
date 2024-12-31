package bundle_service_for_backend.xml_parser;

import org.w3c.dom.*;

/**
 * 继承BundleItem类，专门用于解析酒店套餐信息。
 *
 */
public class BundleItemForHotel extends BundleItem {
    private String serviceType;
    private String serviceCode;
    private String subCode;
    private String segmentIDRef;
    private String notValidBefore;
    private String notValidAfter;
    private String serviceLocation;
    private String[] infoTexts;
    private String totalAmount;
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

    public Node buildHotelElement(Document doc) {
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



