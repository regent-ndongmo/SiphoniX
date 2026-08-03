package tools.spirals.cerberus237.siphonix.api.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Test;

public class PluginContractTest {

    @Test
    public void minimalPluginImplementationCanExposeCoreMetadataAndState() {
        PluginContext context = () -> "http://target.service";
        MinimalPlugin plugin = new MinimalPlugin();

        plugin.initialize(context);
        plugin.start();
        plugin.stop();

        assertEquals("minimal-plugin", plugin.getId());
        assertEquals("1.0.0-test", plugin.getVersion());
        assertEquals(PluginState.STOPPED, plugin.getState());
        assertSame(context, plugin.lastContext);
    }

    @Test
    public void pluginRegistryViewCanExposePluginList() {
        MinimalPlugin plugin = new MinimalPlugin();
        PluginRegistryView view = () -> java.util.List.of(plugin);

        assertEquals(1, view.list().size());
        assertSame(plugin, view.list().iterator().next());
    }

    private static final class MinimalPlugin implements Plugin {

        private PluginState state = PluginState.CREATED;
        private PluginContext lastContext;

        @Override
        public String getId() {
            return "minimal-plugin";
        }

        @Override
        public String getVersion() {
            return "1.0.0-test";
        }

        @Override
        public PluginState getState() {
            return state;
        }

        @Override
        public void initialize(PluginContext context) {
            this.lastContext = context;
            this.state = PluginState.INITIALIZED;
        }

        @Override
        public void start() {
            this.state = PluginState.RUNNING;
        }

        @Override
        public void stop() {
            this.state = PluginState.STOPPED;
        }
    }
}
