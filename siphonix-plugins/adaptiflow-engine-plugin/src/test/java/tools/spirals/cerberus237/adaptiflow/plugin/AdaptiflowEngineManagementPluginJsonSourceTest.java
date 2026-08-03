package tools.spirals.cerberus237.adaptiflow.plugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import tools.spirals.cerberus237.adaptiflow.plugin.runtime.AdaptiflowEngineManagementPlugin;
import tools.spirals.cerberus237.siphonix.api.plugin.management.ScenarioSource;

public class AdaptiflowEngineManagementPluginJsonSourceTest {

    @Test
    public void shouldCreateScenarioFromJsonFileSource() throws IOException {
        Path jsonPath = writeJsonScenarioFile("json-service-source");

        AdaptiflowEngineManagementPlugin plugin = new AdaptiflowEngineManagementPlugin();
        plugin.getScenarioManagementService().createScenario(new FileScenarioSource("json-file", jsonPath));

        Assert.assertTrue(plugin.getScenarioManagementService().listScenarios().contains("json-service-source"));
    }

    @Test
    public void shouldCreateScenarioFromJsonPathThroughCli() throws IOException {
        Path jsonPath = writeJsonScenarioFile("json-cli-source");

        AdaptiflowEngineManagementPlugin plugin = new AdaptiflowEngineManagementPlugin();
        plugin.getScenarioManagementCli().applyScenario(jsonPath.toString());

        Assert.assertTrue(plugin.getScenarioManagementService().listScenarios().contains("json-cli-source"));
    }

    private Path writeJsonScenarioFile(String scenarioId) throws IOException {
        Path baseDirectory = Paths.get("target", "test-tmp", "management-json");
        Files.createDirectories(baseDirectory);
        Path jsonPath = Files.createTempFile(baseDirectory, scenarioId + "-", ".json");

        String json = "{\n"
                + "  \"scenarios\": {\n"
                + "    \"" + scenarioId + "\": {\n"
                + "      \"pluginId\": \"adaptiflow.dynamic\",\n"
                + "      \"enabled\": true,\n"
                + "      \"intervalMs\": 1000,\n"
                + "      \"events\": [\n"
                + "        {\n"
                + "          \"id\": \"cpu-watch\",\n"
                + "          \"type\": \"ConditionalEvent\",\n"
                + "          \"collector\": {\n"
                + "            \"type\": \"ResourceUsageCollector\"\n"
                + "          },\n"
                + "          \"evaluators\": [\n"
                + "            {\n"
                + "              \"type\": \"TrueEvaluator\"\n"
                + "            }\n"
                + "          ],\n"
                + "          \"actions\": [\n"
                + "            {\n"
                + "              \"type\": \"RestAdaptationAction\",\n"
                + "              \"parameters\": {\n"
                + "                \"constructorArgTypes\": [\"java.util.List\", \"java.lang.String\", \"java.lang.String\"],\n"
                + "                \"constructorArgs\": [[\"EnableExternalImageProvider\"], \"http://localhost/adapt\", \"EnableExternalImageProvider\"]\n"
                + "              }\n"
                + "            }\n"
                + "          ]\n"
                + "        }\n"
                + "      ]\n"
                + "    }\n"
                + "  }\n"
                + "}\n";

        Files.writeString(jsonPath, json);
        return jsonPath;
    }

    private static final class FileScenarioSource implements ScenarioSource {

        private final String type;
        private final Path path;

        private FileScenarioSource(String type, Path path) {
            this.type = type;
            this.path = path;
        }

        @Override
        public String getType() {
            return type;
        }

        @Override
        public String getReference() {
            return path.toString();
        }

        @Override
        public Map<String, Object> load() {
            return Map.of();
        }
    }
}