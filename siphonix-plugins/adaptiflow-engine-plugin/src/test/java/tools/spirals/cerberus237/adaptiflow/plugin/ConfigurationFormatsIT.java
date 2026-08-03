package tools.spirals.cerberus237.adaptiflow.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.junit.Assert;
import org.junit.Test;

import tools.spirals.cerberus237.adaptiflow.plugin.core.config.AdaptiflowConfiguration;
import tools.spirals.cerberus237.adaptiflow.plugin.core.config.JsonConfigurationManager;
import tools.spirals.cerberus237.adaptiflow.plugin.core.config.XmlConfigurationManager;
import tools.spirals.cerberus237.adaptiflow.plugin.core.config.YamlConfigurationManager;

public class ConfigurationFormatsIT {

    @Test
    public void shouldLoadEquivalentScenarioFromYamlJsonAndXml() throws IOException {
        Path yamlPath = copyResource("scenarios/cache-size.yml", ".yml");
        Path jsonPath = copyResource("scenarios/cache-size.json", ".json");
        Path xmlPath = copyResource("scenarios/cache-size.xml", ".xml");

        AdaptiflowConfiguration yaml = new YamlConfigurationManager().load(yamlPath);
        AdaptiflowConfiguration json = new JsonConfigurationManager().load(jsonPath);
        AdaptiflowConfiguration xml = new XmlConfigurationManager().load(xmlPath);

        Assert.assertTrue(yaml.getScenarios().containsKey("cache-size"));
        Assert.assertTrue(json.getScenarios().containsKey("cache-size"));
        Assert.assertTrue(xml.getScenarios().containsKey("cache-size"));

        Assert.assertEquals(yaml.getScenarios().get("cache-size").getPluginId(),
                json.getScenarios().get("cache-size").getPluginId());
        Assert.assertEquals(yaml.getScenarios().get("cache-size").getPluginId(),
                xml.getScenarios().get("cache-size").getPluginId());

        Assert.assertTrue(yaml.getScenarios().get("cache-size").getEvents().size() > 0);
        Assert.assertTrue(json.getScenarios().get("cache-size").getEvents().size() > 0);
        Assert.assertTrue(xml.getScenarios().get("cache-size").getEvents().size() > 0);

        Assert.assertEquals("cache-size", yaml.getScenarios().get("cache-size").getId());
        Assert.assertEquals("cache-size", json.getScenarios().get("cache-size").getId());
        Assert.assertEquals("cache-size", xml.getScenarios().get("cache-size").getId());
    }

    private Path copyResource(String resource, String extension) throws IOException {
        Path baseDir = Path.of("target", "test-tmp", "format-it");
        Files.createDirectories(baseDir);
        Path target = Files.createTempFile(baseDir, "scenario-", extension);
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resource)) {
            Assert.assertNotNull("Missing resource: " + resource, inputStream);
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }
}
