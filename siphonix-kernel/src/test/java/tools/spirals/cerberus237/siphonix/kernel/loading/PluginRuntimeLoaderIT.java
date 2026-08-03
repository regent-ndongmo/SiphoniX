package tools.spirals.cerberus237.siphonix.kernel.loading;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import tools.spirals.cerberus237.siphonix.api.plugin.Plugin;
import tools.spirals.cerberus237.siphonix.api.plugin.PluginState;
import tools.spirals.cerberus237.siphonix.api.plugin.management.ScenarioManagementPlugin;
import tools.spirals.cerberus237.siphonix.kernel.DefaultPluginContext;
import tools.spirals.cerberus237.siphonix.kernel.PluginRegistry;

public class PluginRuntimeLoaderIT {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void shouldLoadStartupPluginAndApplyScenarioSourceWhenConfigured() throws Exception {
        Path root = temporaryFolder.newFolder("runtime-loader-startup-it").toPath();
        Path pluginDir = Files.createDirectories(root.resolve("plugins"));
        TestPluginJarBuilder.createScenarioManagementPluginJar(pluginDir, "ScenarioStartupPlugin", "scenario-startup");
        Path scenarioPath = Files.writeString(root.resolve("scenario.yaml"), "name: scenario-startup\n");

        PluginRegistry registry = new PluginRegistry();
        try (PluginRuntimeLoader loader = new PluginRuntimeLoader(
                registry,
                new DefaultPluginContext(),
                pluginDir,
                PluginDiscoveryMode.STARTUP_ONLY,
                scenarioPath.toString())) {
            loader.onRuntimeInitialized();
            loader.loadStartupPlugins();

            Plugin plugin = registry.get("scenario-startup");
            Assert.assertTrue(plugin instanceof ScenarioManagementPlugin);
            ScenarioManagementPlugin managementPlugin = (ScenarioManagementPlugin) plugin;
            Assert.assertEquals(List.of(scenarioPath.toString()),
                    managementPlugin.getScenarioManagementService().listScenarios());
            Assert.assertEquals(PluginState.INITIALIZED, plugin.getState());
        }
    }

    @Test
    public void shouldTrackPendingArtifactsInWatchManualMode() throws Exception {
        Path root = temporaryFolder.newFolder("runtime-loader-manual-it").toPath();
        Path pluginDir = Files.createDirectories(root.resolve("plugins"));
        TestPluginJarBuilder.createBasicPluginJar(pluginDir, "ManualPluginOne", "manual-plugin-1");

        PluginRegistry registry = new PluginRegistry();
        try (PluginRuntimeLoader loader = new PluginRuntimeLoader(
                registry,
                new DefaultPluginContext(),
                pluginDir,
                PluginDiscoveryMode.WATCH_MANUAL,
                null)) {
            loader.onRuntimeInitialized();
            loader.loadStartupPlugins();

            Path pendingJar = TestPluginJarBuilder.createBasicPluginJar(pluginDir, "ManualPluginTwo", "manual-plugin-2");
            loader.scanNow();

            Assert.assertEquals(1, registry.list().size());
            Assert.assertEquals(List.of(pendingJar.toString()), loader.listPendingArtifacts());
        }
    }

    @Test
    public void shouldUnloadPluginWhenArtifactIsRemovedAndDirectoryIsScanned() throws Exception {
        Path root = temporaryFolder.newFolder("runtime-loader-remove-it").toPath();
        Path pluginDir = Files.createDirectories(root.resolve("plugins"));
        Path pluginJar = TestPluginJarBuilder.createBasicPluginJar(pluginDir, "RemovablePlugin", "remove-plugin");

        PluginRegistry registry = new PluginRegistry();
        try (PluginRuntimeLoader loader = new PluginRuntimeLoader(
                registry,
                new DefaultPluginContext(),
                pluginDir,
                PluginDiscoveryMode.STARTUP_ONLY,
                null)) {
            loader.onRuntimeInitialized();
            loader.loadStartupPlugins();
            Assert.assertEquals(1, registry.list().size());

            Files.delete(pluginJar);
            loader.scanNow();

            Assert.assertTrue(registry.list().isEmpty());
            Assert.assertTrue(loader.listLoadedPlugins().isEmpty());
        }
    }

    @Test
    public void shouldStartPluginsLoadedAfterRuntimeStart() throws Exception {
        Path root = temporaryFolder.newFolder("runtime-loader-started-it").toPath();
        Path pluginDir = Files.createDirectories(root.resolve("plugins"));
        TestPluginJarBuilder.createBasicPluginJar(pluginDir, "StartedPlugin", "started-plugin");

        PluginRegistry registry = new PluginRegistry();
        try (PluginRuntimeLoader loader = new PluginRuntimeLoader(
                registry,
                new DefaultPluginContext(),
                pluginDir,
                PluginDiscoveryMode.STARTUP_ONLY,
                null)) {
            loader.onRuntimeStarted();
            loader.loadStartupPlugins();

            Plugin plugin = registry.get("started-plugin");
            Assert.assertEquals(PluginState.RUNNING, plugin.getState());
        }
    }
}
