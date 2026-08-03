package tools.spirals.cerberus237.siphonix.api.plugin;

/**
 * Enumerates the lifecycle states a plugin can traverse while managed by SiphoniX.
 * <p>
 * State transitions are driven by host lifecycle operations and plugin outcomes. The host runtime
 * can use these values for monitoring, health reporting, and failure handling.
 * </p>
 *
 * @author Arléon Zemtsop (Cerberus)
 */
public enum PluginState {
    /**
     * Plugin instance has been constructed but not yet initialized.
     */
    CREATED,
    /**
     * Plugin has completed initialization and is ready to be started.
     */
    INITIALIZED,
    /**
     * Plugin is currently active and executing its runtime responsibilities.
     */
    RUNNING,
    /**
     * Plugin has been stopped gracefully and is no longer active.
     */
    STOPPED,
    /**
     * Plugin encountered an unrecoverable error during lifecycle processing.
     */
    FAILED
}
