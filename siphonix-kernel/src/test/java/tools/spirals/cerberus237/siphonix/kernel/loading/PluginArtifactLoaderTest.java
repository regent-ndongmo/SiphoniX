package tools.spirals.cerberus237.siphonix.kernel.loading;

import java.nio.file.Path;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import tools.spirals.cerberus237.siphonix.api.plugin.Plugin;

public class PluginArtifactLoaderTest {

    @Test
    public void shouldFailWhenArtifactDoesNotExist() {
        PluginArtifactLoader loader = new PluginArtifactLoader();

        try {
            loader.loadPlugin(Path.of("/non/existing/plugin.jar"), Plugin.class);
            Assert.fail("Expected load failure for missing artifact");
        } catch (IllegalStateException ex) {
            Assert.assertTrue(ex.getMessage().contains("Failed to load plugin artifact"));
        }
    }

    @Test
    public void shouldFailWhenNoCandidatePluginTypeIsProvided() {
        PluginArtifactLoader loader = new PluginArtifactLoader();

        try {
            loader.loadAnyPlugin(Path.of("/tmp/does-not-matter.jar"), List.of());
            Assert.fail("Expected load failure for empty plugin type list");
        } catch (IllegalStateException ex) {
            Assert.assertTrue(ex.getMessage().contains("No supported plugin type found"));
        }
    }
}
