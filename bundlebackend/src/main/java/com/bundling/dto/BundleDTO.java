package com.bundling.dto;

/**
 * BundleDTO 类用于表示前端向后端发送的请求数据。
 * 该类包含了打包系统的各种请求参数，如月份、出发地、目的地等。
 */
public class BundleDTO {
    private String month; // 旅行月份
    private String from; // 出发地
    private String to; // 目的地
    private String grade; // 等级或舱位
    private String is_child; // 是否包含儿童票
    private String train_id; // 训练ID

    /**
     * 获取旅行月份。
     *
     * @return 旅行月份。
     */
    public String getMonth() {
        return month;
    }

    /**
     * 设置旅行月份。
     *
     * @param month 旅行月份。
     */
    public void setMonth(String month) {
        this.month = month;
    }

    /**
     * 获取出发地。
     *
     * @return 出发地。
     */
    public String getFrom() {
        return from;
    }

    /**
     * 设置出发地。
     *
     * @param from 出发地。
     */
    public void setFrom(String from) {
        this.from = from;
    }

    /**
     * 获取目的地。
     *
     * @return 目的地。
     */
    public String getTo() {
        return to;
    }

    /**
     * 设置目的地。
     *
     * @param to 目的地。
     */
    public void setTo(String to) {
        this.to = to;
    }

    /**
     * 获取等级或舱位。
     *
     * @return 等级或舱位。
     */
    public String getGrade() {
        return grade;
    }

    /**
     * 设置等级或舱位。
     *
     * @param grade 等级或舱位。
     */
    public void setGrade(String grade) {
        this.grade = grade;
    }

    /**
     * 获取是否包含儿童票的信息。
     *
     * @return 是否包含儿童票的信息。
     */
    public String getIs_child() {
        return is_child;
    }

    /**
     * 设置是否包含儿童票的信息。
     *
     * @param is_child 是否包含儿童票的信息。
     */
    public void setIs_child(String is_child) {
        this.is_child = is_child;
    }

    /**
     * 获取训练ID。
     *
     * @return 训练ID。
     */
    public String getTrain_id() {
        return train_id;
    }

    /**
     * 设置训练ID。
     *
     * @param train_id 训练ID。
     */
    public void setTrain_id(String train_id) {
        this.train_id = train_id;
    }
}
