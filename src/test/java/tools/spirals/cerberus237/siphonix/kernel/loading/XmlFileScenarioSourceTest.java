package tools.spirals.cerberus237.siphonix.kernel.loading;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class XmlFileScenarioSourceTest {

    @Test
    public void shouldExposeXmlTypeAndReferenceWithoutParsing() throws IOException {
        Path xmlPath = createScenarioXml();

        XmlFileScenarioSource source = new XmlFileScenarioSource(xmlPath);
        Map<String, Object> root = source.load();

        Assert.assertEquals("xml-file", source.getType());
        Assert.assertEquals(xmlPath.toString(), source.getReference());
        Assert.assertTrue(root.isEmpty());
    }

    private Path createScenarioXml() throws IOException {
        Path baseDirectory = Paths.get("target", "test-tmp", "xml-source");
        Files.createDirectories(baseDirectory);
        Path xmlPath = Files.createTempFile(baseDirectory, "scenario-", ".xml");

        Files.writeString(xmlPath,
                "<scenarios>\n"
                        + "  <scenario id=\"xml-smoke\" pluginId=\"adaptiflow.dynamic\" enabled=\"true\" intervalMs=\"1000\">\n"
                        + "    <events>\n"
                        + "      <event id=\"cpu-watch\" type=\"ConditionalEvent\">\n"
                        + "        <collector type=\"ResourceUsageCollector\"/>\n"
                        + "        <evaluators>\n"
                        + "          <evaluator type=\"TrueEvaluator\"/>\n"
                        + "        </evaluators>\n"
                        + "        <actions>\n"
                        + "          <action type=\"RestAdaptationAction\"/>\n"
                        + "        </actions>\n"
                        + "      </event>\n"
                        + "    </events>\n"
                        + "  </scenario>\n"
                        + "</scenarios>\n");
        return xmlPath;
    }
}