package tools.spirals.cerberus237.siphonix;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.spirals.cerberus237.siphonix.plugins.adaptiflow.core.config.AdaptiflowConfiguration;
import tools.spirals.cerberus237.siphonix.plugins.adaptiflow.core.config.YamlConfigurationManager;
import tools.spirals.cerberus237.siphonix.kernel.DefaultPluginContext;
import tools.spirals.cerberus237.siphonix.kernel.PluginRegistry;
import tools.spirals.cerberus237.siphonix.plugins.adaptiflow.runtime.YamlScenarioPlugin;
import tools.spirals.cerberus237.siphonix.plugins.adaptiflow.core.scenarios.ScenarioDefinition;

public class SiphoniX {

    protected static final Logger logger = LoggerFactory.getLogger(SiphoniX.class);

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
        String configPath = System.getenv(CONFIG_PATH_ENV);
        if (configPath == null || configPath.trim().isEmpty()) {
            logger.warn("[SiphoniX] No {} configured. YAML-based plugins are required.", CONFIG_PATH_ENV);
            return;
        }

        if (!registerYamlScenarioPlugins(pluginRegistry, Path.of(configPath.trim()))) {
            logger.warn("[SiphoniX] No enabled scenario plugin registered from {}", configPath);
        }
    }

    private static boolean registerYamlScenarioPlugins(PluginRegistry pluginRegistry, Path configPath) {
        YamlConfigurationManager manager = new YamlConfigurationManager();
        try {
            AdaptiflowConfiguration configuration = manager.load(configPath);
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

}