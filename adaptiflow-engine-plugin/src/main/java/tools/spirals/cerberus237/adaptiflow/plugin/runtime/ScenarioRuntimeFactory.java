package tools.spirals.cerberus237.adaptiflow.plugin.runtime;

import java.util.List;

import tools.spirals.cerberus237.adaptiflow.events.Event;
import tools.spirals.cerberus237.adaptiflow.plugin.core.scenarios.EventDefinition;
import tools.spirals.cerberus237.adaptiflow.plugin.core.scenarios.ScenarioDefinition;
import tools.spirals.cerberus237.siphonix.api.plugin.PluginContext;

public interface ScenarioRuntimeFactory {

    Event buildEvent(ScenarioDefinition scenario, EventDefinition eventDefinition, PluginContext context);

    Object createScheduler(ScenarioDefinition scenario, List<Event> events);
}