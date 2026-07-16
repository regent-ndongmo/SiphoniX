package tools.spirals.cerberus237.siphonix.kernel;

import org.junit.Assert;
import org.junit.Test;

public class PluginRegistryTest {

    @Test
    public void shouldInitializeStartAndStopAllRegisteredPlugins() {
        PluginRegistry registry = new PluginRegistry();
        RecordingPlugin pluginA = new RecordingPlugin("plugin-a");
        RecordingPlugin pluginB = new RecordingPlugin("plugin-b");

        registry.register(pluginA);
        registry.register(pluginB);

        registry.initializeAll(new DefaultPluginContext());
        registry.startAll();
        registry.stopAll();

        Assert.assertEquals(PluginState.STOPPED, pluginA.getState());
        Assert.assertEquals(PluginState.STOPPED, pluginB.getState());
        Assert.assertEquals(1, pluginA.initializeCalls);
        Assert.assertEquals(1, pluginA.startCalls);
        Assert.assertEquals(1, pluginA.stopCalls);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectDuplicatePluginIds() {
        PluginRegistry registry = new PluginRegistry();

        registry.register(new RecordingPlugin("plugin-a"));
        registry.register(new RecordingPlugin("plugin-a"));
    }

    private static class RecordingPlugin implements Plugin {
        private final String id;
        private PluginState state = PluginState.CREATED;
        private int initializeCalls;
        private int startCalls;
        private int stopCalls;

        RecordingPlugin(String id) {
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getVersion() {
            return "test";
        }

        @Override
        public PluginState getState() {
            return state;
        }

        @Override
        public void initialize(PluginContext context) {
            initializeCalls++;
            state = PluginState.INITIALIZED;
        }

        @Override
        public void start() {
            startCalls++;
            state = PluginState.RUNNING;
        }

        @Override
        public void stop() {
            stopCalls++;
            state = PluginState.STOPPED;
        }
    }
}
