package tools.spirals.cerberus237.adaptiflow.subscriptions;

import java.util.List;

import tools.spirals.cerberus237.adaptiflow.events.Event;

/**
 * Test-only scheduler used to verify pluginId-aware constructor resolution.
 */
public class TestExtensibleSchedulerWithId {

    private final List<Event> events;
    private final int intervalMs;
    private final String pluginId;

    public TestExtensibleSchedulerWithId(List<Event> events, int intervalMs, String pluginId) {
        this.events = events;
        this.intervalMs = intervalMs;
        this.pluginId = pluginId;
    }

    public List<Event> getEvents() {
        return events;
    }

    public int getIntervalMs() {
        return intervalMs;
    }

    public String getPluginId() {
        return pluginId;
    }
}
