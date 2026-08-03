package tools.spirals.cerberus237.siphonix.kernel;

import tools.spirals.cerberus237.siphonix.api.plugin.PluginContext;

/**
 * Default plugin context implementation backed by environment variables.
 */
public class DefaultPluginContext implements PluginContext {

    private static final String DEFAULT_TARGET_URL =
            "http://adaptable-teastore-image:8080/tools.descartes.teastore.image/rest";

    private final String targetServiceUrl;

    public DefaultPluginContext() {
        this.targetServiceUrl = System.getenv().getOrDefault("TARGET_URL", DEFAULT_TARGET_URL);
    }

    @Override
    public String getTargetServiceUrl() {
        return targetServiceUrl;
    }
}
