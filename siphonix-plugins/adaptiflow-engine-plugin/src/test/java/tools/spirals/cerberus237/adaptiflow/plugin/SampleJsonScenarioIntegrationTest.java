package tools.spirals.cerberus237.adaptiflow.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import tools.spirals.cerberus237.adaptiflow.plugin.core.config.AdaptiflowConfiguration;
import tools.spirals.cerberus237.adaptiflow.plugin.core.config.JsonConfigurationManager;
import tools.spirals.cerberus237.adaptiflow.plugin.core.scenarios.ConditionalEvaluatorDefinition;
import tools.spirals.cerberus237.adaptiflow.plugin.core.scenarios.ScenarioDefinition;
import tools.spirals.cerberus237.adaptiflow.plugin.runtime.ScenarioPlugin;
import tools.spirals.cerberus237.adaptiflow.plugin.runtime.ScenarioRuntimeFactoryImpl;
import tools.spirals.cerberus237.siphonix.api.plugin.PluginState;
import tools.spirals.cerberus237.siphonix.kernel.DefaultPluginContext;

public class SampleJsonScenarioIntegrationTest {

    @Test
    public void shouldLoadAndInitializeBeninTrafficScenarioFromSampleJson() throws IOException {
        ScenarioDefinition scenario = loadScenario("benin-traffic.json", "benin-traffic");

        Assert.assertEquals(2, scenario.getEvents().size());
        Assert.assertEquals("IncreaseResourceUsageEvaluator", scenario.getEvents().get(0).getEvaluators().get(0).getType());
        Assert.assertEquals("DecreaseResourceUsageEvaluator", scenario.getEvents().get(1).getEvaluators().get(0).getType());

        assertSupplierThresholds(scenario.getEvents().get(0).getEvaluators().get(0), List.of(75.0, 80.0));
        assertSupplierThresholds(scenario.getEvents().get(1).getEvaluators().get(0), List.of(60.0, 60.0));

        ScenarioPlugin plugin = new ScenarioPlugin(scenario, new ScenarioRuntimeFactoryImpl());
        plugin.initialize(new DefaultPluginContext());

        Assert.assertEquals("scenario.benin-traffic", plugin.getId());
        Assert.assertEquals(PluginState.INITIALIZED, plugin.getState());
    }

    @Test
    public void shouldLoadAndInitializeDatabaseAvailabilityScenarioFromSampleJson() throws IOException {
        ScenarioDefinition scenario = loadScenario("database-availability.json", "database-availability");

        Assert.assertEquals(2, scenario.getEvents().size());
        Assert.assertEquals("IncreaseResourceUsageEvaluator", scenario.getEvents().get(0).getEvaluators().get(0).getType());
        Assert.assertEquals("DecreaseResourceUsageEvaluator", scenario.getEvents().get(1).getEvaluators().get(0).getType());

        assertSupplierThresholds(scenario.getEvents().get(0).getEvaluators().get(0), List.of(75.0, 80.0));
        assertSupplierThresholds(scenario.getEvents().get(1).getEvaluators().get(0), List.of(60.0, 60.0));

        ScenarioPlugin plugin = new ScenarioPlugin(scenario, new ScenarioRuntimeFactoryImpl());
        plugin.initialize(new DefaultPluginContext());

        Assert.assertEquals("scenario.database-availability", plugin.getId());
        Assert.assertEquals(PluginState.INITIALIZED, plugin.getState());
    }

    @Test
    public void shouldLoadAndInitializeCacheSizeScenarioFromSampleJson() throws IOException {
        ScenarioDefinition scenario = loadScenario("cache-size.json", "cache-size");

        Assert.assertEquals(4, scenario.getEvents().size());
        Assert.assertEquals("RestMetricsCollector", scenario.getEvents().get(0).getCollector().getType());
        Assert.assertEquals("TrueEvaluator", scenario.getEvents().get(0).getEvaluators().get(0).getType());

        ScenarioPlugin plugin = new ScenarioPlugin(scenario, new ScenarioRuntimeFactoryImpl());
        plugin.initialize(new DefaultPluginContext());

        Assert.assertEquals("scenario.cache-size", plugin.getId());
        Assert.assertEquals(PluginState.INITIALIZED, plugin.getState());
    }

    private ScenarioDefinition loadScenario(String fileName, String scenarioKey) throws IOException {
        Path jsonPath = copyResourceToTempFile(fileName, ".json");

        JsonConfigurationManager manager = new JsonConfigurationManager();
        AdaptiflowConfiguration configuration = manager.load(jsonPath);

        ScenarioDefinition scenario = configuration.getScenarios().get(scenarioKey);
        Assert.assertNotNull("Scenario key not found in JSON: " + scenarioKey, scenario);
        return scenario;
    }

    private Path copyResourceToTempFile(String fileName, String extension) throws IOException {
        String resourcePath = "scenarios/" + fileName;
        Path baseDirectory = Path.of("target", "test-tmp", "scenario-integration-json");
        Files.createDirectories(baseDirectory);
        Path tempFile = Files.createTempFile(baseDirectory, "scenario-", extension);

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            Assert.assertNotNull("Missing test resource: " + resourcePath, inputStream);
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }
        return tempFile;
    }

    private void assertSupplierThresholds(ConditionalEvaluatorDefinition evaluator, List<Double> expectedThresholds) {
        Object rawArgTypes = evaluator.getParameters().get("constructorArgTypes");
        Object rawArgs = evaluator.getParameters().get("constructorArgs");

        Assert.assertTrue(rawArgTypes instanceof List);
        Assert.assertTrue(rawArgs instanceof List);

        @SuppressWarnings("unchecked")
        List<Object> argTypes = (List<Object>) rawArgTypes;
        @SuppressWarnings("unchecked")
        List<Object> args = (List<Object>) rawArgs;

        Assert.assertEquals(List.of(
                "tools.spirals.cerberus237.adaptiflow.interfaces.ThresholdProvider",
                "tools.spirals.cerberus237.adaptiflow.interfaces.ThresholdProvider"), argTypes);
        Assert.assertEquals(2, args.size());

        Assert.assertEquals(expectedThresholds.get(0), ((Number) args.get(0)).doubleValue(), 0.0001);
        Assert.assertEquals(expectedThresholds.get(1), ((Number) args.get(1)).doubleValue(), 0.0001);
    }
}