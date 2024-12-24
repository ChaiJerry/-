package bundle_service_for_backend.xml_parser;

import org.w3c.dom.Element;

import javax.xml.xpath.*;
import java.util.*;

@FunctionalInterface
public interface ParseMethod {
    Map<String, List<BundleItem>> execute(Element root) throws XPathExpressionException;
}