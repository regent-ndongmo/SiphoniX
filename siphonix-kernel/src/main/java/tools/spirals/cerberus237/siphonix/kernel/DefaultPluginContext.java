package tools.spirals.cerberus237.siphonix.kernel;

import tools.spirals.cerberus237.siphonix.api.plugin.PluginContext;

/**
 * Default {@link PluginContext} implementation backed by process environment variables.
 * <p>
 * This context currently exposes the target service URL consumed by runtime plugins during
 * bootstrap. The value is resolved from {@code TARGET_URL} when present, otherwise a module-level
 * default endpoint is used.
 * </p>
 *
 * @author Arléon Zemtsop (Cerberus)
 */
public class DefaultPluginContext implements PluginContext {

    private static final String DEFAULT_TARGET_URL =
            "http://adaptable-teastore-image:8080/tools.descartes.teastore.image/rest";

    private final String targetServiceUrl;

    /**
     * Creates a context by resolving the target URL from environment.
     * <p>
     * Resolution order:
     * </p>
     * <ol>
     * <li>{@code TARGET_URL} environment variable when non-null.</li>
     * <li>{@link #DEFAULT_TARGET_URL} fallback otherwise.</li>
     * </ol>
     */
    public DefaultPluginContext() {
        this.targetServiceUrl = System.getenv().getOrDefault("TARGET_URL", DEFAULT_TARGET_URL);
    }

    /**
     * Returns the resolved target service URL.
     *
     * @return target service base URL, never null
     */
    @Override
    public String getTargetServiceUrl() {
        return targetServiceUrl;
    }
}
