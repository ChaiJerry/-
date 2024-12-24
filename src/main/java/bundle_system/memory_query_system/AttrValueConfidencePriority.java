package bundle_system.memory_query_system;

import java.util.*;

/**
 * 这个类用来存储属性值和置信度，以及优先级。
 */
public class AttrValueConfidencePriority {
    //属性值
    private String attributeValue = "";
    //置信度
    private double confidence = -1;
    //优先级
    private int priority = 0;
    /**
     * 赋值，在得到锁之前会阻塞，这里调用后会解除锁
     *
     * @param attributeValue 属性值
     * @param priority       优先级
     * @param confidence     置信度
     */
    public synchronized void tryAssign(String attributeValue, int priority, double confidence) {
        if (this.priority < priority || (this.priority == priority && this.confidence < confidence)) {
            this.attributeValue = attributeValue;
            this.priority = priority;
            this.confidence = confidence;
        }
    }

    /**
     * 得到属性值的方法
     * @return 属性值
     */
    public String getAttributeValue() {
        return attributeValue;
    }

    /**
     * 设置属性值的方法
     * @param attributeValue 属性值
     */
    public void setAttributeValue(String attributeValue) {
        this.attributeValue = attributeValue;
    }

    /**
     * 得到置信度的方法
     * @return 置信度
     */
    public double getConfidence() {
        return confidence;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AttrValueConfidencePriority that)) return false;
        return Double.compare(confidence, that.confidence) == 0 && Objects.equals(getAttributeValue(), that.getAttributeValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAttributeValue(), confidence);
    }
}
