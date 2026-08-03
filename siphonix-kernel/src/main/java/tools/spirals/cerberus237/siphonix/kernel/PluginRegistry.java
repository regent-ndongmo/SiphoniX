package tools.spirals.cerberus237.siphonix.kernel;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import tools.spirals.cerberus237.siphonix.api.plugin.Plugin;
import tools.spirals.cerberus237.siphonix.api.plugin.PluginContext;
import tools.spirals.cerberus237.siphonix.api.plugin.PluginRegistryView;
import tools.spirals.cerberus237.siphonix.api.plugin.PluginState;

/**
 * In-memory registry for plugin lifecycle management.
 *
 * <p>This registry provides synchronized operations for registration, replacement,
 * lifecycle forwarding, and typed listing. It is intentionally lightweight and
 * does not persist plugin state across process restarts.
 *
 * @author Arléon Zemtsop (Cerberus)
 */
public class PluginRegistry implements PluginRegistryView {

    private final Map<String, Plugin> plugins = new LinkedHashMap<>();

    /**
     * Registers a plugin in CREATED state.
     *
     * @param plugin plugin to register.
     */
    public void register(Plugin plugin) {
        if (plugins.containsKey(plugin.getId())) {
            throw new IllegalArgumentException("A plugin with id '" + plugin.getId() + "' is already registered");
        }
        plugins.put(plugin.getId(), plugin);
    }

    /**
     * Registers and initializes a plugin in one operation.
     *
     * @param plugin plugin to register.
     * @param context initialization context.
     */
    public void registerAndInitialize(Plugin plugin, PluginContext context) {
        register(plugin);
        plugin.initialize(context);
    }

    /**
     * Retrieves one plugin by id.
     *
     * @param pluginId plugin identifier.
     * @return registered plugin.
     */
    public Plugin get(String pluginId) {
        Plugin plugin = plugins.get(pluginId);
        if (plugin == null) {
            throw new IllegalArgumentException("No plugin found with id '" + pluginId + "'");
        }
        return plugin;
    }

    /**
     * Starts a plugin by id.
     *
     * @param pluginId plugin identifier.
     */
    public void start(String pluginId) {
        get(pluginId).start();
    }

    /**
     * Stops a plugin by id.
     *
     * @param pluginId plugin identifier.
     */
    public void stop(String pluginId) {
        get(pluginId).stop();
    }

    /**
     * Removes one plugin, stopping it first when currently running.
     *
     * @param pluginId plugin identifier.
     * @return removed plugin.
     */
    public Plugin remove(String pluginId) {
        Plugin plugin = get(pluginId);
        if (plugin.getState() == PluginState.RUNNING) {
            plugin.stop();
        }
        plugins.remove(pluginId);
        return plugin;
    }

    /**
     * Replaces one registered plugin by id.
     *
     * @param pluginId target plugin id.
     * @param replacement replacement plugin with same id.
     * @param context initialization context.
     * @return previous plugin instance.
     */
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

    /**
     * @return all registered plugins.
     */
    public Collection<Plugin> list() {
        return Collections.unmodifiableCollection(plugins.values());
    }

    /**
     * Returns registered plugins assignable to the requested type.
     *
     * @param pluginType type token.
     * @param <T> plugin type.
     * @return immutable collection of matching plugins.
     */
    public <T extends Plugin> Collection<T> listByType(Class<T> pluginType) {
        Set<T> typed = new LinkedHashSet<>();
        for (Plugin plugin : plugins.values()) {
            if (pluginType.isInstance(plugin)) {
                typed.add(pluginType.cast(plugin));
            }
        }
        return Collections.unmodifiableSet(typed);
    }

    /**
     * Initializes all registered plugins.
     *
     * @param context plugin initialization context.
     */
    public void initializeAll(PluginContext context) {
        for (Plugin plugin : plugins.values()) {
            plugin.initialize(context);
        }
    }

    /**
     * Starts all registered plugins.
     */
    public void startAll() {
        for (Plugin plugin : plugins.values()) {
            plugin.start();
        }
    }

    /**
     * Stops all registered plugins.
     */
    public void stopAll() {
        for (Plugin plugin : plugins.values()) {
            plugin.stop();
        }
    }
}
