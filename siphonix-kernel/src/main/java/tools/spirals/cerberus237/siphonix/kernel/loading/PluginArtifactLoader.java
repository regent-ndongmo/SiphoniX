package tools.spirals.cerberus237.siphonix.kernel.loading;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import tools.spirals.cerberus237.siphonix.api.plugin.Plugin;
import tools.spirals.cerberus237.siphonix.api.plugin.management.ScenarioManagementPlugin;

/**
 * Artifact loader based on Java {@link ServiceLoader}.
 *
 * <p>This loader opens a dedicated {@link URLClassLoader} per artifact and tries to resolve
 * plugin implementations for one or more plugin contracts. The associated classloader lifecycle
 * is exposed via {@link LoadedPluginHandle} to allow clean unload/reload behavior.
 *
 * @author Arléon Zemtsop (Cerberus)
 */
public class PluginArtifactLoader {

    /**
     * Compatibility helper for scenario management plugins.
     *
     * @param artifactPath plugin artifact path.
     * @return loaded handle for a {@link ScenarioManagementPlugin}.
     * @throws IllegalStateException when artifact loading fails or no implementation is found
     */
    public LoadedPluginHandle loadScenarioManagementPlugin(Path artifactPath) {
        return loadPlugin(artifactPath, ScenarioManagementPlugin.class);
    }

    /**
     * Loads the first plugin implementation matching a given plugin type.
     *
     * @param artifactPath plugin artifact path.
     * @param pluginType plugin contract to resolve.
     * @param <T> plugin type.
     * @return loaded plugin handle.
    * @throws IllegalStateException when classloading fails or no provider is found
     */
    public <T extends Plugin> LoadedPluginHandle<T> loadPlugin(Path artifactPath, Class<T> pluginType) {
        try {
            URLClassLoader classLoader = new URLClassLoader(
                    new URL[] { artifactPath.toUri().toURL() },
                    getClass().getClassLoader());
            ServiceLoader<T> loader = ServiceLoader.load(pluginType, classLoader);

            for (T plugin : loader) {
                return new LoadedPluginHandle<>(plugin, classLoader, artifactPath, pluginType);
            }

            classLoader.close();
            throw new IllegalStateException(
                    "No " + pluginType.getSimpleName() + " implementation found in artifact: " + artifactPath);
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Failed to load plugin artifact " + artifactPath + ": " + ex.getMessage(), ex);
        }
    }

    /**
     * Tries supported plugin contracts in order and returns the first loadable plugin.
     *
     * @param artifactPath plugin artifact path.
     * @param pluginTypes candidate plugin contracts ordered by priority.
     * @return loaded plugin handle.
         * @throws IllegalStateException when no candidate type can be loaded from the artifact
     */
    public LoadedPluginHandle<? extends Plugin> loadAnyPlugin(Path artifactPath,
            List<Class<? extends Plugin>> pluginTypes) {
        List<String> failures = new ArrayList<>();
        for (Class<? extends Plugin> pluginType : pluginTypes) {
            try {
                return loadPluginUnchecked(artifactPath, pluginType);
            } catch (IllegalStateException ex) {
                failures.add(pluginType.getSimpleName() + ": " + ex.getMessage());
            }
        }

        throw new IllegalStateException(
                "No supported plugin type found in artifact " + artifactPath + ". Attempts: " + failures);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private LoadedPluginHandle<? extends Plugin> loadPluginUnchecked(Path artifactPath,
            Class<? extends Plugin> pluginType) {
        return (LoadedPluginHandle) loadPlugin(artifactPath, (Class) pluginType);
    }

    /**
     * Loaded plugin handle with classloader ownership.
     *
     * @param <T> loaded plugin type.
     * @author Arléon Zemtsop (Cerberus)
     */
    public static final class LoadedPluginHandle<T extends Plugin> implements AutoCloseable {
        private final T plugin;
        private final URLClassLoader classLoader;
        private final Path artifactPath;
        private final Class<T> pluginType;

        private LoadedPluginHandle(T plugin, URLClassLoader classLoader, Path artifactPath, Class<T> pluginType) {
            this.plugin = plugin;
            this.classLoader = classLoader;
            this.artifactPath = artifactPath;
            this.pluginType = pluginType;
        }

        /**
         * Returns the instantiated plugin provider.
         *
         * @return loaded plugin instance
         */
        public T getPlugin() {
            return plugin;
        }

        /**
         * Returns the artifact path that produced this plugin instance.
         *
         * @return plugin artifact path
         */
        public Path getArtifactPath() {
            return artifactPath;
        }

        /**
         * Returns the plugin contract type used for service discovery.
         *
         * @return plugin type token
         */
        public Class<T> getPluginType() {
            return pluginType;
        }

        /**
         * Closes the artifact classloader.
         *
         * @throws Exception if classloader close fails.
         */
        @Override
        public void close() throws Exception {
            classLoader.close();
        }
    }
}
