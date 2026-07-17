package tools.spirals.cerberus237.adaptiflow.plugin.runtime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public final class XmlScenarioMapParser {

    private XmlScenarioMapParser() {
    }

    public static Map<String, Object> parse(Path path) throws IOException {
        if (!Files.exists(path)) {
            return new LinkedHashMap<>();
        }

        DocumentBuilderFactory factory = newSecureFactory();
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            throw new IOException("Cannot initialize XML parser", ex);
        }

        Document document;
        try {
            document = builder.parse(path.toFile());
        } catch (SAXException ex) {
            throw new IOException("Cannot parse XML scenario source: " + path, ex);
        }

        Element root = document.getDocumentElement();
        if (root == null || !"scenarios".equals(root.getTagName())) {
            throw new IllegalArgumentException("Root XML element must be <scenarios>");
        }

        Map<String, Object> rootMap = new LinkedHashMap<>();
        Map<String, Object> scenariosMap = new LinkedHashMap<>();
        rootMap.put("scenarios", scenariosMap);

        for (Element scenarioElement : childrenByTag(root, "scenario")) {
            String scenarioId = scenarioElement.getAttribute("id");
            if (scenarioId == null || scenarioId.trim().isEmpty()) {
                throw new IllegalArgumentException("Scenario element is missing required id attribute");
            }

            Map<String, Object> scenarioMap = new LinkedHashMap<>();
            putIfPresent(scenarioMap, "pluginId", scenarioElement.getAttribute("pluginId"));
            if (scenarioElement.hasAttribute("enabled")) {
                scenarioMap.put("enabled", Boolean.parseBoolean(scenarioElement.getAttribute("enabled")));
            }
            if (scenarioElement.hasAttribute("intervalMs")) {
                scenarioMap.put("intervalMs", parseScalar(scenarioElement.getAttribute("intervalMs")));
            }

            Element schedulerElement = firstChildByTag(scenarioElement, "scheduler");
            if (schedulerElement != null) {
                Map<String, Object> schedulerMap = new LinkedHashMap<>();
                putIfPresent(schedulerMap, "type", schedulerElement.getAttribute("type"));
                schedulerMap.put("parameters", parseParameters(schedulerElement));
                scenarioMap.put("scheduler", schedulerMap);
            }

            Element eventsElement = firstChildByTag(scenarioElement, "events");
            List<Object> events = new ArrayList<>();
            if (eventsElement != null) {
                for (Element eventElement : childrenByTag(eventsElement, "event")) {
                    events.add(parseEvent(eventElement));
                }
            }
            scenarioMap.put("events", events);

            scenariosMap.put(scenarioId, scenarioMap);
        }

        return rootMap;
    }

    private static Map<String, Object> parseEvent(Element eventElement) {
        Map<String, Object> eventMap = new LinkedHashMap<>();
        putIfPresent(eventMap, "id", eventElement.getAttribute("id"));
        putIfPresent(eventMap, "type", eventElement.getAttribute("type"));

        Map<String, Object> eventParameters = parseParameters(eventElement);
        if (!eventParameters.isEmpty()) {
            eventMap.put("parameters", eventParameters);
        }

        Element collectorElement = firstChildByTag(eventElement, "collector");
        if (collectorElement != null) {
            Map<String, Object> collectorMap = new LinkedHashMap<>();
            putIfPresent(collectorMap, "type", collectorElement.getAttribute("type"));
            collectorMap.put("parameters", parseParameters(collectorElement));
            eventMap.put("collector", collectorMap);
        }

        List<Object> evaluators = new ArrayList<>();
        Element evaluatorsElement = firstChildByTag(eventElement, "evaluators");
        if (evaluatorsElement != null) {
            for (Element evaluatorElement : childrenByTag(evaluatorsElement, "evaluator")) {
                Map<String, Object> evaluatorMap = new LinkedHashMap<>();
                putIfPresent(evaluatorMap, "type", evaluatorElement.getAttribute("type"));
                evaluatorMap.put("parameters", parseParameters(evaluatorElement));
                evaluators.add(evaluatorMap);
            }
        }
        eventMap.put("evaluators", evaluators);

        List<Object> actions = new ArrayList<>();
        Element actionsElement = firstChildByTag(eventElement, "actions");
        if (actionsElement != null) {
            for (Element actionElement : childrenByTag(actionsElement, "action")) {
                Map<String, Object> actionMap = new LinkedHashMap<>();
                putIfPresent(actionMap, "type", actionElement.getAttribute("type"));
                actionMap.put("parameters", parseParameters(actionElement));
                actions.add(actionMap);
            }
        }
        eventMap.put("actions", actions);

        return eventMap;
    }

    private static Map<String, Object> parseParameters(Element ownerElement) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        Element parametersElement = firstChildByTag(ownerElement, "parameters");
        if (parametersElement == null) {
            return parameters;
        }

        for (Element parameterElement : childrenByTag(parametersElement, "parameter")) {
            String key = parameterElement.getAttribute("key");
            if (key == null || key.trim().isEmpty()) {
                continue;
            }

            String inlineValue = parameterElement.getAttribute("value");
            if (inlineValue != null && !inlineValue.trim().isEmpty()) {
                parameters.put(key, parseScalar(inlineValue));
                continue;
            }

            parameters.put(key, parseComplexValue(parameterElement));
        }

        return parameters;
    }

    private static Object parseComplexValue(Element parameterElement) {
        Element listElement = firstChildByTag(parameterElement, "list");
        if (listElement != null) {
            return parseList(listElement);
        }

        Element mapElement = firstChildByTag(parameterElement, "map");
        if (mapElement != null) {
            return parseMap(mapElement);
        }

        String text = parameterElement.getTextContent();
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        return parseScalar(text.trim());
    }

    private static List<Object> parseList(Element listElement) {
        List<Object> values = new ArrayList<>();
        for (Element valueElement : directChildren(listElement)) {
            if ("value".equals(valueElement.getTagName())) {
                values.add(parseScalar(valueElement.getTextContent().trim()));
            } else if ("list".equals(valueElement.getTagName())) {
                values.add(parseList(valueElement));
            } else if ("map".equals(valueElement.getTagName())) {
                values.add(parseMap(valueElement));
            }
        }
        return values;
    }

    private static Map<String, Object> parseMap(Element mapElement) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (Element entryElement : childrenByTag(mapElement, "entry")) {
            String key = entryElement.getAttribute("key");
            if (key == null || key.trim().isEmpty()) {
                continue;
            }

            String inlineValue = entryElement.getAttribute("value");
            if (inlineValue != null && !inlineValue.trim().isEmpty()) {
                values.put(key, parseScalar(inlineValue));
            } else {
                values.put(key, parseComplexValue(entryElement));
            }
        }
        return values;
    }

    private static Object parseScalar(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            return "";
        }
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            // Fall back to other scalar types.
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return value;
        }
    }

    private static DocumentBuilderFactory newSecureFactory() throws IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
        } catch (ParserConfigurationException ex) {
            throw new IOException("Cannot secure XML parser", ex);
        }
        return factory;
    }

    private static void putIfPresent(Map<String, Object> target, String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            target.put(key, value);
        }
    }

    private static Element firstChildByTag(Element parent, String tagName) {
        for (Element child : directChildren(parent)) {
            if (tagName.equals(child.getTagName())) {
                return child;
            }
        }
        return null;
    }

    private static List<Element> childrenByTag(Element parent, String tagName) {
        List<Element> children = new ArrayList<>();
        for (Element child : directChildren(parent)) {
            if (tagName.equals(child.getTagName())) {
                children.add(child);
            }
        }
        return children;
    }

    private static List<Element> directChildren(Element parent) {
        List<Element> children = new ArrayList<>();
        NodeList nodeList = parent.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                children.add((Element) node);
            }
        }
        return children;
    }
}