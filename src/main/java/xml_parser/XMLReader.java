package xml_parser;

import javax.xml.parsers.*;

import bundle_system.data_processer.*;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

import javax.xml.xpath.*;

import static bundle_system.data_processer.DataConverter.*;
import static bundle_system.io.SharedAttributes.*;

public class XMLReader {



    public XMLReader() {
    }

    public Element read() throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        File file = new File("src/main/resources/接口2-动态打包组包接口/4.动态打包信息查询请求.OJ_ComboSearchRS  (2).xml");
        return builder.parse(file).getDocumentElement();
    }

}
