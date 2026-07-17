package tools.spirals.cerberus237.siphonix.kernel.loading;

import org.junit.Assert;
import org.junit.Test;

public class PluginDiscoveryModeTest {

    @Test
    public void shouldParseKnownModes() {
        Assert.assertEquals(PluginDiscoveryMode.STARTUP_ONLY, PluginDiscoveryMode.fromValue("startup-only"));
        Assert.assertEquals(PluginDiscoveryMode.WATCH_AUTO, PluginDiscoveryMode.fromValue("watch-auto"));
        Assert.assertEquals(PluginDiscoveryMode.WATCH_MANUAL, PluginDiscoveryMode.fromValue("watch-manual"));
    }

    @Test
    public void shouldParseTrimmedAndCaseInsensitiveMode() {
        Assert.assertEquals(PluginDiscoveryMode.WATCH_AUTO, PluginDiscoveryMode.fromValue("  WATCH-AUTO  "));
    }

    @Test
    public void shouldDefaultToStartupOnlyWhenEmpty() {
        Assert.assertEquals(PluginDiscoveryMode.STARTUP_ONLY, PluginDiscoveryMode.fromValue(null));
        Assert.assertEquals(PluginDiscoveryMode.STARTUP_ONLY, PluginDiscoveryMode.fromValue("   "));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectUnsupportedMode() {
        PluginDiscoveryMode.fromValue("auto");
    }
}
