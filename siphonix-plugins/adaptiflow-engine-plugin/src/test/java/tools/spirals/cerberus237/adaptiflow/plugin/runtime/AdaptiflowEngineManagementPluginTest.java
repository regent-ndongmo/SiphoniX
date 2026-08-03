package tools.spirals.cerberus237.adaptiflow.plugin.runtime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import tools.spirals.cerberus237.siphonix.api.plugin.PluginState;
import tools.spirals.cerberus237.siphonix.api.plugin.management.ScenarioSource;
import tools.spirals.cerberus237.siphonix.kernel.DefaultPluginContext;

public class AdaptiflowEngineManagementPluginTest {

    @Test
    public void shouldCreateEnableDisableAndDeleteScenarioThroughService() throws IOException {
        AdaptiflowEngineManagementPlugin plugin = new AdaptiflowEngineManagementPlugin();
        plugin.initialize(new DefaultPluginContext());

        ScenarioSource source = new InMemoryScenarioSource(baseScenarioMap("scenario-a"));
        plugin.getScenarioManagementService().createScenario(source);

        Assert.assertTrue(plugin.getScenarioManagementService().listScenarios().contains("scenario-a"));

        plugin.getScenarioManagementService().disableScenario("scenario-a");
        plugin.getScenarioManagementService().enableScenario("scenario-a");
        plugin.getScenarioManagementService().deleteScenario("scenario-a");

        Assert.assertFalse(plugin.getScenarioManagementService().listScenarios().contains("scenario-a"));
    }

    @Test
    public void shouldValidateInvalidScenarioAsFailure() throws IOException {
        AdaptiflowEngineManagementPlugin plugin = new AdaptiflowEngineManagementPlugin();

        ScenarioSource invalidSource = new InMemoryScenarioSource(Map.of(
                "scenarios", Map.of(
                        "invalid", Map.of(
                                "enabled", true,
                                "intervalMs", 1000,
                                "events", java.util.List.of()))));

        Assert.assertFalse(plugin.getScenarioManagementService().validateScenario(invalidSource).isValid());
    }

    @Test
    public void shouldDetectSourceTypeFromCliPath() throws IOException {
        Path baseDirectory = Paths.get("target", "test-tmp", "management-plugin");
        Files.createDirectories(baseDirectory);

        Path yamlPath = Files.createTempFile(baseDirectory, "scenario-", ".yml");
        Files.writeString(yamlPath,
                "scenarios:\n"
                        + "  cli-scenario:\n"
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
        plugin.getScenarioManagementCli().applyScenario(yamlPath.toString());

        Assert.assertTrue(plugin.getScenarioManagementService().listScenarios().contains("cli-scenario"));
    }

    @Test
    public void shouldTransitionPluginStateAcrossLifecycle() {
        AdaptiflowEngineManagementPlugin plugin = new AdaptiflowEngineManagementPlugin();

        Assert.assertEquals(PluginState.CREATED, plugin.getState());
        plugin.initialize(new DefaultPluginContext());
        Assert.assertEquals(PluginState.INITIALIZED, plugin.getState());
        plugin.start();
        Assert.assertEquals(PluginState.RUNNING, plugin.getState());
        plugin.stop();
        Assert.assertEquals(PluginState.STOPPED, plugin.getState());
    }

    private Map<String, Object> baseScenarioMap(String scenarioId) {
        return Map.of(
                "scenarios",
                Map.of(
                        scenarioId,
                        Map.of(
                                "pluginId", "adaptiflow.dynamic",
                                "enabled", true,
                                "intervalMs", 1000,
                                "events", java.util.List.of(
                                        Map.of(
                                                "id", "e1",
                                                "type", "ConditionalEvent",
                                                "collector", Map.of("type", "ResourceUsageCollector"),
                                                "evaluators", java.util.List.of(Map.of("type", "TrueEvaluator")))))));
    }

    private static final class InMemoryScenarioSource implements ScenarioSource {

        private final Map<String, Object> payload;

        private InMemoryScenarioSource(Map<String, Object> payload) {
            this.payload = payload;
        }

        @Override
        public String getType() {
            return "inline-map";
        }

        @Override
        public String getReference() {
            return "";
        }

        @Override
        public Map<String, Object> load() {
            return payload;
        }
    }
}
