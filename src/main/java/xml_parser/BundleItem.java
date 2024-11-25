package xml_parser;

import java.util.*;

public class BundleItem {

    private final String FlightSegmentRPH;
    private final Map<String,String> attributes = new HashMap<>();

    public BundleItem(String rph) {
        this.FlightSegmentRPH = rph;
    }
    public String getFlightSegmentRPH() {
        return FlightSegmentRPH;
    }

    /**
     * 为商品加入属性名和属性值键值对
     * @param name 属性名
     * @param value 属性值
     */
    public void addAttributeNameValuePair(String name, String value) {
        attributes.put(name, value);
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("FlightSegmentRPH: ").append(this.getFlightSegmentRPH()).append("\n");
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }
}
