package tools.spirals.cerberus237.siphonix.kernel;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

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
