package tools.spirals.cerberus237.adaptiflow.plugin.core.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;

public class JsonConfigurationManagerTest {

    @Test
    public void shouldReturnEmptyConfigurationWhenJsonFileDoesNotExist() throws IOException {
        Path missingFile = Paths.get("target", "test-tmp", "json-config", "missing.json");
        Files.createDirectories(missingFile.getParent());
        Files.deleteIfExists(missingFile);

        JsonConfigurationManager manager = new JsonConfigurationManager();
        AdaptiflowConfiguration configuration = manager.load(missingFile);

        Assert.assertTrue(configuration.getScenarios().isEmpty());
    }

    @Test
    public void shouldLoadAndSaveJsonConfiguration() throws IOException {
        Path baseDirectory = Paths.get("target", "test-tmp", "json-config");
        Files.createDirectories(baseDirectory);
        Path jsonPath = Files.createTempFile(baseDirectory, "scenario-", ".json");

        String json = "{\n"
                + "  \"scenarios\": {\n"
                + "    \"cache-size\": {\n"
                + "      \"pluginId\": \"adaptiflow.cache-size\",\n"
                + "      \"enabled\": true,\n"
                + "      \"intervalMs\": 1000,\n"
                + "      \"events\": [\n"
                + "        {\n"
                + "          \"id\": \"event-a\",\n"
                + "          \"type\": \"ConditionalEvent\",\n"
                + "          \"collector\": {\n"
                + "            \"type\": \"ResourceUsageCollector\"\n"
                + "          },\n"
                + "          \"evaluators\": [\n"
                + "            {\n"
                + "              \"type\": \"TrueEvaluator\"\n"
                + "            }\n"
                + "          ]\n"
                + "        }\n"
                + "      ]\n"
                + "    }\n"
                + "  }\n"
                + "}\n";

        Files.writeString(jsonPath, json);

        JsonConfigurationManager manager = new JsonConfigurationManager();
        AdaptiflowConfiguration loaded = manager.load(jsonPath);

        Assert.assertEquals(1, loaded.getScenarios().size());
        Assert.assertNotNull(loaded.getScenarios().get("cache-size"));
        Assert.assertEquals("adaptiflow.cache-size", loaded.getScenarios().get("cache-size").getPluginId());

        Path outputPath = Files.createTempFile(baseDirectory, "scenario-roundtrip-", ".json");
        manager.save(outputPath, loaded);

        AdaptiflowConfiguration reloaded = manager.load(outputPath);
        Assert.assertEquals("adaptiflow.cache-size", reloaded.getScenarios().get("cache-size").getPluginId());
        Assert.assertEquals(1000, reloaded.getScenarios().get("cache-size").getIntervalMs());
    }

    @Test(expected = InvalidConfigurationException.class)
    public void shouldRejectScenarioWithoutPluginId() throws IOException {
        Path baseDirectory = Paths.get("target", "test-tmp", "json-config");
        Files.createDirectories(baseDirectory);
        Path jsonPath = Files.createTempFile(baseDirectory, "invalid-", ".json");

        String json = "{\n"
                + "  \"scenarios\": {\n"
                + "    \"broken\": {\n"
                + "      \"enabled\": true,\n"
                + "      \"intervalMs\": 1000,\n"
                + "      \"events\": []\n"
                + "    }\n"
                + "  }\n"
                + "}\n";

        Files.writeString(jsonPath, json);

        JsonConfigurationManager manager = new JsonConfigurationManager();
        manager.load(jsonPath);
    }
}
