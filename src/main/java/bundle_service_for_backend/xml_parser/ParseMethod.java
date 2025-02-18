package bundle_service_for_backend.xml_parser;

import org.w3c.dom.Element;

import javax.xml.xpath.*;
import java.util.*;

/**
 * 解析XML的方法接口，用于解析不同的商品。
 */
@FunctionalInterface
public interface ParseMethod {
    /**
     * 执行解析方法。
     * @param root XML的根元素
     * @return 一个包含商品信息的Map，其中键是航段的ID，值是一个商品列表
     * @throws XPathExpressionException 如果XPath表达式有误抛出此异常
     */
    Map<String, List<BundleItem>> execute(Element root) throws XPathExpressionException;
}