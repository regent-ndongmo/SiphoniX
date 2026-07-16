package tools.spirals.cerberus237.siphonix.kernel;

/**
 * Generic lifecycle contract for runtime-loadable management plugins.
 */
public interface Plugin {

    String getId();

    String getVersion();

    PluginState getState();

    void initialize(PluginContext context);

    void start();

    void stop();
}
