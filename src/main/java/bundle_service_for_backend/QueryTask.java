package bundle_service_for_backend;

import bundle_service_for_backend.xml_parser.*;
import bundle_system.io.*;
import bundle_system.memory_query_system.*;
import org.w3c.dom.*;

import javax.xml.xpath.*;
import java.util.*;
import java.util.concurrent.*;

import static bundle_service_for_backend.BackendBundleSystem.*;
import static bundle_system.io.SharedAttributes.*;

public class QueryTask implements Callable<Void> {
    private final Document doc;
    private static final XPathFactory xPathfactory = XPathFactory.newInstance();
    private final List<RulesStorage> rulesStorages;

    public QueryTask(Document doc, List<RulesStorage> rulesStorages) {
        this.doc = doc;
        this.rulesStorages = rulesStorages;
    }

    @Override
    public Void call() throws Exception {
        XMLParser xmlParser = new XMLParser(xPathfactory.newXPath());

        Element root = doc.getDocumentElement();
        Element comboWith = doc.createElement("ComboWith");
        root.appendChild(comboWith);
        Map<String, BundleItem> segTicketMap = xmlParser.parseComboSource(root);

        for(int i=MEAL; i<=SEAT; i++){
            boolean isEmpty = false;
            for(BundleItem item : segTicketMap.values()) {
                if (rulesStorages.get(i).queryItemAttributes(item.getAttributes()).isEmpty()) {
                    isEmpty = true;
                }
            }
            if (!isEmpty) {
                Element ancillary = doc.createElement("Ancillary");
                ancillary.setAttribute("type",SharedAttributes.getFullNames()[i]);
                comboWith.appendChild(ancillary);
            }
        }
        return null;
    }
}
