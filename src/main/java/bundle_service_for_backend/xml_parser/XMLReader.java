package bundle_service_for_backend.xml_parser;

import javax.xml.parsers.*;

import org.w3c.dom.*;

import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;



public class XMLReader {



    public XMLReader() {
    }

    public Document read() throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        File file = new File("src/main/resources/接口2-动态打包组包接口/4.动态打包信息查询请求.OJ_ComboSearchRS  (2).xml");
        return builder.parse(file);
    }

    public Document read(String path) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        File file = new File(path);
        return builder.parse(file);
    }

}
