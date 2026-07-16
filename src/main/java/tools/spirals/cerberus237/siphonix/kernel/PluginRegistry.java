package tools.spirals.cerberus237.siphonix.kernel;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import tools.spirals.cerberus237.siphonix.api.plugin.Plugin;
import tools.spirals.cerberus237.siphonix.api.plugin.PluginContext;
import tools.spirals.cerberus237.siphonix.api.plugin.PluginState;

/**
 * In-memory registry for plugin lifecycle management.
 */
public class PluginRegistry {

    private final Map<String, Plugin> plugins = new LinkedHashMap<>();

    public void register(Plugin plugin) {
        if (plugins.containsKey(plugin.getId())) {
            throw new IllegalArgumentException("A plugin with id '" + plugin.getId() + "' is already registered");
        }
        plugins.put(plugin.getId(), plugin);
    }

    public void registerAndInitialize(Plugin plugin, PluginContext context) {
        register(plugin);
        plugin.initialize(context);
    }

    public Plugin get(String pluginId) {
        Plugin plugin = plugins.get(pluginId);
        if (plugin == null) {
            throw new IllegalArgumentException("No plugin found with id '" + pluginId + "'");
        }
        return plugin;
    }

    public void start(String pluginId) {
        get(pluginId).start();
    }

    public void stop(String pluginId) {
        get(pluginId).stop();
    }

    public Plugin remove(String pluginId) {
        Plugin plugin = get(pluginId);
        if (plugin.getState() == PluginState.RUNNING) {
            plugin.stop();
        }
        plugins.remove(pluginId);
        return plugin;
    }

    public Plugin replace(String pluginId, Plugin replacement, PluginContext context) {
        if (!pluginId.equals(replacement.getId())) {
            throw new IllegalArgumentException(
                    "Replacement plugin id mismatch: expected '" + pluginId + "' but got '" + replacement.getId() + "'");
        }

        Plugin previous = get(pluginId);
        boolean wasRunning = previous.getState() == PluginState.RUNNING;
        if (wasRunning) {
            previous.stop();
        }

        plugins.remove(pluginId);
        registerAndInitialize(replacement, context);
        if (wasRunning) {
            replacement.start();
        }

        return previous;
    }

    public Collection<Plugin> list() {
        return Collections.unmodifiableCollection(plugins.values());
    }

    public void initializeAll(PluginContext context) {
        for (Plugin plugin : plugins.values()) {
            plugin.initialize(context);
        }
    }

    public void startAll() {
        for (Plugin plugin : plugins.values()) {
            plugin.start();
        }
    }

    public void stopAll() {
        for (Plugin plugin : plugins.values()) {
            plugin.stop();
        }
    }
}
