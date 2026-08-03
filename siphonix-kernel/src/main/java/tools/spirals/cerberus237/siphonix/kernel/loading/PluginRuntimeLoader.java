package tools.spirals.cerberus237.siphonix.kernel.loading;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.spirals.cerberus237.siphonix.api.plugin.Plugin;
import tools.spirals.cerberus237.siphonix.api.plugin.PluginContext;
import tools.spirals.cerberus237.siphonix.api.plugin.management.ScenarioManagementPlugin;
import tools.spirals.cerberus237.siphonix.api.plugin.management.ScenarioSource;
import tools.spirals.cerberus237.siphonix.kernel.PluginRegistry;

/**
 * Runtime plugin directory manager.
 *
 * <p>This component is responsible for:
 * <ul>
 * <li>Startup plugin discovery from a configured directory.</li>
 * <li>Optional periodic watch behavior depending on discovery mode.</li>
 * <li>Artifact-to-plugin mapping for safe unload and replacement.</li>
 * <li>Scenario source bootstrap for {@link ScenarioManagementPlugin} instances.</li>
 * </ul>
 *
 * <p>In manual watch mode, changed artifacts are tracked as pending until explicitly loaded.
 *
 * @author Arléon Zemtsop (Cerberus)
 */
public class PluginRuntimeLoader implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(PluginRuntimeLoader.class);

    private static final List<Class<? extends Plugin>> SUPPORTED_PLUGIN_TYPES = List.of(
            ScenarioManagementPlugin.class,
            Plugin.class);

    private static final long WATCH_PERIOD_SECONDS = 2L;

    private final PluginRegistry pluginRegistry;
    private final PluginContext pluginContext;
    private final PluginArtifactLoader artifactLoader = new PluginArtifactLoader();
    private final Path pluginDirectory;
    private final PluginDiscoveryMode discoveryMode;
    private final String configPath;

    private final Map<String, ActivePlugin> activePlugins = new LinkedHashMap<>();
    private final Map<Path, Long> observedArtifacts = new LinkedHashMap<>();
    private final Set<Path> pendingArtifacts = new LinkedHashSet<>();
    private final Map<Path, String> artifactToPlugin = new LinkedHashMap<>();

    private ScheduledExecutorService watcher;
    private boolean runtimeInitialized;
    private boolean runtimeStarted;
    private boolean watchEnabled;

    public PluginRuntimeLoader(PluginRegistry pluginRegistry, PluginContext pluginContext, Path pluginDirectory,
            PluginDiscoveryMode discoveryMode, String configPath) {
        this.pluginRegistry = pluginRegistry;
        this.pluginContext = pluginContext;
        this.pluginDirectory = pluginDirectory;
        this.discoveryMode = discoveryMode;
        this.configPath = configPath;
        this.watchEnabled = discoveryMode != PluginDiscoveryMode.STARTUP_ONLY;
    }

    /**
     * Performs startup plugin scan and activation according to configured mode.
     */
    public synchronized void loadStartupPlugins() {
        scanAndApply(true);
    }

    /**
     * Marks runtime as started.
     */
    public synchronized void onRuntimeStarted() {
        runtimeInitialized = true;
        runtimeStarted = true;
    }

    /**
     * Marks runtime as initialized.
     */
    public synchronized void onRuntimeInitialized() {
        runtimeInitialized = true;
    }

    /**
     * Starts periodic watch loop for plugin directory.
     */
    public synchronized void startWatcher() {
        if (discoveryMode == PluginDiscoveryMode.STARTUP_ONLY) {
            return;
        }
        if (watcher != null) {
            return;
        }

        watcher = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "siphonix-plugin-folder-watcher");
            thread.setDaemon(true);
            return thread;
        });

        watcher.scheduleWithFixedDelay(() -> {
            try {
                synchronized (PluginRuntimeLoader.this) {
                    if (!watchEnabled) {
                        return;
                    }
                    scanAndApply(false);
                }
            } catch (RuntimeException ex) {
                logger.error("[SiphoniX] Plugin watcher cycle failed", ex);
            }
        }, WATCH_PERIOD_SECONDS, WATCH_PERIOD_SECONDS, TimeUnit.SECONDS);

        logger.info("[SiphoniX] Plugin watcher started in {} mode for {}", discoveryMode.getValue(), pluginDirectory);
    }

    /**
     * Stops periodic watch loop if running.
     */
    public synchronized void stopWatcher() {
        if (watcher == null) {
            return;
        }
        watcher.shutdownNow();
        watcher = null;
    }

    /**
     * @return whether runtime watch behavior is currently enabled.
     */
    public synchronized boolean isWatchEnabled() {
        return watchEnabled;
    }

    /**
     * Enables or disables watch behavior.
     *
     * @param enabled desired watch state.
     */
    public synchronized void setWatchEnabled(boolean enabled) {
        if (discoveryMode == PluginDiscoveryMode.STARTUP_ONLY && enabled) {
            throw new IllegalStateException("Watcher cannot be enabled in startup-only mode");
        }
        this.watchEnabled = enabled;
    }

    /**
     * @return human-readable list of loaded plugins.
     */
    public synchronized List<String> listLoadedPlugins() {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, ActivePlugin> entry : activePlugins.entrySet()) {
            ActivePlugin activePlugin = entry.getValue();
            Plugin plugin = activePlugin.plugin;
            lines.add(entry.getKey() + " [type=" + activePlugin.pluginType.getSimpleName() + ", state="
                    + plugin.getState() + ", artifact=" + activePlugin.artifactPath + "]");
        }
        return lines;
    }

    /**
     * @return artifacts detected but not activated in manual mode.
     */
    public synchronized List<String> listPendingArtifacts() {
        List<String> pending = new ArrayList<>();
        for (Path path : pendingArtifacts) {
            pending.add(path.toString());
        }
        return pending;
    }

    /**
     * Loads and activates one plugin artifact immediately.
     *
     * @param artifactPath artifact path.
     * @return loaded plugin instance.
     */
    public synchronized Plugin loadPlugin(Path artifactPath) {
        return loadAndActivate(artifactPath);
    }

    /**
     * Unloads a plugin by id.
     *
     * @param pluginId plugin identifier.
     */
    public synchronized void unloadPlugin(String pluginId) {
        ActivePlugin activePlugin = activePlugins.remove(pluginId);
        if (activePlugin == null) {
            throw new IllegalArgumentException("No loaded plugin with id '" + pluginId + "'");
        }

        pluginRegistry.remove(pluginId);
        artifactToPlugin.remove(activePlugin.artifactPath);
        pendingArtifacts.remove(activePlugin.artifactPath);
        closeQuietly(activePlugin.handle);
    }

    /**
     * Reloads a plugin from its known artifact path.
     *
     * @param pluginId plugin identifier.
     */
    public synchronized void reloadPlugin(String pluginId) {
        ActivePlugin activePlugin = activePlugins.get(pluginId);
        if (activePlugin == null) {
            throw new IllegalArgumentException("No loaded plugin with id '" + pluginId + "'");
        }
        loadAndActivate(activePlugin.artifactPath);
    }

    /**
     * Triggers one scan cycle immediately.
     */
    public synchronized void scanNow() {
        scanAndApply(false);
    }

    private void scanAndApply(boolean startupPhase) {
        List<Path> jarPaths = listJarFiles();
        Set<Path> current = new LinkedHashSet<>(jarPaths);

        for (Path path : jarPaths) {
            long modifiedAt = lastModified(path);
            Long previous = observedArtifacts.get(path);
            boolean changed = previous == null || modifiedAt > previous.longValue();
            observedArtifacts.put(path, modifiedAt);

            if (!changed) {
                continue;
            }

            if (discoveryMode == PluginDiscoveryMode.WATCH_MANUAL && !startupPhase) {
                pendingArtifacts.add(path);
                logger.info("[SiphoniX] Detected plugin artifact {} (pending manual activation)", path);
                continue;
            }

            loadAndActivate(path);
        }

        List<Path> removed = new ArrayList<>();
        for (Path knownPath : observedArtifacts.keySet()) {
            if (!current.contains(knownPath)) {
                removed.add(knownPath);
            }
        }

        for (Path removedPath : removed) {
            observedArtifacts.remove(removedPath);
            pendingArtifacts.remove(removedPath);
            String pluginId = artifactToPlugin.remove(removedPath);
            if (pluginId != null && activePlugins.containsKey(pluginId)) {
                unloadPlugin(pluginId);
                logger.info("[SiphoniX] Unloaded plugin {} because artifact was removed: {}", pluginId, removedPath);
            }
        }
    }

    private Plugin loadAndActivate(Path artifactPath) {
        PluginArtifactLoader.LoadedPluginHandle<? extends Plugin> handle = artifactLoader
                .loadAnyPlugin(artifactPath, SUPPORTED_PLUGIN_TYPES);

        Plugin plugin = handle.getPlugin();
        String pluginId = plugin.getId();
        ActivePlugin previous = activePlugins.get(pluginId);

        try {
            if (pluginExists(pluginId)) {
                if (runtimeInitialized) {
                    pluginRegistry.replace(pluginId, plugin, pluginContext);
                } else {
                    pluginRegistry.remove(pluginId);
                    pluginRegistry.register(plugin);
                }
            } else {
                if (runtimeInitialized) {
                    pluginRegistry.registerAndInitialize(plugin, pluginContext);
                } else {
                    pluginRegistry.register(plugin);
                }
                if (runtimeStarted) {
                    plugin.start();
                }
            }

            ActivePlugin active = new ActivePlugin(plugin, handle, artifactPath, handle.getPluginType());
            activePlugins.put(pluginId, active);
            artifactToPlugin.put(artifactPath, pluginId);
            pendingArtifacts.remove(artifactPath);

            applyScenarioConfigIfPossible(plugin);

            if (previous != null && previous.handle != handle) {
                closeQuietly(previous.handle);
            }

            logger.info("[SiphoniX] Loaded plugin {} [{}] from {}", pluginId,
                    handle.getPluginType().getSimpleName(), artifactPath);
            return plugin;
        } catch (RuntimeException ex) {
            closeQuietly(handle);
            throw ex;
        }
    }

    private boolean pluginExists(String pluginId) {
        for (Plugin existing : pluginRegistry.list()) {
            if (existing.getId().equals(pluginId)) {
                return true;
            }
        }
        return false;
    }

    private void applyScenarioConfigIfPossible(Plugin plugin) {
        if (!(plugin instanceof ScenarioManagementPlugin)) {
            return;
        }
        if (configPath == null || configPath.trim().isEmpty()) {
            return;
        }

        Path scenarioPath = Path.of(configPath.trim());
        ScenarioSource scenarioSource = new PathFileScenarioSource(scenarioPath);
        try {
            ((ScenarioManagementPlugin) plugin).getScenarioManagementService().createScenario(scenarioSource);
            logger.info("[SiphoniX] Applied scenario source {} through plugin {}", configPath, plugin.getId());
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Failed to apply scenario source " + configPath + " for plugin " + plugin.getId(), ex);
        }
    }

    private List<Path> listJarFiles() {
        List<Path> jarPaths = new ArrayList<>();
        if (!Files.exists(pluginDirectory) || !Files.isDirectory(pluginDirectory)) {
            return jarPaths;
        }

        try (Stream<Path> stream = Files.list(pluginDirectory)) {
            stream
                    .filter(path -> Files.isRegularFile(path)
                            && path.getFileName().toString().toLowerCase().endsWith(".jar"))
                    .sorted()
                    .forEach(jarPaths::add);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to list plugin directory " + pluginDirectory, ex);
        }
        return jarPaths;
    }

    private long lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot read plugin artifact metadata: " + path, ex);
        }
    }

    private void closeQuietly(PluginArtifactLoader.LoadedPluginHandle<? extends Plugin> handle) {
        try {
            handle.close();
        } catch (Exception ex) {
            logger.warn("[SiphoniX] Failed to close classloader for {}: {}", handle.getArtifactPath(), ex.getMessage());
        }
    }

    /**
     * Releases watcher and classloader resources.
     */
    @Override
    public synchronized void close() {
        stopWatcher();
        for (ActivePlugin activePlugin : activePlugins.values()) {
            closeQuietly(activePlugin.handle);
        }
        activePlugins.clear();
        artifactToPlugin.clear();
        pendingArtifacts.clear();
        observedArtifacts.clear();
    }

    /**
     * Internal loaded plugin metadata.
     *
     * @author Arléon Zemtsop (Cerberus)
     */
    private static final class ActivePlugin {
        private final Plugin plugin;
        private final PluginArtifactLoader.LoadedPluginHandle<? extends Plugin> handle;
        private final Path artifactPath;
        private final Class<? extends Plugin> pluginType;

        private ActivePlugin(Plugin plugin, PluginArtifactLoader.LoadedPluginHandle<? extends Plugin> handle,
                Path artifactPath, Class<? extends Plugin> pluginType) {
            this.plugin = plugin;
            this.handle = handle;
            this.artifactPath = artifactPath;
            this.pluginType = pluginType;
        }
    }
}
