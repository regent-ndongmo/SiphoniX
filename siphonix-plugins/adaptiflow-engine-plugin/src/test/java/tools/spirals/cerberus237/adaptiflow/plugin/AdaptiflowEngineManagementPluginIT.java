package tools.spirals.cerberus237.adaptiflow.plugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import tools.spirals.cerberus237.adaptiflow.plugin.runtime.AdaptiflowEngineManagementPlugin;
import tools.spirals.cerberus237.siphonix.api.plugin.PluginState;
import tools.spirals.cerberus237.siphonix.api.plugin.management.ScenarioSource;
import tools.spirals.cerberus237.siphonix.kernel.DefaultPluginContext;

public class AdaptiflowEngineManagementPluginIT {

    @Test
    public void shouldRunCrudScenarioLifecycleAcrossFormats() throws IOException {
        Path baseDirectory = Paths.get("target", "test-tmp", "management-it");
        Files.createDirectories(baseDirectory);

        Path yamlPath = Files.createTempFile(baseDirectory, "scenario-", ".yml");
        Files.writeString(yamlPath,
                "scenarios:\n"
                        + "  scenario-it:\n"
                        + "    pluginId: adaptiflow.dynamic\n"
                        + "    enabled: true\n"
                        + "    intervalMs: 1000\n"
                        + "    events:\n"
                        + "      - id: e1\n"
                        + "        type: ConditionalEvent\n"
                        + "        collector:\n"
                        + "          type: ResourceUsageCollector\n"
                        + "        evaluators:\n"
                        + "          - type: TrueEvaluator\n");

        AdaptiflowEngineManagementPlugin plugin = new AdaptiflowEngineManagementPlugin();
        plugin.initialize(new DefaultPluginContext());

        plugin.getScenarioManagementService().createScenario(new FileScenarioSource("yaml-file", yamlPath));
        Assert.assertTrue(plugin.getScenarioManagementService().listScenarios().contains("scenario-it"));

        plugin.start();
        Assert.assertEquals(PluginState.RUNNING, plugin.getState());

        plugin.getScenarioManagementService().disableScenario("scenario-it");
        plugin.getScenarioManagementService().enableScenario("scenario-it");

        Path jsonPath = Files.createTempFile(baseDirectory, "scenario-", ".json");
        Files.writeString(jsonPath,
                "{\n"
                        + "  \"scenarios\": {\n"
                        + "    \"scenario-it\": {\n"
                        + "      \"pluginId\": \"adaptiflow.dynamic\",\n"
                        + "      \"enabled\": true,\n"
                        + "      \"intervalMs\": 1100,\n"
                        + "      \"events\": [\n"
                        + "        {\n"
                        + "          \"id\": \"e2\",\n"
                        + "          \"type\": \"ConditionalEvent\",\n"
                        + "          \"collector\": {\"type\": \"ResourceUsageCollector\"},\n"
                        + "          \"evaluators\": [{\"type\": \"TrueEvaluator\"}]\n"
                        + "        }\n"
                        + "      ]\n"
                        + "    }\n"
                        + "  }\n"
                        + "}\n");

        plugin.getScenarioManagementService().updateScenario("scenario-it", new FileScenarioSource("json-file", jsonPath));
        Assert.assertTrue(plugin.getScenarioManagementService().listScenarios().contains("scenario-it"));

        plugin.getScenarioManagementService().deleteScenario("scenario-it");
        Assert.assertFalse(plugin.getScenarioManagementService().listScenarios().contains("scenario-it"));

        plugin.stop();
        Assert.assertEquals(PluginState.STOPPED, plugin.getState());
    }

    @Test
    public void shouldValidateXmlScenarioSource() throws IOException {
        Path baseDirectory = Paths.get("target", "test-tmp", "management-it");
        Files.createDirectories(baseDirectory);
        Path xmlPath = Files.createTempFile(baseDirectory, "scenario-", ".xml");

        Files.writeString(xmlPath,
                "<scenarios>\n"
                        + "  <scenario id=\"xml-it\" pluginId=\"adaptiflow.dynamic\" enabled=\"true\" intervalMs=\"1000\">\n"
                        + "    <events>\n"
                        + "      <event id=\"e1\" type=\"ConditionalEvent\">\n"
                        + "        <collector type=\"ResourceUsageCollector\"/>\n"
                        + "        <evaluators>\n"
                        + "          <evaluator type=\"TrueEvaluator\"/>\n"
                        + "        </evaluators>\n"
                        + "      </event>\n"
                        + "    </events>\n"
                        + "  </scenario>\n"
                        + "</scenarios>\n");

        AdaptiflowEngineManagementPlugin plugin = new AdaptiflowEngineManagementPlugin();
        Assert.assertTrue(plugin.getScenarioManagementService()
                .validateScenario(new FileScenarioSource("xml-file", xmlPath)).isValid());
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
