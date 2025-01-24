package bundle_service_for_backend;

import bundle_service_for_backend.xml_parser.*;
import bundle_system.io.*;
import bundle_system.memory_query_system.*;
import org.w3c.dom.*;

import javax.xml.xpath.*;
import java.util.*;
import java.util.concurrent.*;

import static bundle_system.io.SharedAttributes.*;

/**
 * 用于第一个接口的并行查询任务
 * 可以得到是否可以查询到某一商品的规则
 */
public class QueryTask implements Callable<Void> {
    // 传入的XML文档对象
    private final Document doc;
    private static final XPathFactory xPathfactory = XPathFactory.newDefaultInstance();
    private final List<RulesStorage> rulesStorages;

    public QueryTask(Document doc, List<RulesStorage> rulesStorages) {
        this.doc = doc;
        this.rulesStorages = rulesStorages;
    }

    @Override
    public Void call() throws Exception {
        XMLParser xmlParser = new XMLParser(xPathfactory.newXPath());
        Element root = doc.getDocumentElement();
        // 删掉原有的comboWith标签（如果有），重新创建新的comboWith标签
        if(xmlParser.getElementByRelativePath(root, "/OJ_ComboSearchRQ/ComboWith") != null) {
            root.removeChild(xmlParser.getElementByRelativePath(root, "/OJ_ComboSearchRQ/ComboWith"));
        }
        Element comboWith = doc.createElement("ComboWith");
        root.appendChild(comboWith);
        Map<String, BundleItem> segTicketMap = xmlParser.parseComboSourceForRQ(root);

        for (int i = HOTEL; i < getFullNames().length; i++) {
            boolean haveEmptyAttribute = false;
            for (BundleItem item : segTicketMap.values()) {
                for (AttrValueConfidencePriority attrValueConfidencePriority
                        : rulesStorages.get(i).queryBestRules(item.getAttributes()).values()) {
                    if (attrValueConfidencePriority.getConfidence() < 0) {
                        haveEmptyAttribute = true;
                        break;
                    }
                }
                if (haveEmptyAttribute) break;
            }
            if (!haveEmptyAttribute) {
                Element ancillary = doc.createElement("Ancillary");
                ancillary.setAttribute("type", SharedAttributes.getFullNames()[i]);
                comboWith.appendChild(ancillary);
            }
        }
        replaceRootNode(doc, "OJ_ComboSearchRS");
        return null;
    }

    /**
     * 替换给定文档的根节点。
     *
     * @param doc 包含要替换的根节点的文档
     * @param newRootName 新的根节点名称
     */
    public static void replaceRootNode(Document doc, String newRootName) {
        // 创建一个新的元素，作为新的根节点
        Element newRootElement = doc.createElement(newRootName);

        // 获取当前的根节点
        Element oldRootElement = doc.getDocumentElement();

        // 复制所有属性到新根节点（如果有）
        NamedNodeMap attributes = oldRootElement.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Attr attr = (Attr) attributes.item(i);
            newRootElement.setAttribute(attr.getName(), attr.getValue());
        }

        // 将旧根节点的所有子节点移动到新根节点
        while (oldRootElement.hasChildNodes()) {
            Node child = oldRootElement.getFirstChild();
            newRootElement.appendChild(child);
        }

        // 将新根节点添加到文档，并移除旧根节点
        doc.replaceChild(newRootElement, oldRootElement);
    }
}
