package bundle_service_for_backend.xml_parser;

import org.jetbrains.annotations.*;
import org.w3c.dom.*;

import java.util.*;

import static java.lang.Math.*;

public class BundleItem implements Comparable<BundleItem> {

    private final String FlightSegmentRPH;
    private Element element;
    private final Map<String, String> attributes = new HashMap<>();
    // priority优先级，默认为0，是double的原因是方便以后根据置信度拓展排序优先度计算
    private double priority = 0;


    // xmlAttributes是xml元素的属性，用于存储xml元素的属性，以便于后续的处理，这里主要是为了重组Seat的xml属性
    private  Map<String, String> xmlAttributes;



    public Element getElement() {
        return element;
    }

    /**
     * 设置优先级，直接比较字符串
     * @param recommendAttributes 推荐的属性
     */
    public void setPriority(Map<String, String> recommendAttributes) {
        for (String key : recommendAttributes.keySet()) {
            if (attributes.containsKey(key)) {
                // 实际的属性值
                String value = attributes.get(key);
                // 推荐属性值
                String recommendValue = recommendAttributes.get(key);
                if (value.equalsIgnoreCase(recommendValue)) {
                    priority++;
                }
            }
        }
    }

    /**
     * 设置优先级，如果是字符串，直接比较，如果是数字，则计算二者的差值的负指数
     * 但是有风险在于字符串要是内容是数值但是其实是应该直接比较的情况下可能造成错误
     * @param recommendAttributes 推荐的属性
     */
    public void setPriorityWithNumParse(Map<String, String> recommendAttributes) {
        for (String key : recommendAttributes.keySet()) {
            if (attributes.containsKey(key)) {
                //实际的属性值
                String value = attributes.get(key);
                //推荐属性值
                String recommendValue = recommendAttributes.get(key);
                if (value.equalsIgnoreCase(recommendValue)) {
                    priority++;
                } else if (isNum(value) && isNum(recommendValue)) {
                    //判断不相等的情况，判断是否为浮点数或整数
                    //优先级加上二者之差的绝对值的负指数
                    priority += exp(-Math.abs(Double.parseDouble(value) - Double.parseDouble(recommendValue)));
                }
            }
        }
    }


    /**
     * 特殊的判断段是否为数字的方法
     * 这个地方为了效率仅判断最后一位是否为数字，如果最后一位是数字则认为这个字符串是数字（这是由数据集特性决定的）
     *
     * @param s 字符串
     * @return 是否为数字
     */
    private boolean isNum(String s) {
        return !s.isEmpty() && Character.isDigit(s.charAt(s.length() - 1));
    }

    public BundleItem(String rph) {
        this.FlightSegmentRPH = rph;
    }

    public BundleItem(String rph, Node node) {
        this.FlightSegmentRPH = rph;
        this.element = (Element) node;
    }

    public String getFlightSegmentRPH() {
        return FlightSegmentRPH;
    }

    /**
     * 为商品加入属性名和属性值键值对
     *
     * @param name  属性名
     * @param value 属性值
     */
    public void addAttributeNameValuePair(String name, String value) {
        attributes.put(name, value);
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public Map<String, String> getXmlAttributes() {
        return xmlAttributes;
    }

    public void setXmlAttributes(Map<String, String> xmlAttributes) {
        this.xmlAttributes = xmlAttributes;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\nFlightSegmentRPH: ").append(this.getFlightSegmentRPH()).append("\n");
        // 打印优先级
        sb.append("Priority: ").append(this.priority).append("\n");
        // 遍历attributes的所有键值对
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Compares this object with the specified object for order.  Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.
     *
     * <p>The implementor must ensure {@link Integer#signum
     * signum}{@code (x.compareTo(y)) == -signum(y.compareTo(x))} for
     * all {@code x} and {@code y}.  (This implies that {@code
     * x.compareTo(y)} must throw an exception if and only if {@code
     * y.compareTo(x)} throws an exception.)
     *
     * <p>The implementor must also ensure that the relation is transitive:
     * {@code (x.compareTo(y) > 0 && y.compareTo(z) > 0)} implies
     * {@code x.compareTo(z) > 0}.
     *
     * <p>Finally, the implementor must ensure that {@code
     * x.compareTo(y)==0} implies that {@code signum(x.compareTo(z))
     * == signum(y.compareTo(z))}, for all {@code z}.
     *
     * @param o the object to be compared.
     * @return a negative integer, zero, or a positive integer as this object
     * is less than, equal to, or greater than the specified object.
     * @throws NullPointerException if the specified object is null
     * @throws ClassCastException   if the specified object's type prevents it
     *                              from being compared to this object.
     * @apiNote It is strongly recommended, but <i>not</i> strictly required that
     * {@code (x.compareTo(y)==0) == (x.equals(y))}.  Generally speaking, any
     * class that implements the {@code Comparable} interface and violates
     * this condition should clearly indicate this fact.  The recommended
     * language is "Note: this class has a natural ordering that is
     * inconsistent with equals."
     */
    @Override
    public int compareTo(@NotNull BundleItem o) {
        return Double.compare(o.priority,this.priority);
    }
}
