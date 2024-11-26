package bundle_solutions_for_backend;

import bundle_system.memory_query_system.*;
import org.w3c.dom.*;
import xml_parser.*;

import java.util.*;

@FunctionalInterface
public interface SortBundleItemMethod {
    void execute(Map<String, BundleItem> ticketInfo
            , Map<String, List<BundleItem>> bundleItems
            , RulesStorage rulesStorage, Element comboWith,Document doc);
}
