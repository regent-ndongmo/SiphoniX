package tools.spirals.cerberus237.adaptiflow.plugin.runtime;

import java.util.List;

import tools.spirals.cerberus237.adaptiflow.events.Event;
import tools.spirals.cerberus237.adaptiflow.plugin.core.scenarios.EventDefinition;
import tools.spirals.cerberus237.adaptiflow.plugin.core.scenarios.ScenarioDefinition;
import tools.spirals.cerberus237.siphonix.api.plugin.PluginContext;

/**
 * Factory contract that materializes executable AdaptiFlow runtime components from scenario
 * definitions.
 *
 * @author Arléon Zemtsop (Cerberus)
 */
public interface ScenarioRuntimeFactory {

    /**
     * Builds one executable event from its declarative definition.
     *
     * @param scenario owning scenario definition
     * @param eventDefinition event definition to materialize
     * @param context plugin context
     * @return materialized event ready for scheduler subscription
     */
    Event buildEvent(ScenarioDefinition scenario, EventDefinition eventDefinition, PluginContext context);

    /**
     * Creates a scheduler instance for scenario event observation.
     *
     * @param scenario owning scenario definition
     * @param events materialized events to schedule
     * @return scheduler instance compatible with {@code ManagedSchedulerHandle}
     */
    Object createScheduler(ScenarioDefinition scenario, List<Event> events);
}