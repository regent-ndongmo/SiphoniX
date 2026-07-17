package tools.spirals.cerberus237.adaptiflow.plugin;

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import tools.spirals.cerberus237.siphonix.api.plugin.PluginState;
import tools.spirals.cerberus237.siphonix.kernel.DefaultPluginContext;
import tools.spirals.cerberus237.adaptiflow.plugin.core.config.InvalidConfigurationException;
import tools.spirals.cerberus237.adaptiflow.plugin.core.scenarios.ActionDefinition;
import tools.spirals.cerberus237.adaptiflow.plugin.core.scenarios.ConditionalEvaluatorDefinition;
import tools.spirals.cerberus237.adaptiflow.plugin.core.scenarios.EventDefinition;
import tools.spirals.cerberus237.adaptiflow.plugin.core.scenarios.MetricCollectorDefinition;
import tools.spirals.cerberus237.adaptiflow.plugin.core.scenarios.ScenarioDefinition;
import tools.spirals.cerberus237.adaptiflow.plugin.core.scenarios.SubscriberDefinition;
import tools.spirals.cerberus237.adaptiflow.plugin.runtime.ScenarioPlugin;
import tools.spirals.cerberus237.adaptiflow.plugin.runtime.ScenarioRuntimeFactoryImpl;

public class YamlScenarioPluginTest {

    @Test
    public void shouldInitializeAndRunYamlScenarioPlugin() {
        ScenarioDefinition definition = new ScenarioDefinition();
        definition.setId("dynamic-cache");
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
            "constructorArgs", List.of(
                List.of("EnableExternalImageProvider"),
                "http://localhost/adapt",
                "EnableExternalImageProvider")));

        EventDefinition event = new EventDefinition();
        event.setId("cpu-watch");
        event.setType("ConditionalEvent");
        event.setCollector(collector);
        event.setEvaluators(List.of(evaluator));

        SubscriberDefinition subscriber = new SubscriberDefinition();
        subscriber.setType("EventSubscriber");
        subscriber.setActions(List.of(action));
        event.setSubscribers(List.of(subscriber));

        definition.setEvents(List.of(event));

        ScenarioPlugin plugin = new ScenarioPlugin(definition, new ScenarioRuntimeFactoryImpl());
        plugin.initialize(new DefaultPluginContext());
        plugin.start();
        plugin.stop();

        Assert.assertEquals(PluginState.STOPPED, plugin.getState());
    }

    @Test(expected = InvalidConfigurationException.class)
    public void shouldRejectUnsupportedEvaluatorType() {
        ScenarioDefinition definition = new ScenarioDefinition();
        definition.setId("invalid-evaluator");
        definition.setPluginId("adaptiflow.dynamic");

        MetricCollectorDefinition collector = new MetricCollectorDefinition();
        collector.setType("ResourceUsageCollector");

        ConditionalEvaluatorDefinition evaluator = new ConditionalEvaluatorDefinition();
        evaluator.setType("UnknownEvaluator");

        ActionDefinition action = new ActionDefinition();
        action.setType("RestAdaptationAction");
        action.setParameters(Map.of(
            "constructorArgTypes", List.of("java.util.List", "java.lang.String", "java.lang.String"),
            "constructorArgs", List.of(List.of("noop"), "http://localhost/adapt", "noop")));

        EventDefinition event = new EventDefinition();
        event.setId("invalid");
        event.setCollector(collector);
        event.setEvaluators(List.of(evaluator));

        SubscriberDefinition subscriber = new SubscriberDefinition();
        subscriber.setType("EventSubscriber");
        subscriber.setActions(List.of(action));
        event.setSubscribers(List.of(subscriber));

        definition.setEvents(List.of(event));

        ScenarioPlugin plugin = new ScenarioPlugin(definition, new ScenarioRuntimeFactoryImpl());
        plugin.initialize(new DefaultPluginContext());
    }

    @Test
    public void shouldInstantiateComponentsFromClassNames() {
        ScenarioDefinition definition = new ScenarioDefinition();
        definition.setId("class-name-scenario");
        definition.setPluginId("adaptiflow.dynamic");
        definition.setIntervalMs(1000);

        MetricCollectorDefinition collector = new MetricCollectorDefinition();
        collector.setType("tools.spirals.cerberus237.metricscollectorbase.metrics.cpu.ResourceUsageCollector");

        ConditionalEvaluatorDefinition evaluator = new ConditionalEvaluatorDefinition();
        evaluator.setType("tools.spirals.cerberus237.adaptiflow.operators.TrueEvaluator");

        ActionDefinition action = new ActionDefinition();
        action.setType("tools.spirals.cerberus237.adaptationactionsbase.core.RestAdaptationAction");
        action.setParameters(Map.of(
                "constructorArgTypes", List.of("java.util.List", "java.lang.String", "java.lang.String"),
                "constructorArgs", List.of(List.of("EnableExternalImageProvider"), "http://localhost/adapt", "EnableExternalImageProvider")));

        EventDefinition event = new EventDefinition();
        event.setId("class-components");
        event.setType("tools.spirals.cerberus237.adaptiflow.events.ConditionalEvent");
        event.setCollector(collector);
        event.setEvaluators(List.of(evaluator));

        SubscriberDefinition subscriber = new SubscriberDefinition();
        subscriber.setType("EventSubscriber");
        subscriber.setActions(List.of(action));
        event.setSubscribers(List.of(subscriber));

        definition.setEvents(List.of(event));

        ScenarioPlugin plugin = new ScenarioPlugin(definition, new ScenarioRuntimeFactoryImpl());
        plugin.initialize(new DefaultPluginContext());
        plugin.start();
        plugin.stop();

        Assert.assertEquals(PluginState.STOPPED, plugin.getState());
    }

    @Test
    public void shouldAllowEmptyActionListWhenSubscribersAreConfigured() {
        ScenarioDefinition definition = new ScenarioDefinition();
        definition.setId("subscriber-only-scenario");
        definition.setPluginId("adaptiflow.dynamic");
        definition.setIntervalMs(1000);

        MetricCollectorDefinition collector = new MetricCollectorDefinition();
        collector.setType("ResourceUsageCollector");

        ConditionalEvaluatorDefinition evaluator = new ConditionalEvaluatorDefinition();
        evaluator.setType("TrueEvaluator");

        SubscriberDefinition subscriber = new SubscriberDefinition();
        subscriber.setType("EventSubscriber");

        EventDefinition event = new EventDefinition();
        event.setId("subscriber-only");
        event.setType("ConditionalEvent");
        event.setCollector(collector);
        event.setEvaluators(List.of(evaluator));
        event.setSubscribers(List.of(subscriber));

        definition.setEvents(List.of(event));

        ScenarioPlugin plugin = new ScenarioPlugin(definition, new ScenarioRuntimeFactoryImpl());
        plugin.initialize(new DefaultPluginContext());
        plugin.start();
        plugin.stop();

        Assert.assertEquals(PluginState.STOPPED, plugin.getState());
    }
}
