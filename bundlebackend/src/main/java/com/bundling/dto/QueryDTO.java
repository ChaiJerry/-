package com.bundling.dto;

import java.util.*;
/**
 * 前端发送到后端进行打包查询的DTO类。
 */
public class QueryDTO {
    // 航司
    private String carrier;
    // 舱位等级
    private String grade;
    // 价格
    private String price;
    // 月份
    private String month;
    // 目的地
    private String to;
    // 出发地
    private String from;
    // 是否带儿童
    private String have_child;
    // 打包项目数组
    private String[] bundle_item;
    // 作为知识库的训练ID
    private String train_id;

    public String getCarrier() {
        return carrier;
    }

    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getHave_child() {
        return have_child;
    }

    public void setHave_child(String have_child) {
        this.have_child = have_child;
    }

    public String[] getBundle_item() {
        return bundle_item;
    }

    public void setBundle_item(String[] bundle_item) {
        this.bundle_item = bundle_item;
    }

    public String getTrain_id() {
        return train_id;
    }

    public void setTrain_id(String train_id) {
        this.train_id = train_id;
    }

    @Override
    public String toString() {
        return "QueryDTO{" +
                "carrier='" + carrier + '\'' +
                ", grade='" + grade + '\'' +
                ", price='" + price + '\'' +
                ", month='" + month + '\'' +
                ", to='" + to + '\'' +
                ", from='" + from + '\'' +
                ", haveChild='" + have_child + '\'' +
                ", bundleItem=" + Arrays.toString(bundle_item) +
                ", trainId='" + train_id + '\'' +
                '}';
    }
}
