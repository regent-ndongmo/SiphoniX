package tools.spirals.cerberus237.siphonix.api.plugin;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

public class PluginStateTest {

    @Test
    public void exposesExpectedLifecycleOrder() {
        PluginState[] expected = new PluginState[] {
                PluginState.CREATED,
                PluginState.INITIALIZED,
                PluginState.RUNNING,
                PluginState.STOPPED,
                PluginState.FAILED
        };

        assertArrayEquals(expected, PluginState.values());
    }
}
