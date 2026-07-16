package tools.spirals.cerberus237.siphonix.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import tools.spirals.cerberus237.siphonix.scenarios.ScenarioDefinition;

public class YamlConfigurationManagerTest {

    private Path createWritableTempFile(String prefix) throws IOException {
        Path baseDirectory = Paths.get("target", "test-tmp", "config-tests");
        Files.createDirectories(baseDirectory);
        return Files.createTempFile(baseDirectory, prefix, ".yml");
    }

    private Path createWritableTempDirectory(String prefix) throws IOException {
        Path baseDirectory = Paths.get("target", "test-tmp", "config-tests");
        Files.createDirectories(baseDirectory);
        return Files.createTempDirectory(baseDirectory, prefix);
    }

    @Test
    public void shouldLoadScenarioDefinitionsFromYaml() throws IOException {
        Path tempFile = createWritableTempFile("siphonix-config");
        Files.writeString(tempFile,
                "scenarios:\n"
                        + "  cache-observation:\n"
                        + "    pluginId: adaptiflow.cache-size\n"
                        + "    enabled: true\n"
                        + "    intervalMs: 6000\n"
                        + "    thresholds:\n"
                        + "      high: 80\n"
                        + "    actions:\n"
                        + "      - EnableExternalImageProvider\n");

        YamlConfigurationManager manager = new YamlConfigurationManager();
        SiphonixConfiguration configuration = manager.load(tempFile);

        ScenarioDefinition definition = configuration.getScenarios().get("cache-observation");
        Assert.assertNotNull(definition);
        Assert.assertEquals("adaptiflow.cache-size", definition.getPluginId());
        Assert.assertEquals(6000, definition.getIntervalMs());
        Assert.assertEquals(80, ((Number) definition.getThresholds().get("high")).intValue());
        Assert.assertEquals(1, definition.getActions().size());
    }

    @Test
    public void shouldSaveAndReloadConfiguration() throws IOException {
        Path tempDirectory = createWritableTempDirectory("siphonix-config-dir");
        Path configPath = tempDirectory.resolve("scenarios.yml");

        ScenarioDefinition scenario = new ScenarioDefinition();
        scenario.setId("db-availability");
        scenario.setPluginId("adaptiflow.database-availability");
        scenario.setEnabled(false);
        scenario.setIntervalMs(3000);
        Map<String, Object> thresholds = new LinkedHashMap<>();
        thresholds.put("cpuMax", 75);
        scenario.setThresholds(thresholds);
        scenario.setActions(List.of("DatabaseUnavailableEventBroadcast"));

        SiphonixConfiguration configuration = new SiphonixConfiguration();
        configuration.getScenarios().put("db-availability", scenario);

        YamlConfigurationManager manager = new YamlConfigurationManager();
        manager.save(configPath, configuration);

        SiphonixConfiguration reloaded = manager.load(configPath);
        ScenarioDefinition reloadedScenario = reloaded.getScenarios().get("db-availability");

        Assert.assertNotNull(reloadedScenario);
        Assert.assertEquals("adaptiflow.database-availability", reloadedScenario.getPluginId());
        Assert.assertFalse(reloadedScenario.isEnabled());
        Assert.assertEquals(3000, reloadedScenario.getIntervalMs());
        Assert.assertEquals("DatabaseUnavailableEventBroadcast", reloadedScenario.getActions().get(0));
    }

    @Test(expected = InvalidConfigurationException.class)
    public void shouldRejectScenarioWithoutPluginId() throws IOException {
        Path tempFile = createWritableTempFile("siphonix-invalid");
        Files.writeString(tempFile,
                "scenarios:\n"
                        + "  broken-scenario:\n"
                        + "    intervalMs: 1000\n");

        YamlConfigurationManager manager = new YamlConfigurationManager();
        manager.load(tempFile);
    }
}
