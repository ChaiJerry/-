package com.bundling.vo;

/**
 * 封装产品信息的 VO（View Object）类，用于向前端展示打包结果中的产品。
 */
public class ProductVO {
    /**
     * 产品的名称。
     */
    private String name;
    /**
     * 产品的价格。
     */
    private String price;

    /**
     * 构造函数，用于初始化产品的名称和价格。
     *
     * @param name  产品的名称。
     * @param price 产品的价格。
     */
    public ProductVO(String name, String price) {
        this.name = name;
        this.price = price;
    }
    /**
     * 获取产品的名称。
     *
     * @return 产品的名称。
     */
    public String getName() {
        return name;
    }

    /**
     * 设置产品的名称。
     *
     * @param name 产品的名称。
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取产品的价格。
     *
     * @return 产品的价格。
     */
    public String getPrice() {
        return price;
    }
    /**
     * 设置产品的价格。
     *
     * @param price 产品的价格。
     */
    public void setPrice(String price) {
        this.price = price;
    }
}



