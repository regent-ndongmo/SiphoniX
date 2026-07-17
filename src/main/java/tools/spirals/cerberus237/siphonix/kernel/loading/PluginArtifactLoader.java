package tools.spirals.cerberus237.siphonix.kernel.loading;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import tools.spirals.cerberus237.siphonix.api.plugin.Plugin;
import tools.spirals.cerberus237.siphonix.api.plugin.management.ScenarioManagementPlugin;

public class PluginArtifactLoader {

    public LoadedPluginHandle loadScenarioManagementPlugin(Path artifactPath) {
        return loadPlugin(artifactPath, ScenarioManagementPlugin.class);
    }

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

        public T getPlugin() {
            return plugin;
        }

        public Path getArtifactPath() {
            return artifactPath;
        }

        public Class<T> getPluginType() {
            return pluginType;
        }

        @Override
        public void close() throws Exception {
            classLoader.close();
        }
    }
}
