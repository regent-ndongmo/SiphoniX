package tools.spirals.cerberus237.siphonix.api.plugin;

import java.util.Collection;

/**
 * Read-only projection of the runtime plugin registry.
 * <p>
 * This contract allows plugins to inspect other loaded plugins for introspection, diagnostics,
 * and optional coordination scenarios without exposing mutating operations.
 * </p>
 *
 * @author Arléon Zemtsop (Cerberus)
 */
public interface PluginRegistryView {

    /**
     * Returns the currently registered plugin instances.
     * <p>
     * The returned collection is implementation-defined and may be immutable or defensive-copied.
     * Callers should treat it as read-only and avoid assumptions about ordering.
     * </p>
     *
     * @return a collection view of registered plugins
     */
    Collection<Plugin> list();
}