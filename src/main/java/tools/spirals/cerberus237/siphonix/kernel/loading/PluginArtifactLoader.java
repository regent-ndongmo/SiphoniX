package tools.spirals.cerberus237.siphonix.kernel.loading;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ServiceLoader;

import tools.spirals.cerberus237.siphonix.api.plugin.management.ScenarioManagementPlugin;

public class PluginArtifactLoader {

    public LoadedPluginHandle loadScenarioManagementPlugin(Path artifactPath) {
        try {
            URLClassLoader classLoader = new URLClassLoader(
                    new URL[] { artifactPath.toUri().toURL() },
                    getClass().getClassLoader());
            ServiceLoader<ScenarioManagementPlugin> loader =
                    ServiceLoader.load(ScenarioManagementPlugin.class, classLoader);

            for (ScenarioManagementPlugin plugin : loader) {
                return new LoadedPluginHandle(plugin, classLoader, artifactPath);
            }

            classLoader.close();
            throw new IllegalStateException(
                    "No ScenarioManagementPlugin implementation found in artifact: " + artifactPath);
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Failed to load plugin artifact " + artifactPath + ": " + ex.getMessage(), ex);
        }
    }

    public static final class LoadedPluginHandle implements AutoCloseable {
        private final ScenarioManagementPlugin plugin;
        private final URLClassLoader classLoader;
        private final Path artifactPath;

        private LoadedPluginHandle(ScenarioManagementPlugin plugin, URLClassLoader classLoader, Path artifactPath) {
            this.plugin = plugin;
            this.classLoader = classLoader;
            this.artifactPath = artifactPath;
        }

        public ScenarioManagementPlugin getPlugin() {
            return plugin;
        }

        public Path getArtifactPath() {
            return artifactPath;
        }

        @Override
        public void close() throws Exception {
            classLoader.close();
        }
    }
}
