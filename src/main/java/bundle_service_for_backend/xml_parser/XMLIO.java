package bundle_service_for_backend.xml_parser;

import javax.xml.parsers.*;

import org.w3c.dom.*;

import org.xml.sax.*;

import java.io.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

/**
 * XMLIO 类用于：
 * 1、将xml和string相互转换以接收和发送给前端
 * 2、xml和本地文件系统交互
 */
public class XMLIO {
    // 创建一个DocumentBuilderFactory实例，用于创建DocumentBuilder对象
    private final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    // 创建一个TransformerFactory实例，用于创建Transformer对象
    private final TransformerFactory tf = TransformerFactory.newInstance();

    public XMLIO() throws ParserConfigurationException {
        // 设置工厂类
        setupDocumentBuilderFactory();
    }

    /**
     * 第一个接口的xml文件读取测试
     * @return Document对象
     * @throws ParserConfigurationException 解析配置异常
     * @throws IOException 输入输出异常
     * @throws SAXException SAX异常
     */
    public Document readTest1() throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilder builder = factory.newDocumentBuilder();
        File file = new File("C:\\Users\\mille\\Desktop\\同步\\民航项目文档类\\打包接口请求响应设计样例\\打包接口请求响应设计样例\\打包接口请求响应设计样例\\接口1-可打包附加产品类型查询\\0.可打包附加产品类型查询请求.OJ_ComboSearchRQ.xml");
        return builder.parse(file);
    }

    /**
     * 第二个接口的xml文件读取测试
     * @return Document对象
     * @throws ParserConfigurationException 解析配置异常
     * @throws IOException 输入输出异常
     * @throws SAXException SAX异常
     */
    public Document readTest2() throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilder builder = factory.newDocumentBuilder();
        File file = new File("C:\\Users\\mille\\Desktop\\同步\\民航项目文档类\\打包接口请求响应设计样例\\打包接口请求响应设计样例\\接口2-动态打包组包接口\\4.动态打包信息查询请求.OJ_ComboSearchRS  (2).xml");
        return builder.parse(file);
    }

    /**
     * 读取xml文件
     * @param path xml文件路径
     * @return Document对象
     * @throws ParserConfigurationException 解析配置异常
     * @throws IOException  输入输出异常
     * @throws SAXException SAX异常
     */
    public Document read(String path) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilder builder = factory.newDocumentBuilder();
        File file = new File(path);
        return builder.parse(file);
    }

    /**
     * 将字符串转换为Document对象
     * @param xmlSource xml字符串
     * @return  Document对象
     * @throws ParserConfigurationException 解析配置异常
     * @throws IOException 输入输出异常
     */
    public Document stringToDocument(String xmlSource) throws ParserConfigurationException, IOException, SAXException {
        // 使用factory创建一个新的DocumentBuilder实例
        DocumentBuilder builder = factory.newDocumentBuilder();
        // 将字符串转换为输入源
        InputSource is = new InputSource(new StringReader(xmlSource));
        // 解析输入源并返回一个Document对象
        return builder.parse(is);
    }

    /**
     * 将Document对象（xml）转换为字符串
     * @param doc Document对象
     * @return xml字符串
     * @throws TransformerException 异常
     */
    public String documentToString(Document doc) throws TransformerException {
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.getBuffer().toString();
    }

    /**
     * 设置DocumentBuilderFactory的配置
     * 使其不加载外部DTD，禁用XInclude处理等
     * @throws ParserConfigurationException 解析配置异常
     */
    private void setupDocumentBuilderFactory() throws ParserConfigurationException {
// 禁止 DOCTYPE 声明
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
// 禁止外部通用实体
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
// 禁止外部参数实体
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
// 不加载外部 DTD
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
// 禁用 XInclude 处理
        factory.setXIncludeAware(false);
// 不展开实体引用
        factory.setExpandEntityReferences(false);
    }

}
