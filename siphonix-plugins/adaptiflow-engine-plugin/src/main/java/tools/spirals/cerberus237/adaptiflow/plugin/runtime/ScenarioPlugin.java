package tools.spirals.cerberus237.adaptiflow.plugin.runtime;
/**
 * 
 * @author Arléon Zemtsop (Cerberus)
 */

import java.util.ArrayList;
import java.util.List;

import tools.spirals.cerberus237.adaptiflow.events.Event;
import tools.spirals.cerberus237.adaptiflow.plugin.core.scenarios.EventDefinition;
import tools.spirals.cerberus237.adaptiflow.plugin.core.scenarios.ScenarioDefinition;
import tools.spirals.cerberus237.siphonix.api.plugin.Plugin;
import tools.spirals.cerberus237.siphonix.api.plugin.PluginContext;
import tools.spirals.cerberus237.siphonix.api.plugin.ManagedSchedulerHandle;
import tools.spirals.cerberus237.siphonix.api.plugin.PluginState;

public class ScenarioPlugin implements Plugin {

    private final ScenarioDefinition scenario;
    private final ScenarioRuntimeFactory runtimeFactory;

    private PluginState state = PluginState.CREATED;
    private ManagedSchedulerHandle schedulerHandle;

    public ScenarioPlugin(ScenarioDefinition scenario, ScenarioRuntimeFactory runtimeFactory) {
        this.scenario = scenario;
        this.runtimeFactory = runtimeFactory;
    }

    @Override
    public String getId() {
        return "scenario." + scenario.getId();
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public PluginState getState() {
        return state;
    }

    @Override
    public void initialize(PluginContext context) {
        List<Event> events = new ArrayList<>();
        for (EventDefinition eventDefinition : scenario.getEvents()) {
            events.add(runtimeFactory.buildEvent(scenario, eventDefinition, context));
        }

        Object scheduler = runtimeFactory.createScheduler(scenario, events);
        schedulerHandle = new ManagedSchedulerHandle(scheduler);
        state = PluginState.INITIALIZED;
    }

    @Override
    public void start() {
        if (schedulerHandle == null) {
            throw new IllegalStateException("Plugin " + getId() + " is not initialized");
        }
        schedulerHandle.start();
        state = PluginState.RUNNING;
    }

    @Override
    public void stop() {
        if (schedulerHandle == null) {
            return;
        }
        schedulerHandle.stop();
        state = PluginState.STOPPED;
    }
}