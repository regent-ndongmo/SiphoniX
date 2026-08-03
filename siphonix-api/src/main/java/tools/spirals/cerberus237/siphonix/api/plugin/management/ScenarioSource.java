package tools.spirals.cerberus237.siphonix.api.plugin.management;

import java.io.IOException;
import java.util.Map;

/**
 * Defines a scenario content provider abstraction.
 * <p>
 * A source encapsulates how scenario data is identified and loaded, independently of transport
 * format or storage backend. Implementations may represent file paths, remote URLs, classpath
 * resources, or in-memory payloads.
 * </p>
 *
 * @author Arléon Zemtsop (Cerberus)
 */
public interface ScenarioSource {

    /**
     * Returns the source type identifier.
     * <p>
     * Typical values can denote encoding or transport strategy, such as {@code yaml},
     * {@code json}, {@code xml}, {@code file}, or {@code http}.
     * </p>
     *
     * @return source type descriptor, never null
     */
    String getType();

    /**
     * Returns a human-readable source reference.
     * <p>
     * This value should be suitable for logs and diagnostics, for example a URI, file path, or
     * logical identifier.
     * </p>
     *
     * @return source reference string, never null
     */
    String getReference();

    /**
     * Loads and parses scenario content into a map representation.
     *
     * @return scenario payload represented as key-value structure
     * @throws IOException when source retrieval or parsing fails
     */
    Map<String, Object> load() throws IOException;
}
