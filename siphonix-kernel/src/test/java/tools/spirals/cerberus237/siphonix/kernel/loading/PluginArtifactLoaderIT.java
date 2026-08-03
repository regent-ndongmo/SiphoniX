package tools.spirals.cerberus237.siphonix.kernel.loading;

import java.nio.file.Path;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import tools.spirals.cerberus237.siphonix.api.plugin.Plugin;
import tools.spirals.cerberus237.siphonix.api.plugin.management.ScenarioManagementPlugin;

public class PluginArtifactLoaderIT {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void shouldLoadPluginImplementationFromServiceJar() throws Exception {
        Path root = temporaryFolder.newFolder("plugin-artifact-loader-it").toPath();
        Path pluginJar = TestPluginJarBuilder.createBasicPluginJar(root, "BasicPluginImpl", "basic-plugin");

        PluginArtifactLoader loader = new PluginArtifactLoader();
        try (PluginArtifactLoader.LoadedPluginHandle<Plugin> handle = loader.loadPlugin(pluginJar, Plugin.class)) {
            Assert.assertEquals("basic-plugin", handle.getPlugin().getId());
            Assert.assertEquals(Plugin.class, handle.getPluginType());
            Assert.assertEquals(pluginJar, handle.getArtifactPath());
        }
    }

    @Test
    public void shouldPreferScenarioManagementContractWhenLoadingAnyPlugin() throws Exception {
        Path root = temporaryFolder.newFolder("plugin-artifact-loader-any-it").toPath();
        Path pluginJar = TestPluginJarBuilder
                .createScenarioManagementPluginJar(root, "ScenarioPluginImpl", "scenario-plugin");
        java.util.List<Class<? extends Plugin>> pluginTypes = java.util.List.of(
            ScenarioManagementPlugin.class,
            Plugin.class);

        PluginArtifactLoader loader = new PluginArtifactLoader();
        try (PluginArtifactLoader.LoadedPluginHandle<? extends Plugin> handle = loader.loadAnyPlugin(
                pluginJar,
            pluginTypes)) {
            Assert.assertTrue(handle.getPlugin() instanceof ScenarioManagementPlugin);
            Assert.assertEquals(ScenarioManagementPlugin.class, handle.getPluginType());
            Assert.assertEquals("scenario-plugin", handle.getPlugin().getId());
        }
    }

    @Test
    public void shouldFailWhenArtifactContainsNoServiceProvider() throws Exception {
        Path root = temporaryFolder.newFolder("plugin-artifact-loader-empty-it").toPath();
        Path emptyJar = TestPluginJarBuilder.createEmptyJar(root, "empty.jar");

        PluginArtifactLoader loader = new PluginArtifactLoader();

        try {
            loader.loadPlugin(emptyJar, Plugin.class);
            Assert.fail("Expected failure when no provider is available");
        } catch (IllegalStateException ex) {
            Assert.assertTrue(ex.getMessage().contains("No Plugin implementation found"));
        }
    }
}
