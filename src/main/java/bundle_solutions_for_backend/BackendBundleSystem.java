package bundle_solutions_for_backend;

import bundle_system.memory_query_system.*;
import org.xml.sax.*;
import xml_parser.*;
import xml_parser.XMLReader;

import javax.xml.parsers.*;
import javax.xml.xpath.*;
import java.io.*;
import java.util.*;

import static bundle_system.io.SharedAttributes.*;

public class BackendBundleSystem {

    public static void test() throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        XMLReader xmlReader = new XMLReader();
        XMLParser xmlParser = new XMLParser();
        RulesStorage rulesStorage = QuickQuery.initRulesStorageByType(MEAL);
        Map<String, BundleItem> stringBundleItemMap = xmlParser.parseComboSource(xmlReader.read());
        for (Map.Entry<String, BundleItem> entry : stringBundleItemMap.entrySet()) {
            rulesStorage.queryItemAttributes(entry.getValue());
        }


    }

}
