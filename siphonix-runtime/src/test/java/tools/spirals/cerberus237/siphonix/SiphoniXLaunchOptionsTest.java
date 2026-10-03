package tools.spirals.cerberus237.siphonix;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class SiphoniXLaunchOptionsTest {

    @Test
    public void shouldWaitForTargetByDefault() {
        SiphoniX.LaunchOptions options = SiphoniX.LaunchOptions.fromArgs(new String[0], Map.of());

        Assert.assertTrue(options.waitForTarget());
    }

    @Test
    public void shouldDisableTargetWaitWithSeparatedCliValue() {
        SiphoniX.LaunchOptions options = SiphoniX.LaunchOptions.fromArgs(
                new String[] { "--wait-for-target", "false" }, Map.of());

        Assert.assertFalse(options.waitForTarget());
    }

    @Test
    public void shouldDisableTargetWaitWithInlineCliValue() {
        SiphoniX.LaunchOptions options = SiphoniX.LaunchOptions.fromArgs(
                new String[] { "--wait-for-target=false" }, Map.of());

        Assert.assertFalse(options.waitForTarget());
    }

    @Test
    public void shouldReadTargetWaitFromEnvironment() {
        SiphoniX.LaunchOptions options = SiphoniX.LaunchOptions.fromArgs(
                new String[0], Map.of("SIPHONIX_WAIT_FOR_TARGET", "false"));

        Assert.assertFalse(options.waitForTarget());
    }

    @Test
    public void shouldPreferCliValueOverEnvironment() {
        SiphoniX.LaunchOptions options = SiphoniX.LaunchOptions.fromArgs(
                new String[] { "--wait-for-target=true" },
                Map.of("SIPHONIX_WAIT_FOR_TARGET", "false"));

        Assert.assertTrue(options.waitForTarget());
    }

    @Test
    public void shouldRejectInvalidTargetWaitValue() {
        try {
            SiphoniX.LaunchOptions.fromArgs(
                    new String[] { "--wait-for-target=sometimes" }, Map.of());
            Assert.fail("Expected invalid boolean option to be rejected");
        } catch (IllegalArgumentException exception) {
            Assert.assertTrue(exception.getMessage().contains("expected true or false"));
        }
    }

    @Test
    public void shouldRejectMissingTargetWaitValue() {
        try {
            SiphoniX.LaunchOptions.fromArgs(new String[] { "--wait-for-target" }, Map.of());
            Assert.fail("Expected missing option value to be rejected");
        } catch (IllegalArgumentException exception) {
            Assert.assertTrue(exception.getMessage().contains("Missing value for --wait-for-target"));
        }
    }
}
