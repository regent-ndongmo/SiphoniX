package tools.spirals.cerberus237.adaptiflow.plugin.core.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;

public class XmlConfigurationManagerTest {

    @Test
    public void shouldReturnEmptyConfigurationWhenXmlFileDoesNotExist() throws IOException {
        Path missingFile = Paths.get("target", "test-tmp", "xml-config", "missing.xml");
        Files.createDirectories(missingFile.getParent());
        Files.deleteIfExists(missingFile);

        XmlConfigurationManager manager = new XmlConfigurationManager();
        AdaptiflowConfiguration configuration = manager.load(missingFile);

        Assert.assertTrue(configuration.getScenarios().isEmpty());
    }

    @Test
    public void shouldLoadXmlConfiguration() throws IOException {
        Path baseDirectory = Paths.get("target", "test-tmp", "xml-config");
        Files.createDirectories(baseDirectory);
        Path xmlPath = Files.createTempFile(baseDirectory, "scenario-", ".xml");

        String xml = "<scenarios>\n"
                + "  <scenario id=\"cache-size\" pluginId=\"adaptiflow.cache-size\" enabled=\"true\" intervalMs=\"1000\">\n"
                + "    <events>\n"
                + "      <event id=\"event-a\" type=\"ConditionalEvent\">\n"
                + "        <collector type=\"ResourceUsageCollector\"/>\n"
                + "        <evaluators>\n"
                + "          <evaluator type=\"TrueEvaluator\"/>\n"
                + "        </evaluators>\n"
                + "      </event>\n"
                + "    </events>\n"
                + "  </scenario>\n"
                + "</scenarios>\n";

        Files.writeString(xmlPath, xml);

        XmlConfigurationManager manager = new XmlConfigurationManager();
        AdaptiflowConfiguration configuration = manager.load(xmlPath);

        Assert.assertEquals(1, configuration.getScenarios().size());
        Assert.assertEquals("adaptiflow.cache-size", configuration.getScenarios().get("cache-size").getPluginId());
        Assert.assertEquals(1, configuration.getScenarios().get("cache-size").getEvents().size());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void shouldRejectXmlSave() {
        XmlConfigurationManager manager = new XmlConfigurationManager();
        manager.save(Path.of("target", "test-tmp", "xml-config", "out.xml"), new AdaptiflowConfiguration());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectInvalidRootElement() throws IOException {
        Path baseDirectory = Paths.get("target", "test-tmp", "xml-config");
        Files.createDirectories(baseDirectory);
        Path xmlPath = Files.createTempFile(baseDirectory, "invalid-root-", ".xml");
        Files.writeString(xmlPath, "<invalidRoot/>\n");

        XmlConfigurationManager manager = new XmlConfigurationManager();
        manager.load(xmlPath);
    }
}
