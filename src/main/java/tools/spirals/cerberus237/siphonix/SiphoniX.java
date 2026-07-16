package tools.spirals.cerberus237.siphonix;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.spirals.cerberus237.siphonix.config.SiphonixConfiguration;
import tools.spirals.cerberus237.siphonix.config.YamlConfigurationManager;
import tools.spirals.cerberus237.siphonix.kernel.DefaultPluginContext;
import tools.spirals.cerberus237.siphonix.kernel.Plugin;
import tools.spirals.cerberus237.siphonix.kernel.PluginRegistry;
import tools.spirals.cerberus237.siphonix.plugins.adaptiflow.BeninTrafficObservationPlugin;
import tools.spirals.cerberus237.siphonix.plugins.adaptiflow.CacheSizeObservationPlugin;
import tools.spirals.cerberus237.siphonix.plugins.adaptiflow.DatabaseAvailabilityObservationPlugin;
import tools.spirals.cerberus237.siphonix.plugins.adaptiflow.YamlScenarioPlugin;
import tools.spirals.cerberus237.siphonix.scenarios.ScenarioDefinition;

public class SiphoniX {

    protected static final Logger logger = LoggerFactory.getLogger(SiphoniX.class);

    private static final String DEFAULT_ENABLED_PLUGINS = "adaptiflow.cache-size";
    private static final String CONFIG_PATH_ENV = "SIPHONIX_CONFIG";
    private static final String TARGET_SERVICE_URL = System.getenv().getOrDefault("TARGET_URL", "http://adaptable-teastore-image:8080/tools.descartes.teastore.image/rest");
    
    public static void main(String[] args) {
        logger.info("[SiphoniX] Starting Autonomic Manager Sidecar...");
        logger.info("[SiphoniX] Monitoring Target: {}", TARGET_SERVICE_URL);

        PluginRegistry pluginRegistry = new PluginRegistry();
        registerPlugins(pluginRegistry);
        pluginRegistry.initializeAll(new DefaultPluginContext());
        pluginRegistry.startAll();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("[SiphoniX] Shutdown requested, stopping all plugins...");
            pluginRegistry.stopAll();
        }, "siphonix-shutdown-hook"));

        logger.info("[SiphoniX] Started {} plugin(s)", pluginRegistry.list().size());
    }

    private static void registerPlugins(PluginRegistry pluginRegistry) {
        String rawEnabledPlugins = System.getenv("SIPHONIX_ENABLED_PLUGINS");
        if (rawEnabledPlugins != null && !rawEnabledPlugins.trim().isEmpty()) {
            registerEnabledPlugins(pluginRegistry, parsePluginIds(rawEnabledPlugins));
            return;
        }

        String configPath = System.getenv(CONFIG_PATH_ENV);
        if (configPath != null && !configPath.trim().isEmpty()) {
            if (!registerYamlScenarioPlugins(pluginRegistry, Path.of(configPath.trim()))) {
                logger.warn("[SiphoniX] No enabled scenario plugin registered from {}", configPath);
            }
            return;
        }

        registerEnabledPlugins(pluginRegistry, parsePluginIds(DEFAULT_ENABLED_PLUGINS));
    }

    private static boolean registerYamlScenarioPlugins(PluginRegistry pluginRegistry, Path configPath) {
        YamlConfigurationManager manager = new YamlConfigurationManager();
        try {
            SiphonixConfiguration configuration = manager.load(configPath);
            int registeredCount = 0;

            for (Map.Entry<String, ScenarioDefinition> scenarioEntry : configuration.getScenarios().entrySet()) {
                ScenarioDefinition scenario = scenarioEntry.getValue();
                if (!scenario.isEnabled()) {
                    continue;
                }
                pluginRegistry.register(new YamlScenarioPlugin(scenario));
                registeredCount++;
                logger.info("[SiphoniX] Registered YAML scenario plugin for scenario {}", scenario.getId());
            }

            return registeredCount > 0;
        } catch (IOException | RuntimeException ex) {
            logger.error("[SiphoniX] Failed to register YAML scenario plugins from {}", configPath, ex);
            return false;
        }
    }

    private static Set<String> parsePluginIds(String rawEnabledPlugins) {
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