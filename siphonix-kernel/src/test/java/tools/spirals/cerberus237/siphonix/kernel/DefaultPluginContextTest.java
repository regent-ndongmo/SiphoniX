package tools.spirals.cerberus237.siphonix.kernel;

import org.junit.Assert;
import org.junit.Test;

public class DefaultPluginContextTest {

    @Test
    public void shouldResolveTargetUrlFromEnvironmentOrDefault() {
        DefaultPluginContext context = new DefaultPluginContext();

        String expected = System.getenv().getOrDefault(
                "TARGET_URL",
                "http://adaptable-teastore-image:8080/tools.descartes.teastore.image/rest");

        Assert.assertEquals(expected, context.getTargetServiceUrl());
    }
}
