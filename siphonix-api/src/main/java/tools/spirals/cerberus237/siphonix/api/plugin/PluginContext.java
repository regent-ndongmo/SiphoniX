package tools.spirals.cerberus237.siphonix.api.plugin;

/**
 * Represents shared runtime information exposed by the host to plugins.
 * <p>
 * This context is intentionally minimal at API level and can be enriched over time without
 * requiring plugins to depend on kernel internals. Implementations may provide dynamic values,
 * but method contracts should remain stable across runtime versions.
 * </p>
 *
 * @author Arléon Zemtsop (Cerberus)
 */
public interface PluginContext {

    /**
     * Returns the URL of the target service managed or observed by the runtime.
     * <p>
     * Plugins can use this value for bootstrap connectivity checks, endpoint registration, or
     * context-aware orchestration decisions.
     * </p>
     *
     * @return the target service base URL, never null
     */
    String getTargetServiceUrl();
}
