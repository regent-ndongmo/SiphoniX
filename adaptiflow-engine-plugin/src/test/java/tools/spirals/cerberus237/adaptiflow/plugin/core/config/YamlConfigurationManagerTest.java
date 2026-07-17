package tools.spirals.cerberus237.adaptiflow.plugin.core.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import tools.spirals.cerberus237.adaptiflow.plugin.core.scenarios.ActionDefinition;
import tools.spirals.cerberus237.adaptiflow.plugin.core.scenarios.ConditionalEvaluatorDefinition;
import tools.spirals.cerberus237.adaptiflow.plugin.core.scenarios.EventDefinition;
import tools.spirals.cerberus237.adaptiflow.plugin.core.scenarios.MetricCollectorDefinition;
import tools.spirals.cerberus237.adaptiflow.plugin.core.scenarios.ScenarioDefinition;
import tools.spirals.cerberus237.adaptiflow.plugin.core.scenarios.SubscriberDefinition;

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
                + "    events:\n"
                + "      - id: cpu-spike\n"
                + "        type: ConditionalEvent\n"
                + "        collector:\n"
                        + "          type: RestMetricsCollector\n"
                + "          parameters:\n"
                + "            endpoint: /metrics/cpu\n"
                        + "            method: GET\n"
                        + "            responseType: java.util.HashMap\n"
                + "        evaluators:\n"
                        + "          - type: IncreaseResourceUsageEvaluator\n"
                + "            parameters:\n"
                        + "              constructorArgTypes:\n"
                        + "                - java.util.function.Supplier\n"
                        + "                - java.util.function.Supplier\n"
                        + "              constructorArgs:\n"
                        + "                - 75\n"
                        + "                - 80\n"
                + "        subscribers:\n"
                        + "          - type: EventSubscriber\n"
                + "            actions:\n"
                        + "              - type: RestAdaptationAction\n"
                + "                parameters:\n"
                        + "                  constructorArgTypes:\n"
                        + "                    - java.lang.String\n"
                        + "                    - java.lang.String\n"
                        + "                  constructorArgs:\n"
                        + "                    - http://localhost/adapt\n"
                        + "                    - EnableExternalImageProvider\n");

        YamlConfigurationManager manager = new YamlConfigurationManager();
        AdaptiflowConfiguration configuration = manager.load(tempFile);

        ScenarioDefinition definition = configuration.getScenarios().get("cache-observation");
        Assert.assertNotNull(definition);
        Assert.assertEquals("adaptiflow.cache-size", definition.getPluginId());
        Assert.assertEquals(6000, definition.getIntervalMs());
        Assert.assertEquals(1, definition.getEvents().size());
        EventDefinition event = definition.getEvents().get(0);
        Assert.assertEquals("cpu-spike", event.getId());
        Assert.assertEquals("ConditionalEvent", event.getType());
        Assert.assertEquals("RestMetricsCollector", event.getCollector().getType());
        Assert.assertEquals("/metrics/cpu", event.getCollector().getParameters().get("endpoint"));
        Assert.assertEquals(1, event.getEvaluators().size());
        Assert.assertEquals("IncreaseResourceUsageEvaluator", event.getEvaluators().get(0).getType());
        Assert.assertEquals(80, ((Number) ((List<?>) event.getEvaluators().get(0).getParameters().get("constructorArgs")).get(1)).intValue());
        Assert.assertEquals(1, event.getSubscribers().size());
        Assert.assertEquals("EventSubscriber", event.getSubscribers().get(0).getType());
        Assert.assertEquals(1, event.getSubscribers().get(0).getActions().size());
        Assert.assertEquals("RestAdaptationAction", event.getSubscribers().get(0).getActions().get(0).getType());
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

        EventDefinition event = new EventDefinition();
        event.setId("db-unavailable");
        event.setType("ConditionalEvent");

        MetricCollectorDefinition collector = new MetricCollectorDefinition();
        collector.setType("RestMetricsCollector");
        collector.setParameters(Map.of("endpoint", "/metrics/database"));
        event.setCollector(collector);

        ConditionalEvaluatorDefinition evaluator = new ConditionalEvaluatorDefinition();
        evaluator.setType("DecreaseResourceUsageEvaluator");
        evaluator.setParameters(new LinkedHashMap<>(Map.of(
            "constructorArgTypes", List.of("java.util.function.Supplier", "java.util.function.Supplier"),
            "constructorArgs", List.of(60, 55))));
        event.setEvaluators(List.of(evaluator));

        ActionDefinition action = new ActionDefinition();
        action.setType("RestAdaptationAction");
        action.setParameters(new LinkedHashMap<>(Map.of(
            "constructorArgTypes", List.of("java.util.List", "java.lang.String", "java.lang.String"),
            "constructorArgs", List.of(
                List.of("DatabaseUnavailableEventBroadcast"),
                "http://localhost/adapt",
                "DatabaseUnavailableEventBroadcast"))));
        SubscriberDefinition subscriber = new SubscriberDefinition();
        subscriber.setType("EventSubscriber");
        subscriber.setActions(List.of(action));
        event.setSubscribers(List.of(subscriber));

        scenario.setEvents(List.of(event));

        AdaptiflowConfiguration configuration = new AdaptiflowConfiguration();
        configuration.getScenarios().put("db-availability", scenario);

        YamlConfigurationManager manager = new YamlConfigurationManager();
        manager.save(configPath, configuration);

        AdaptiflowConfiguration reloaded = manager.load(configPath);
        ScenarioDefinition reloadedScenario = reloaded.getScenarios().get("db-availability");

        Assert.assertNotNull(reloadedScenario);
        Assert.assertEquals("adaptiflow.database-availability", reloadedScenario.getPluginId());
        Assert.assertFalse(reloadedScenario.isEnabled());
        Assert.assertEquals(3000, reloadedScenario.getIntervalMs());
        Assert.assertEquals(1, reloadedScenario.getEvents().size());
        EventDefinition reloadedEvent = reloadedScenario.getEvents().get(0);
        Assert.assertEquals("db-unavailable", reloadedEvent.getId());
        Assert.assertEquals("ConditionalEvent", reloadedEvent.getType());
        Assert.assertEquals("RestMetricsCollector", reloadedEvent.getCollector().getType());
        Assert.assertEquals("DecreaseResourceUsageEvaluator", reloadedEvent.getEvaluators().get(0).getType());
        Assert.assertEquals(1, reloadedEvent.getSubscribers().size());
        Assert.assertEquals("EventSubscriber", reloadedEvent.getSubscribers().get(0).getType());
        Assert.assertEquals("RestAdaptationAction", reloadedEvent.getSubscribers().get(0).getActions().get(0).getType());
    }

    @Test(expected = InvalidConfigurationException.class)
    public void shouldRejectScenarioWithoutPluginId() throws IOException {
        Path tempFile = createWritableTempFile("siphonix-invalid");
        Files.writeString(tempFile,
                "scenarios:\n"
                        + "  broken-scenario:\n"
                        + "    pluginId: adaptiflow.cache-size\n"
                        + "    intervalMs: 1000\n");

        YamlConfigurationManager manager = new YamlConfigurationManager();
        manager.load(tempFile);
    }

    @Test(expected = InvalidConfigurationException.class)
    public void shouldRejectEventWithoutEvaluators() throws IOException {
        Path tempFile = createWritableTempFile("siphonix-invalid-evaluator");
        Files.writeString(tempFile,
                "scenarios:\n"
                        + "  invalid-event:\n"
                        + "    pluginId: adaptiflow.cache-size\n"
                        + "    intervalMs: 1000\n"
                        + "    events:\n"
                        + "      - id: no-evaluator\n"
                        + "        collector:\n"
                        + "          type: RestMetricsCollector\n"
                        + "        actions:\n"
                        + "          - type: RestAdaptationAction\n");

        YamlConfigurationManager manager = new YamlConfigurationManager();
        manager.load(tempFile);
    }

        @Test
        public void shouldAllowEventWithSubscribersAndEmptyActions() throws IOException {
                Path tempFile = createWritableTempFile("siphonix-empty-actions");
                Files.writeString(tempFile,
                                "scenarios:\n"
                                                + "  cache-observation:\n"
                                                + "    pluginId: adaptiflow.cache-size\n"
                                                + "    enabled: true\n"
                                                + "    intervalMs: 5000\n"
                                                + "    events:\n"
                                                + "      - id: cache-watch\n"
                                                + "        type: ConditionalEvent\n"
                                                + "        collector:\n"
                                                + "          type: RestMetricsCollector\n"
                                                + "        evaluators:\n"
                                                + "          - type: TrueEvaluator\n"
                                                + "        subscribers:\n"
                                                + "          - type: EventCounterSubscriber\n"
                                                + "            parameters:\n"
                                                + "              constructorArgTypes:\n"
                                                + "                - int\n"
                                                + "              constructorArgs:\n"
                                                + "                - 3\n");

                YamlConfigurationManager manager = new YamlConfigurationManager();
                AdaptiflowConfiguration configuration = manager.load(tempFile);

                EventDefinition event = configuration.getScenarios().get("cache-observation").getEvents().get(0);
                Assert.assertEquals(1, event.getSubscribers().size());
                Assert.assertEquals("EventCounterSubscriber", event.getSubscribers().get(0).getType());
                Assert.assertEquals(0, event.getSubscribers().get(0).getActions().size());
                Assert.assertEquals(3,
                                ((Number) ((List<?>) event.getSubscribers().get(0).getParameters().get("constructorArgs")).get(0))
                                                .intValue());
        }
}
