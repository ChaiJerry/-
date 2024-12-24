package com.bundling.service;

import com.bundling.vo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

class ProductTest {

    private Product productA;
    private Product productB;

    @BeforeEach
    public void setUp() {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("color", "red");
        attributes.put("size", "small");
        productA = new Product("Product A", "100", attributes);
        Map<String, String> newAttributes = new HashMap<>();
        newAttributes.put("color", "red");
        newAttributes.put("size", "large");
        productB = new Product("Product B", "200", newAttributes);
    }



    @Test
    void testSetPriority() {
        Map<String, String> commendedAttributes = new HashMap<>();
        commendedAttributes.put("color", "red");
        commendedAttributes.put("size", "large");

        productA.setPriority(commendedAttributes);
        assertEquals(1.0, productA.getPriority(), 0.001);


        productB.setPriority(commendedAttributes);
        assertEquals(2.0, productB.getPriority(), 0.001);

    }

    @Test
    void testCompareTo() {
        Map<String, String> commendedAttributes = new HashMap<>();
        commendedAttributes.put("color", "red");
        commendedAttributes.put("size", "large");
        productA.setPriority(commendedAttributes);
        productB.setPriority(commendedAttributes);

        assertTrue(productB.getPriority().compareTo(productA.getPriority()) > 0);

    }

    @Test
    void testCopy() {
        Product copiedProduct = productA.copy();
        assertNotSame(productA, copiedProduct);
        assertEquals(productA.name, copiedProduct.name);
        assertEquals(productA.price, copiedProduct.price);
        assertEquals(productA.attributes, copiedProduct.attributes);
        assertEquals(productA.attributes, copiedProduct.attributes);
    }


    @Test
    void testGetProductVOList() {
        Map<String, String> commendedAttributes = new HashMap<>();
        commendedAttributes.put("color", "red");
        commendedAttributes.put("size", "large");

        List<Product> productList = Arrays.asList(
                productA, productB
        );


        List<ProductVO> productVOList = Product.getProductVOList(productList, commendedAttributes);

        assertEquals(2, productVOList.size());
        assertEquals("Product B", productVOList.get(0).getName());
        assertEquals("200", productVOList.get(0).getPrice());
        assertEquals("Product A", productVOList.get(1).getName());
        assertEquals("100", productVOList.get(1).getPrice());
    }




}



