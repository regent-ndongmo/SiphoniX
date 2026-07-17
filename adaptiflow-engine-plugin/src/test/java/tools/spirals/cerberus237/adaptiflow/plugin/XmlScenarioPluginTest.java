package tools.spirals.cerberus237.adaptiflow.plugin;

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import tools.spirals.cerberus237.adaptiflow.plugin.core.config.InvalidConfigurationException;
import tools.spirals.cerberus237.adaptiflow.plugin.core.scenarios.ActionDefinition;
import tools.spirals.cerberus237.adaptiflow.plugin.core.scenarios.ConditionalEvaluatorDefinition;
import tools.spirals.cerberus237.adaptiflow.plugin.core.scenarios.EventDefinition;
import tools.spirals.cerberus237.adaptiflow.plugin.core.scenarios.MetricCollectorDefinition;
import tools.spirals.cerberus237.adaptiflow.plugin.core.scenarios.ScenarioDefinition;
import tools.spirals.cerberus237.adaptiflow.plugin.runtime.XmlScenarioPlugin;
import tools.spirals.cerberus237.siphonix.api.plugin.PluginState;
import tools.spirals.cerberus237.siphonix.kernel.DefaultPluginContext;

public class XmlScenarioPluginTest {

    @Test
    public void shouldInitializeAndRunXmlScenarioPlugin() {
        ScenarioDefinition definition = createValidScenario("xml-dynamic");

        XmlScenarioPlugin plugin = new XmlScenarioPlugin(definition);
        plugin.initialize(new DefaultPluginContext());
        plugin.start();
        plugin.stop();

        Assert.assertEquals("scenario.xml-dynamic", plugin.getId());
        Assert.assertEquals(PluginState.STOPPED, plugin.getState());
    }

    @Test(expected = InvalidConfigurationException.class)
    public void shouldRejectInvalidXmlScenarioPluginConfiguration() {
        ScenarioDefinition definition = createValidScenario("xml-invalid");
        definition.getEvents().get(0).getEvaluators().get(0).setType("UnknownEvaluator");

        XmlScenarioPlugin plugin = new XmlScenarioPlugin(definition);
        plugin.initialize(new DefaultPluginContext());
    }

    private ScenarioDefinition createValidScenario(String scenarioId) {
        ScenarioDefinition definition = new ScenarioDefinition();
        definition.setId(scenarioId);
        definition.setPluginId("adaptiflow.dynamic");
        definition.setIntervalMs(1000);

        MetricCollectorDefinition collector = new MetricCollectorDefinition();
        collector.setType("ResourceUsageCollector");

        ConditionalEvaluatorDefinition evaluator = new ConditionalEvaluatorDefinition();
        evaluator.setType("TrueEvaluator");

        ActionDefinition action = new ActionDefinition();
        action.setType("RestAdaptationAction");
        action.setParameters(Map.of(
                "constructorArgTypes", List.of("java.util.List", "java.lang.String", "java.lang.String"),
                "constructorArgs", List.of(List.of("EnableExternalImageProvider"),
                        "http://localhost/adapt",
                        "EnableExternalImageProvider")));

        EventDefinition event = new EventDefinition();
        event.setId("cpu-watch");
        event.setType("ConditionalEvent");
        event.setCollector(collector);
        event.setEvaluators(List.of(evaluator));
        event.setActions(List.of(action));

        definition.setEvents(List.of(event));
        return definition;
    }
}