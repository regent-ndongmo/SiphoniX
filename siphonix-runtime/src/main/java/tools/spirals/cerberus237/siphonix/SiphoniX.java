package tools.spirals.cerberus237.siphonix;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.spirals.cerberus237.siphonix.api.plugin.Plugin;
import tools.spirals.cerberus237.siphonix.kernel.DefaultPluginContext;
import tools.spirals.cerberus237.siphonix.kernel.PluginRegistry;
import tools.spirals.cerberus237.siphonix.kernel.loading.PluginDiscoveryMode;
import tools.spirals.cerberus237.siphonix.kernel.loading.PluginRuntimeLoader;

/**
 * SiphoniX bootstrap entrypoint.
 *
 * <p>This class supports two execution modes:
 * <ul>
 * <li>Daemon mode: starts SiphoniX as a long-running sidecar that loads plugins from a
 * configured folder and optionally watches that folder for runtime changes.</li>
 * <li>Command mode: executes plugin management commands (list/load/unload/start/stop/reload/watch)
 * against a transient runtime context.</li>
 * </ul>
 *
 * <p>Configuration precedence for plugin folder and discovery mode is:
 * CLI arguments first, then environment variables, then defaults.
 *
 * @author Arléon Zemtsop (Cerberus)
 */
public class SiphoniX {

    protected static final Logger logger = LoggerFactory.getLogger(SiphoniX.class);

    private static final String CONFIG_PATH_ENV = "SIPHONIX_CONFIG";
    private static final String PLUGIN_DIRECTORY_ENV = "SIPHONIX_PLUGIN_DIR";
    private static final String PLUGIN_DISCOVERY_MODE_ENV = "SIPHONIX_PLUGIN_DISCOVERY_MODE";
    private static final String PLUGIN_ARTIFACT_ENV = "SIPHONIX_PLUGIN_ARTIFACT";
    private static final String WAIT_FOR_TARGET_ENV = "SIPHONIX_WAIT_FOR_TARGET";
    private static final String DEFAULT_PLUGIN_DIRECTORY = "/opt/siphonix/plugins";
    private static final String DEFAULT_TARGET_SERVICE_URL =
            "http://localhost:8080/tools.descartes.teastore.image/rest";
    private static final String TARGET_SERVICE_URL =
            System.getenv().getOrDefault("TARGET_URL", DEFAULT_TARGET_SERVICE_URL);
    private static final String READINESS_URL = System.getenv().getOrDefault(
            "SIPHONIX_READINESS_URL", TARGET_SERVICE_URL + "/image/finished");

    /**
     * Main startup entrypoint.
     *
     * @param args runtime options and optional command tokens.
     */
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
        logger.info("[SiphoniX] Readiness endpoint: {}", READINESS_URL);
        logger.info("[SiphoniX] Plugin directory: {}", options.pluginDirectory);
        logger.info("[SiphoniX] Plugin discovery mode: {}", options.discoveryMode.getValue());
        logger.info("[SiphoniX] Wait for target service: {}", options.waitForTarget);

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
            awaitTargetServiceIfEnabled(options);
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

    /**
     * Runs one plugin management command and exits.
     *
     * @param options fully resolved launch options.
     */
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
            awaitTargetServiceIfEnabled(options);
            pluginRegistry.startAll();
            runtimeLoader.onRuntimeStarted();
            executePluginCommand(options.commandTokens, pluginRegistry, runtimeLoader);
        } finally {
            runtimeLoader.close();
            pluginRegistry.stopAll();
        }
    }

    /**
     * Executes plugin command tokens.
     *
     * @param commandTokens parsed command tokens.
     * @param pluginRegistry plugin registry used by command operations.
     * @param runtimeLoader runtime loader used by command operations.
     */
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

    private static void awaitTargetService() {
        try {
            new RemoteServiceReadinessChecker().awaitReady(READINESS_URL);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Interrupted while waiting for the monitored service", ex);
        }
    }

    private static void awaitTargetServiceIfEnabled(LaunchOptions options) {
        if (!options.waitForTarget) {
            logger.info("[Readiness] Target service wait disabled by configuration");
            return;
        }
        awaitTargetService();
    }

    /**
     * Prints command and daemon usage instructions.
     */
    private static void printUsage() {
        logger.info("[SiphoniX] Usage:");
        logger.info("[SiphoniX]   Daemon mode:");
        logger.info("[SiphoniX]     java -jar siphonix.jar [--plugin-dir <path>] [--plugin-discovery-mode <startup-only|watch-auto|watch-manual>] [--wait-for-target <true|false>]");
        logger.info("[SiphoniX]   Command mode:");
        logger.info("[SiphoniX]     java -jar siphonix.jar [--plugin-dir <path>] [--plugin-discovery-mode <mode>] plugin list");
        logger.info("[SiphoniX]     java -jar siphonix.jar [--plugin-dir <path>] [--plugin-discovery-mode <mode>] plugin load <jarPath>");
        logger.info("[SiphoniX]     java -jar siphonix.jar [--plugin-dir <path>] [--plugin-discovery-mode <mode>] plugin unload <pluginId>");
        logger.info("[SiphoniX]     java -jar siphonix.jar [--plugin-dir <path>] [--plugin-discovery-mode <mode>] plugin start <pluginId>");
        logger.info("[SiphoniX]     java -jar siphonix.jar [--plugin-dir <path>] [--plugin-discovery-mode <mode>] plugin stop <pluginId>");
        logger.info("[SiphoniX]     java -jar siphonix.jar [--plugin-dir <path>] [--plugin-discovery-mode <mode>] plugin reload <pluginId>");
        logger.info("[SiphoniX]     java -jar siphonix.jar [--plugin-dir <path>] [--plugin-discovery-mode <mode>] plugin watch on|off|status");
        logger.info("[SiphoniX] Optional in both modes: --wait-for-target <true|false> (default: true)");
        logger.info("[SiphoniX] Environment defaults: SIPHONIX_PLUGIN_DIR, SIPHONIX_PLUGIN_DISCOVERY_MODE, SIPHONIX_CONFIG, SIPHONIX_PLUGIN_ARTIFACT, SIPHONIX_WAIT_FOR_TARGET");
        logger.info("[SiphoniX] Readiness endpoint: SIPHONIX_READINESS_URL (defaults to TARGET_URL + /image/finished)");
        logger.info("[SiphoniX] Target readiness wait is enabled by default");
    }

    /**
     * Validates command arity.
     *
     * @param commandTokens current command tokens.
     * @param requiredCount required number of tokens.
     * @param usage usage string shown in error message.
     */
    private static void requireArgs(List<String> commandTokens, int requiredCount, String usage) {
        if (commandTokens.size() < requiredCount) {
            throw new IllegalArgumentException("Invalid command. Usage: " + usage);
        }
    }

    /**
     * Loads a legacy single plugin artifact when the compatibility variable is set.
     *
     * @param runtimeLoader active runtime loader.
     * @param artifactPath artifact path from compatibility environment variable.
     */
    private static void loadLegacyPluginArtifactIfConfigured(PluginRuntimeLoader runtimeLoader, String artifactPath) {
        if (artifactPath == null || artifactPath.trim().isEmpty()) {
            return;
        }
        Path path = Path.of(artifactPath.trim());
        logger.info("[SiphoniX] Loading legacy plugin artifact from {}", path);
        runtimeLoader.loadPlugin(path);
    }

    /**
     * Immutable launch options resolved from CLI arguments and environment variables.
     *
     * @author Arléon Zemtsop (Cerberus)
     */
    static final class LaunchOptions {
        private final Path pluginDirectory;
        private final PluginDiscoveryMode discoveryMode;
        private final String configPath;
        private final String legacyPluginArtifactPath;
        private final boolean waitForTarget;
        private final boolean commandMode;
        private final List<String> commandTokens;

        private LaunchOptions(Path pluginDirectory, PluginDiscoveryMode discoveryMode, String configPath,
                String legacyPluginArtifactPath, boolean waitForTarget, boolean commandMode,
                List<String> commandTokens) {
            this.pluginDirectory = pluginDirectory;
            this.discoveryMode = discoveryMode;
            this.configPath = configPath;
            this.legacyPluginArtifactPath = legacyPluginArtifactPath;
            this.waitForTarget = waitForTarget;
            this.commandMode = commandMode;
            this.commandTokens = commandTokens;
        }

        static LaunchOptions fromArgs(String[] args) {
            return fromArgs(args, System.getenv());
        }

        static LaunchOptions fromArgs(String[] args, Map<String, String> environment) {
            String pluginDirFromArgs = null;
            String discoveryModeFromArgs = null;
            String waitForTargetFromArgs = null;
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
                if (arg.startsWith("--wait-for-target=")) {
                    waitForTargetFromArgs = arg.substring("--wait-for-target=".length());
                    continue;
                }
                if ("--wait-for-target".equals(arg)) {
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("Missing value for --wait-for-target");
                    }
                    waitForTargetFromArgs = args[++i];
                    continue;
                }
                commandTokens.add(arg);
            }

            String configuredPluginDir = firstNonBlank(
                    pluginDirFromArgs,
                    environment.get(PLUGIN_DIRECTORY_ENV),
                    DEFAULT_PLUGIN_DIRECTORY);

            String configuredMode = firstNonBlank(
                    discoveryModeFromArgs,
                    environment.get(PLUGIN_DISCOVERY_MODE_ENV),
                    PluginDiscoveryMode.STARTUP_ONLY.getValue());

            String waitForTargetValue = waitForTargetFromArgs != null
                    ? waitForTargetFromArgs
                    : firstNonBlank(environment.get(WAIT_FOR_TARGET_ENV), null, Boolean.TRUE.toString());
            boolean waitForTarget = parseBoolean(waitForTargetValue, "--wait-for-target");
            String configPath = environment.get(CONFIG_PATH_ENV);
            String legacyPluginArtifactPath = environment.get(PLUGIN_ARTIFACT_ENV);
            return new LaunchOptions(
                    Path.of(configuredPluginDir),
                    PluginDiscoveryMode.fromValue(configuredMode),
                    configPath,
                    legacyPluginArtifactPath,
                    waitForTarget,
                    !commandTokens.isEmpty(),
                    commandTokens);
        }

        boolean waitForTarget() {
            return waitForTarget;
        }

        private static boolean parseBoolean(String value, String optionName) {
            if ("true".equalsIgnoreCase(value)) {
                return true;
            }
            if ("false".equalsIgnoreCase(value)) {
                return false;
            }
            throw new IllegalArgumentException(
                    "Invalid value for " + optionName + ": '" + value + "' (expected true or false)");
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
