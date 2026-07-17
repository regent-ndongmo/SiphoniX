package tools.spirals.cerberus237.siphonix;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.spirals.cerberus237.siphonix.api.plugin.Plugin;
import tools.spirals.cerberus237.siphonix.kernel.DefaultPluginContext;
import tools.spirals.cerberus237.siphonix.kernel.PluginRegistry;
import tools.spirals.cerberus237.siphonix.kernel.loading.PluginDiscoveryMode;
import tools.spirals.cerberus237.siphonix.kernel.loading.PluginRuntimeLoader;

public class SiphoniX {

    protected static final Logger logger = LoggerFactory.getLogger(SiphoniX.class);

    private static final String CONFIG_PATH_ENV = "SIPHONIX_CONFIG";
    private static final String PLUGIN_DIRECTORY_ENV = "SIPHONIX_PLUGIN_DIR";
    private static final String PLUGIN_DISCOVERY_MODE_ENV = "SIPHONIX_PLUGIN_DISCOVERY_MODE";
    private static final String PLUGIN_ARTIFACT_ENV = "SIPHONIX_PLUGIN_ARTIFACT";
    private static final String DEFAULT_PLUGIN_DIRECTORY = "/opt/siphonix/plugins";
    private static final String TARGET_SERVICE_URL = System.getenv().getOrDefault("TARGET_URL", "http://adaptable-teastore-image:8080/tools.descartes.teastore.image/rest");

    public static void main(String[] args) {
        LaunchOptions options;
        try {
            options = LaunchOptions.fromArgs(args);
        } catch (IllegalArgumentException ex) {
            logger.error("[SiphoniX] {}", ex.getMessage());
            printUsage();
            return;
        }

        if (options.commandMode) {
            try {
                runPluginCommand(options);
            } catch (RuntimeException ex) {
                logger.error("[SiphoniX] Command failed: {}", ex.getMessage(), ex);
                printUsage();
            }
            return;
        }

        logger.info("[SiphoniX] Starting Autonomic Manager Sidecar...");
        logger.info("[SiphoniX] Monitoring Target: {}", TARGET_SERVICE_URL);
        logger.info("[SiphoniX] Plugin directory: {}", options.pluginDirectory);
        logger.info("[SiphoniX] Plugin discovery mode: {}", options.discoveryMode.getValue());

        PluginRegistry pluginRegistry = new PluginRegistry();
        DefaultPluginContext context = new DefaultPluginContext();
        PluginRuntimeLoader runtimeLoader = new PluginRuntimeLoader(
                pluginRegistry,
                context,
                options.pluginDirectory,
                options.discoveryMode,
                options.configPath);

        CountDownLatch shutdownLatch = new CountDownLatch(1);
        try {
            loadLegacyPluginArtifactIfConfigured(runtimeLoader, options.legacyPluginArtifactPath);
            runtimeLoader.loadStartupPlugins();
            pluginRegistry.initializeAll(context);
            runtimeLoader.onRuntimeInitialized();
            pluginRegistry.startAll();
            runtimeLoader.onRuntimeStarted();
            runtimeLoader.startWatcher();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("[SiphoniX] Shutdown requested, stopping all plugins...");
                runtimeLoader.close();
                pluginRegistry.stopAll();
                shutdownLatch.countDown();
            }, "siphonix-shutdown-hook"));

            logger.info("[SiphoniX] Started {} plugin(s)", pluginRegistry.list().size());
            shutdownLatch.await();
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            logger.warn("[SiphoniX] Main thread interrupted, shutting down...");
            runtimeLoader.close();
            pluginRegistry.stopAll();
        } catch (RuntimeException ex) {
            logger.error("[SiphoniX] Startup failed", ex);
            runtimeLoader.close();
            pluginRegistry.stopAll();
        }
    }

    private static void runPluginCommand(LaunchOptions options) {
        PluginRegistry pluginRegistry = new PluginRegistry();
        DefaultPluginContext context = new DefaultPluginContext();
        PluginRuntimeLoader runtimeLoader = new PluginRuntimeLoader(
                pluginRegistry,
                context,
                options.pluginDirectory,
                options.discoveryMode,
                options.configPath);

        try {
            loadLegacyPluginArtifactIfConfigured(runtimeLoader, options.legacyPluginArtifactPath);
            runtimeLoader.loadStartupPlugins();
            pluginRegistry.initializeAll(context);
            runtimeLoader.onRuntimeInitialized();
            pluginRegistry.startAll();
            runtimeLoader.onRuntimeStarted();
            executePluginCommand(options.commandTokens, pluginRegistry, runtimeLoader);
        } finally {
            runtimeLoader.close();
            pluginRegistry.stopAll();
        }
    }

    private static void executePluginCommand(List<String> commandTokens, PluginRegistry pluginRegistry,
            PluginRuntimeLoader runtimeLoader) {
        if (commandTokens.size() < 2 || !"plugin".equals(commandTokens.get(0))) {
            throw new IllegalArgumentException("Unsupported command. Use: plugin <subcommand>");
        }

        String subcommand = commandTokens.get(1);
        if ("list".equals(subcommand)) {
            List<String> loaded = runtimeLoader.listLoadedPlugins();
            if (loaded.isEmpty()) {
                logger.info("[SiphoniX] No loaded plugins");
            } else {
                logger.info("[SiphoniX] Loaded plugins:");
                for (String line : loaded) {
                    logger.info("[SiphoniX] {}", line);
                }
            }
            List<String> pending = runtimeLoader.listPendingArtifacts();
            if (!pending.isEmpty()) {
                logger.info("[SiphoniX] Pending artifacts:");
                for (String path : pending) {
                    logger.info("[SiphoniX] {}", path);
                }
            }
            return;
        }

        if ("load".equals(subcommand)) {
            requireArgs(commandTokens, 3, "plugin load <jarPath>");
            Plugin loaded = runtimeLoader.loadPlugin(Path.of(commandTokens.get(2)));
            logger.info("[SiphoniX] Loaded plugin {}", loaded.getId());
            return;
        }

        if ("unload".equals(subcommand)) {
            requireArgs(commandTokens, 3, "plugin unload <pluginId>");
            runtimeLoader.unloadPlugin(commandTokens.get(2));
            logger.info("[SiphoniX] Unloaded plugin {}", commandTokens.get(2));
            return;
        }

        if ("start".equals(subcommand)) {
            requireArgs(commandTokens, 3, "plugin start <pluginId>");
            pluginRegistry.start(commandTokens.get(2));
            logger.info("[SiphoniX] Started plugin {}", commandTokens.get(2));
            return;
        }

        if ("stop".equals(subcommand)) {
            requireArgs(commandTokens, 3, "plugin stop <pluginId>");
            pluginRegistry.stop(commandTokens.get(2));
            logger.info("[SiphoniX] Stopped plugin {}", commandTokens.get(2));
            return;
        }

        if ("reload".equals(subcommand)) {
            requireArgs(commandTokens, 3, "plugin reload <pluginId>");
            runtimeLoader.reloadPlugin(commandTokens.get(2));
            logger.info("[SiphoniX] Reloaded plugin {}", commandTokens.get(2));
            return;
        }

        if ("watch".equals(subcommand)) {
            requireArgs(commandTokens, 3, "plugin watch on|off|status");
            String action = commandTokens.get(2);
            if ("status".equals(action)) {
                logger.info("[SiphoniX] Watch status: {}", runtimeLoader.isWatchEnabled() ? "on" : "off");
                return;
            }
            if ("on".equals(action)) {
                runtimeLoader.setWatchEnabled(true);
                runtimeLoader.startWatcher();
                logger.info("[SiphoniX] Watch enabled");
                return;
            }
            if ("off".equals(action)) {
                runtimeLoader.setWatchEnabled(false);
                logger.info("[SiphoniX] Watch disabled");
                return;
            }
            throw new IllegalArgumentException("Unknown watch action: " + action);
        }

        throw new IllegalArgumentException("Unsupported plugin command: " + subcommand);
    }

    private static void printUsage() {
        logger.info("[SiphoniX] Usage:");
        logger.info("[SiphoniX]   Daemon mode:");
        logger.info("[SiphoniX]     java -jar siphonix.jar [--plugin-dir <path>] [--plugin-discovery-mode <startup-only|watch-auto|watch-manual>]");
        logger.info("[SiphoniX]   Command mode:");
        logger.info("[SiphoniX]     java -jar siphonix.jar [--plugin-dir <path>] [--plugin-discovery-mode <mode>] plugin list");
        logger.info("[SiphoniX]     java -jar siphonix.jar [--plugin-dir <path>] [--plugin-discovery-mode <mode>] plugin load <jarPath>");
        logger.info("[SiphoniX]     java -jar siphonix.jar [--plugin-dir <path>] [--plugin-discovery-mode <mode>] plugin unload <pluginId>");
        logger.info("[SiphoniX]     java -jar siphonix.jar [--plugin-dir <path>] [--plugin-discovery-mode <mode>] plugin start <pluginId>");
        logger.info("[SiphoniX]     java -jar siphonix.jar [--plugin-dir <path>] [--plugin-discovery-mode <mode>] plugin stop <pluginId>");
        logger.info("[SiphoniX]     java -jar siphonix.jar [--plugin-dir <path>] [--plugin-discovery-mode <mode>] plugin reload <pluginId>");
        logger.info("[SiphoniX]     java -jar siphonix.jar [--plugin-dir <path>] [--plugin-discovery-mode <mode>] plugin watch on|off|status");
        logger.info("[SiphoniX] Environment defaults: SIPHONIX_PLUGIN_DIR, SIPHONIX_PLUGIN_DISCOVERY_MODE, SIPHONIX_CONFIG, SIPHONIX_PLUGIN_ARTIFACT");
    }

    private static void requireArgs(List<String> commandTokens, int requiredCount, String usage) {
        if (commandTokens.size() < requiredCount) {
            throw new IllegalArgumentException("Invalid command. Usage: " + usage);
        }
    }

    private static void loadLegacyPluginArtifactIfConfigured(PluginRuntimeLoader runtimeLoader, String artifactPath) {
        if (artifactPath == null || artifactPath.trim().isEmpty()) {
            return;
        }
        Path path = Path.of(artifactPath.trim());
        logger.info("[SiphoniX] Loading legacy plugin artifact from {}", path);
        runtimeLoader.loadPlugin(path);
    }

    private static final class LaunchOptions {
        private final Path pluginDirectory;
        private final PluginDiscoveryMode discoveryMode;
        private final String configPath;
        private final String legacyPluginArtifactPath;
        private final boolean commandMode;
        private final List<String> commandTokens;

        private LaunchOptions(Path pluginDirectory, PluginDiscoveryMode discoveryMode, String configPath,
                String legacyPluginArtifactPath, boolean commandMode, List<String> commandTokens) {
            this.pluginDirectory = pluginDirectory;
            this.discoveryMode = discoveryMode;
            this.configPath = configPath;
            this.legacyPluginArtifactPath = legacyPluginArtifactPath;
            this.commandMode = commandMode;
            this.commandTokens = commandTokens;
        }

        private static LaunchOptions fromArgs(String[] args) {
            String pluginDirFromArgs = null;
            String discoveryModeFromArgs = null;
            List<String> commandTokens = new ArrayList<>();

            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg.startsWith("--plugin-dir=")) {
                    pluginDirFromArgs = arg.substring("--plugin-dir=".length());
                    continue;
                }
                if ("--plugin-dir".equals(arg)) {
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("Missing value for --plugin-dir");
                    }
                    pluginDirFromArgs = args[++i];
                    continue;
                }
                if (arg.startsWith("--plugin-discovery-mode=")) {
                    discoveryModeFromArgs = arg.substring("--plugin-discovery-mode=".length());
                    continue;
                }
                if ("--plugin-discovery-mode".equals(arg)) {
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("Missing value for --plugin-discovery-mode");
                    }
                    discoveryModeFromArgs = args[++i];
                    continue;
                }
                commandTokens.add(arg);
            }

            String configuredPluginDir = firstNonBlank(
                    pluginDirFromArgs,
                    System.getenv(PLUGIN_DIRECTORY_ENV),
                    DEFAULT_PLUGIN_DIRECTORY);

            String configuredMode = firstNonBlank(
                    discoveryModeFromArgs,
                    System.getenv(PLUGIN_DISCOVERY_MODE_ENV),
                    PluginDiscoveryMode.STARTUP_ONLY.getValue());

            String configPath = System.getenv(CONFIG_PATH_ENV);
                String legacyPluginArtifactPath = System.getenv(PLUGIN_ARTIFACT_ENV);
            return new LaunchOptions(
                    Path.of(configuredPluginDir),
                    PluginDiscoveryMode.fromValue(configuredMode),
                    configPath,
                    legacyPluginArtifactPath,
                    !commandTokens.isEmpty(),
                    commandTokens);
        }

        private static String firstNonBlank(String first, String second, String fallback) {
            if (first != null && !first.trim().isEmpty()) {
                return first.trim();
            }
            if (second != null && !second.trim().isEmpty()) {
                return second.trim();
            }
            return fallback;
        }
    }

}