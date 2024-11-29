package bundle_service_for_backend;

import bundle_service_for_backend.xml_parser.*;
import bundle_system.memory_query_system.*;
import org.w3c.dom.*;

import java.util.*;

@FunctionalInterface
public interface SortBundleItemMethod {
    Element execute(Map<String, BundleItem> ticketInfo
            , Map<String, List<BundleItem>> bundleItems
            , RulesStorage rulesStorage, Element fatherElement,Document doc);
}
