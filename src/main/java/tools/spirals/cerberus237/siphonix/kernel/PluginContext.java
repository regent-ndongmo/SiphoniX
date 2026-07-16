package tools.spirals.cerberus237.siphonix.kernel;

/**
 * Shared runtime context passed to plugins during initialization.
 */
public interface PluginContext {

    String getTargetServiceUrl();
}
