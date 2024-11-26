package xml_parser;

import org.w3c.dom.Element;

import javax.swing.text.*;
import javax.xml.xpath.*;
import java.util.*;

@FunctionalInterface
public interface Operation {
    public Map<String, List<BundleItem>> execute(Element root) throws XPathExpressionException;
}