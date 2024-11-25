package bundle_system.memory_query_system;

public class ItemAttributeValueAndConfidenceAndPriority {
    private String attributeValue = "";
    private double confidence = 0.0;
    private int priority = 0;
    public ItemAttributeValueAndConfidenceAndPriority() {
    }

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

    //得到属性值
    public String getAttributeValue() {
        return attributeValue;
    }

}
