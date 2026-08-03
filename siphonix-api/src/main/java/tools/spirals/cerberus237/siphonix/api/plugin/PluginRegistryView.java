package tools.spirals.cerberus237.siphonix.api.plugin;

import java.util.Collection;

/**
 * Read-only registry view exposed to plugins that need to inspect loaded plugins.
 */
public interface PluginRegistryView {

    Collection<Plugin> list();
}