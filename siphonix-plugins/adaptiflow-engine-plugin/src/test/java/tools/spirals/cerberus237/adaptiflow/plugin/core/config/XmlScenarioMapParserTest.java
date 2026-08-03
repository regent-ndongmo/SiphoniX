package tools.spirals.cerberus237.adaptiflow.plugin.core.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class XmlScenarioMapParserTest {

    @Test
    public void shouldParseNestedParametersFromXml() throws IOException {
        Path baseDirectory = Paths.get("target", "test-tmp", "xml-parser");
        Files.createDirectories(baseDirectory);
        Path xmlPath = Files.createTempFile(baseDirectory, "nested-", ".xml");

        String xml = "<scenarios>\n"
                + "  <scenario id=\"scenario-a\" pluginId=\"adaptiflow.dynamic\" intervalMs=\"1000\">\n"
                + "    <events>\n"
                + "      <event id=\"event-a\" type=\"ConditionalEvent\">\n"
                + "        <collector type=\"ResourceUsageCollector\">\n"
                + "          <parameters>\n"
                + "            <parameter key=\"constructorArgTypes\">\n"
                + "              <list>\n"
                + "                <value>java.lang.String</value>\n"
                + "              </list>\n"
                + "            </parameter>\n"
                + "            <parameter key=\"constructorArgs\">\n"
                + "              <list>\n"
                + "                <value>cpu</value>\n"
                + "              </list>\n"
                + "            </parameter>\n"
                + "          </parameters>\n"
                + "        </collector>\n"
                + "        <evaluators>\n"
                + "          <evaluator type=\"TrueEvaluator\"/>\n"
                + "        </evaluators>\n"
                + "      </event>\n"
                + "    </events>\n"
                + "  </scenario>\n"
                + "</scenarios>\n";

        Files.writeString(xmlPath, xml);

        Map<String, Object> parsed = XmlScenarioMapParser.parse(xmlPath);

        @SuppressWarnings("unchecked")
        Map<String, Object> scenarios = (Map<String, Object>) parsed.get("scenarios");
        @SuppressWarnings("unchecked")
        Map<String, Object> scenarioMap = (Map<String, Object>) scenarios.get("scenario-a");
        @SuppressWarnings("unchecked")
        List<Object> events = (List<Object>) scenarioMap.get("events");
        @SuppressWarnings("unchecked")
        Map<String, Object> eventMap = (Map<String, Object>) events.get(0);
        @SuppressWarnings("unchecked")
        Map<String, Object> collectorMap = (Map<String, Object>) eventMap.get("collector");
        @SuppressWarnings("unchecked")
        Map<String, Object> parameters = (Map<String, Object>) collectorMap.get("parameters");

        Assert.assertEquals(List.of("java.lang.String"), parameters.get("constructorArgTypes"));
        Assert.assertEquals(List.of("cpu"), parameters.get("constructorArgs"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectScenarioWithoutIdAttribute() throws IOException {
        Path baseDirectory = Paths.get("target", "test-tmp", "xml-parser");
        Files.createDirectories(baseDirectory);
        Path xmlPath = Files.createTempFile(baseDirectory, "missing-id-", ".xml");

        String xml = "<scenarios>\n"
                + "  <scenario pluginId=\"adaptiflow.dynamic\">\n"
                + "    <events/>\n"
                + "  </scenario>\n"
                + "</scenarios>\n";

        Files.writeString(xmlPath, xml);
        XmlScenarioMapParser.parse(xmlPath);
    }
}
