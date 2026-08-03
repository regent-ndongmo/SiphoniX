package tools.spirals.cerberus237.siphonix.api.plugin;

/**
 * Defines the lifecycle contract for a runtime-loadable SiphoniX plugin.
 * <p>
 * Implementations are expected to provide stable identity metadata, expose their current
 * lifecycle state, and react to lifecycle transitions initiated by the runtime. The expected
 * sequence is typically:
 * </p>
 * <ol>
 * <li>Plugin instance creation.</li>
 * <li>{@link #initialize(PluginContext)} to inject runtime context and prepare resources.</li>
 * <li>{@link #start()} to activate processing and external integrations.</li>
 * <li>{@link #stop()} to release resources and terminate background activities.</li>
 * </ol>
 * <p>
 * Implementations should be idempotent when possible and must leave the system in a consistent
 * state if startup or shutdown fails.
 * </p>
 *
 * @author Arléon Zemtsop (Cerberus)
 */
public interface Plugin {

    /**
     * Returns the unique plugin identifier.
     * <p>
     * This identifier is used by discovery, registry, and operational tooling to distinguish one
     * plugin from another.
     * </p>
     *
     * @return a non-null, stable plugin identifier
     */
    String getId();

    /**
     * Returns the plugin version string.
     * <p>
     * The format is implementation-defined, but semantic versioning is recommended for
     * compatibility management and release tracking.
     * </p>
     *
     * @return the plugin version, never null
     */
    String getVersion();

    /**
     * Returns the current lifecycle state of this plugin instance.
     *
     * @return the current {@link PluginState}
     */
    PluginState getState();

    /**
     * Initializes the plugin with runtime context.
     * <p>
     * This method should validate configuration, wire dependencies, and prepare resources required
     * for {@link #start()}. Heavy processing should generally be deferred to startup.
     * </p>
     *
     * @param context shared runtime context provided by the host application
     */
    void initialize(PluginContext context);

    /**
     * Starts plugin activity.
     * <p>
     * Implementations may start schedulers, event consumers, or network listeners. Any failure
     * should be surfaced through an exception to allow the host to mark the plugin as failed.
     * </p>
     */
    void start();

    /**
     * Stops plugin activity and releases allocated resources.
     * <p>
     * Implementations should stop background work, flush pending operations when relevant, and
     * leave the plugin in a safe terminal state.
     * </p>
     */
    void stop();
}
