package tools.spirals.cerberus237.siphonix;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.spirals.cerberus237.siphonix.kernel.DefaultPluginContext;
import tools.spirals.cerberus237.siphonix.kernel.Plugin;
import tools.spirals.cerberus237.siphonix.kernel.PluginRegistry;
import tools.spirals.cerberus237.siphonix.plugins.adaptiflow.BeninTrafficObservationPlugin;
import tools.spirals.cerberus237.siphonix.plugins.adaptiflow.CacheSizeObservationPlugin;
import tools.spirals.cerberus237.siphonix.plugins.adaptiflow.DatabaseAvailabilityObservationPlugin;

public class SiphoniX {

    protected static final Logger logger = LoggerFactory.getLogger(SiphoniX.class);

    private static final String DEFAULT_ENABLED_PLUGINS = "adaptiflow.cache-size";
    private static final String TARGET_SERVICE_URL = System.getenv().getOrDefault("TARGET_URL", "http://adaptable-teastore-image:8080/tools.descartes.teastore.image/rest");
    
    public static void main(String[] args) {
        logger.info("[SiphoniX] Starting Autonomic Manager Sidecar...");
        logger.info("[SiphoniX] Monitoring Target: {}", TARGET_SERVICE_URL);

        PluginRegistry pluginRegistry = new PluginRegistry();
        registerEnabledPlugins(pluginRegistry, getEnabledPluginIds());
        pluginRegistry.initializeAll(new DefaultPluginContext());
        pluginRegistry.startAll();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("[SiphoniX] Shutdown requested, stopping all plugins...");
            pluginRegistry.stopAll();
        }, "siphonix-shutdown-hook"));

        logger.info("[SiphoniX] Started {} plugin(s)", pluginRegistry.list().size());
    }

    private static Set<String> getEnabledPluginIds() {
        String rawEnabledPlugins = System.getenv().getOrDefault("SIPHONIX_ENABLED_PLUGINS", DEFAULT_ENABLED_PLUGINS);
        Set<String> pluginIds = new LinkedHashSet<>();
        Arrays.stream(rawEnabledPlugins.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .forEach(pluginIds::add);
        return pluginIds;
    }

    private static void registerEnabledPlugins(PluginRegistry pluginRegistry, Set<String> enabledPluginIds) {
        registerIfEnabled(pluginRegistry, enabledPluginIds, new CacheSizeObservationPlugin());
        registerIfEnabled(pluginRegistry, enabledPluginIds, new BeninTrafficObservationPlugin());
        registerIfEnabled(pluginRegistry, enabledPluginIds, new DatabaseAvailabilityObservationPlugin());
    }

    private static void registerIfEnabled(PluginRegistry pluginRegistry, Set<String> enabledPluginIds, Plugin plugin) {
        if (enabledPluginIds.contains(plugin.getId())) {
            pluginRegistry.register(plugin);
            logger.info("[SiphoniX] Registered plugin {}", plugin.getId());
            return;
        }
        logger.info("[SiphoniX] Plugin {} is available but not enabled", plugin.getId());
    }
}