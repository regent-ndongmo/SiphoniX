package tools.spirals.cerberus237.siphonix.kernel;

import org.junit.Assert;
import org.junit.Test;

import tools.spirals.cerberus237.siphonix.api.plugin.Plugin;
import tools.spirals.cerberus237.siphonix.api.plugin.PluginContext;
import tools.spirals.cerberus237.siphonix.api.plugin.PluginState;

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

    @Test
    public void shouldRegisterAndInitializePluginAtRuntime() {
        PluginRegistry registry = new PluginRegistry();
        RecordingPlugin plugin = new RecordingPlugin("dynamic-plugin");

        registry.registerAndInitialize(plugin, new DefaultPluginContext());

        Assert.assertEquals(PluginState.INITIALIZED, plugin.getState());
        Assert.assertEquals(1, plugin.initializeCalls);
        Assert.assertEquals(plugin, registry.get("dynamic-plugin"));
    }

    @Test
    public void shouldRemoveRunningPluginAndStopIt() {
        PluginRegistry registry = new PluginRegistry();
        RecordingPlugin plugin = new RecordingPlugin("dynamic-plugin");
        registry.register(plugin);
        registry.initializeAll(new DefaultPluginContext());
        registry.start("dynamic-plugin");

        Plugin removed = registry.remove("dynamic-plugin");

        Assert.assertEquals(plugin, removed);
        Assert.assertEquals(PluginState.STOPPED, plugin.getState());
        Assert.assertEquals(1, plugin.stopCalls);
        Assert.assertEquals(0, registry.list().size());
    }

    @Test
    public void shouldReplaceRunningPluginAndStartReplacement() {
        PluginRegistry registry = new PluginRegistry();
        RecordingPlugin previous = new RecordingPlugin("dynamic-plugin");
        RecordingPlugin replacement = new RecordingPlugin("dynamic-plugin");

        registry.register(previous);
        registry.initializeAll(new DefaultPluginContext());
        registry.start("dynamic-plugin");

        Plugin old = registry.replace("dynamic-plugin", replacement, new DefaultPluginContext());

        Assert.assertEquals(previous, old);
        Assert.assertEquals(PluginState.STOPPED, previous.getState());
        Assert.assertEquals(1, previous.stopCalls);
        Assert.assertEquals(PluginState.RUNNING, replacement.getState());
        Assert.assertEquals(1, replacement.initializeCalls);
        Assert.assertEquals(1, replacement.startCalls);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectReplaceWhenIdsDoNotMatch() {
        PluginRegistry registry = new PluginRegistry();
        RecordingPlugin previous = new RecordingPlugin("dynamic-plugin");
        RecordingPlugin replacement = new RecordingPlugin("other-plugin");

        registry.register(previous);
        registry.initializeAll(new DefaultPluginContext());

        registry.replace("dynamic-plugin", replacement, new DefaultPluginContext());
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
