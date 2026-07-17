package tools.spirals.cerberus237.siphonix;

import java.io.IOException;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.spirals.cerberus237.siphonix.api.plugin.management.ScenarioSource;
import tools.spirals.cerberus237.siphonix.api.plugin.management.ScenarioManagementPlugin;
import tools.spirals.cerberus237.siphonix.kernel.DefaultPluginContext;
import tools.spirals.cerberus237.siphonix.kernel.PluginRegistry;
import tools.spirals.cerberus237.siphonix.kernel.loading.JsonFileScenarioSource;
import tools.spirals.cerberus237.siphonix.kernel.loading.PluginArtifactLoader;
import tools.spirals.cerberus237.siphonix.kernel.loading.XmlFileScenarioSource;
import tools.spirals.cerberus237.siphonix.kernel.loading.YamlFileScenarioSource;

public class SiphoniX {

    protected static final Logger logger = LoggerFactory.getLogger(SiphoniX.class);

    private static final String CONFIG_PATH_ENV = "SIPHONIX_CONFIG";
    private static final String PLUGIN_ARTIFACT_ENV = "SIPHONIX_PLUGIN_ARTIFACT";
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
        String pluginArtifactPath = System.getenv(PLUGIN_ARTIFACT_ENV);
        if (pluginArtifactPath == null || pluginArtifactPath.trim().isEmpty()) {
            logger.warn("[SiphoniX] No {} configured. External plugin artifact is required.", PLUGIN_ARTIFACT_ENV);
            return;
        }

        if (!registerExternalScenarioManagementPlugin(pluginRegistry, Path.of(pluginArtifactPath.trim()))) {
            logger.warn("[SiphoniX] No plugin registered from artifact {}", pluginArtifactPath);
        }
    }

    private static boolean registerExternalScenarioManagementPlugin(PluginRegistry pluginRegistry, Path artifactPath) {
        try {
            PluginArtifactLoader loader = new PluginArtifactLoader();
            PluginArtifactLoader.LoadedPluginHandle handle = loader.loadScenarioManagementPlugin(artifactPath);
            ScenarioManagementPlugin plugin = handle.getPlugin();

            pluginRegistry.register(plugin);
            logger.info("[SiphoniX] Registered external scenario management plugin {} from {}",
                    plugin.getId(), handle.getArtifactPath());

            String configPath = System.getenv(CONFIG_PATH_ENV);
            if (configPath != null && !configPath.trim().isEmpty()) {
                Path scenarioPath = Path.of(configPath.trim());
                ScenarioSource scenarioSource = sourceFromPath(scenarioPath);
                plugin.getScenarioManagementService().createScenario(
                        scenarioSource);
                logger.info("[SiphoniX] Applied scenario source {} through external plugin", configPath);
            } else {
                logger.warn("[SiphoniX] No {} configured. Plugin loaded without initial scenarios", CONFIG_PATH_ENV);
            }

            return true;
        } catch (IOException | RuntimeException ex) {
            logger.error("[SiphoniX] Failed to load external scenario plugin from {}", artifactPath, ex);
            return false;
        }
    }

    private static ScenarioSource sourceFromPath(Path path) {
        String fileName = path.getFileName() == null ? "" : path.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".json")) {
            return new JsonFileScenarioSource(path);
        }
        if (fileName.endsWith(".xml")) {
            return new XmlFileScenarioSource(path);
        }
        return new YamlFileScenarioSource(path);
    }

}