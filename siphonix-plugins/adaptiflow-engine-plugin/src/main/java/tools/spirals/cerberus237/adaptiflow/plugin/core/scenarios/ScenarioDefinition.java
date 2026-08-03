package tools.spirals.cerberus237.adaptiflow.plugin.core.scenarios;
/**
 * 
 * @author Arléon Zemtsop (Cerberus)
 */

import java.util.ArrayList;
import java.util.List;

/**
 * Declarative scenario definition loaded from configuration.
 */
public class ScenarioDefinition {

    private String id;
    private String pluginId;
    private boolean enabled = true;
    private int intervalMs = 5000;
    private List<EventDefinition> events = new ArrayList<>();
    private ObservationSchedulerDefinition scheduler;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPluginId() {
        return pluginId;
    }

    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getIntervalMs() {
        return intervalMs;
    }

    public void setIntervalMs(int intervalMs) {
        this.intervalMs = intervalMs;
    }

    public List<EventDefinition> getEvents() {
        return events;
    }

    public void setEvents(List<EventDefinition> events) {
        this.events = events;
    }

    public ObservationSchedulerDefinition getScheduler() {
        return scheduler;
    }

    public void setScheduler(ObservationSchedulerDefinition scheduler) {
        this.scheduler = scheduler;
    }
}
