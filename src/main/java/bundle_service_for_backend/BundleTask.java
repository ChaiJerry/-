package bundle_service_for_backend;

import bundle_service_for_backend.xml_parser.*;
import bundle_system.memory_query_system.*;
import org.w3c.dom.*;
import xml_parser.*;

import javax.xml.xpath.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import static bundle_service_for_backend.BackendBundleSystem.*;
import static bundle_system.io.SharedAttributes.*;

public class BundleTask implements Callable<Void> {
    private final Document doc;
    private static final XPathFactory xPathfactory = XPathFactory.newInstance();
    private static final List<RulesStorage> rulesStorages;

    static {
        try {
            rulesStorages = QuickQuery.initAllRulesStorage();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public BundleTask(Document doc) {
        this.doc = doc;
    }

    @Override
    public Void call() throws Exception {
        XMLParser xmlParser = new XMLParser(xPathfactory.newXPath());

        Element root = doc.getDocumentElement();
        Element comboWith = doc.createElement("ComboWith");

        //新建返回的Document部分
        //解析xml文件部分
        List<ParseMethod> parseMethods = xmlParser.getParseMethods();
        Map<String, BundleItem> segTicketMap = xmlParser.parseComboSource(root);
        // 处理餐食
        Map<String, List<BundleItem>> bundleItems = parseMethods.get(MEAL).execute(root);
        // 得到选座/餐食返回的AncillaryProducts
        Element ancillaryProducts = testBundleMeal(segTicketMap
                , bundleItems, rulesStorages.get(MEAL), null, doc);

        // 处理行李
        bundleItems = parseMethods.get(BAGGAGE).execute(root);
        // 得到行李返回的ancillary0
        Element ancillary0 =sortBundleItemMethods.get(BAGGAGE).execute(segTicketMap, bundleItems
                , rulesStorages.get(BAGGAGE), null ,doc);

        // 处理保险
        bundleItems = parseMethods.get(INSURANCE).execute(root);
        // 得到保险返回的insurance
        Element insurance = sortBundleItemMethods.get(INSURANCE).execute(segTicketMap, bundleItems
                , rulesStorages.get(INSURANCE), null ,doc);

        // 处理选座
        bundleItems = parseMethods.get(SEAT).execute(root);
        // 得到选座返回的ancillary1
        Element ancillary1 = testBundleSeat(segTicketMap, bundleItems
                , rulesStorages.get(SEAT), ancillaryProducts, doc);

        // 将返回的ancillaryProducts、insurance、ancillary0、ancillary1添加到comboWith中
        comboWith.appendChild(insurance);
        comboWith.appendChild(ancillary0);
        comboWith.appendChild(ancillary1);

        xmlParser.renewComboWith(root,comboWith);
        return null;
    }

}
