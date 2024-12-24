package com.bundling.vo;

import java.util.*;

/**
 * 在打包体验接口发送给前端的打包结果对象。
 * 其中包含了四种类品类的打包结果：保险、餐食、座位和行李。
 */
public class BundleVO {
    /**
     * 保险产品的列表。
     */
    private List<ProductVO> insurance;

    /**
     * 餐食产品的列表。
     */
    private List<ProductVO> meal;

    /**
     * 座位产品的列表。
     */
    private List<ProductVO> seat;

    /**
     * 行李产品的列表。
     */
    private List<ProductVO> baggage;

    /**
     * 构造函数，用于初始化不同种类的产品列表。
     *
     * @param productsList 包含四个子列表的列表，分别对应保险、餐食、座位和行李产品。
     */
    public BundleVO(List<List<ProductVO>> productsList) {
        this.meal = productsList.get(0);
        this.baggage = productsList.get(1);
        this.insurance = productsList.get(2);
        this.seat = productsList.get(3);
    }

    // Getters and Setters

    /**
     * 获取保险产品的列表。
     *
     * @return 保险产品的列表。
     */
    public List<ProductVO> getInsurance() {
        return insurance;
    }

    /**
     * 设置保险产品的列表。
     *
     * @param insurance 保险产品的列表。
     */
    public void setInsurance(List<ProductVO> insurance) {
        this.insurance = insurance;
    }

    /**
     * 获取餐食产品的列表。
     *
     * @return 餐食产品的列表。
     */
    public List<ProductVO> getMeal() {
        return meal;
    }

    /**
     * 设置餐食产品的列表。
     *
     * @param meal 餐食产品的列表。
     */
    public void setMeal(List<ProductVO> meal) {
        this.meal = meal;
    }

    /**
     * 获取座位产品的列表。
     *
     * @return 座位产品的列表。
     */
    public List<ProductVO> getSeat() {
        return seat;
    }

    /**
     * 设置座位产品的列表。
     *
     * @param seat 座位产品的列表。
     */
    public void setSeat(List<ProductVO> seat) {
        this.seat = seat;
    }

    /**
     * 获取行李产品的列表。
     *
     * @return 行李产品的列表。
     */
    public List<ProductVO> getBaggage() {
        return baggage;
    }

    /**
     * 设置行李产品的列表。
     *
     * @param baggage 行李产品的列表。
     */
    public void setBaggage(List<ProductVO> baggage) {
        this.baggage = baggage;
    }
}



