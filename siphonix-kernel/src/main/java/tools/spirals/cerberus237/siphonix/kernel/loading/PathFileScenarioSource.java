package tools.spirals.cerberus237.siphonix.kernel.loading;

import java.nio.file.Path;
import java.util.Map;

import tools.spirals.cerberus237.siphonix.api.plugin.management.ScenarioSource;

/**
 * Generic file-backed scenario source. Plugin implementations resolve the
 * concrete format from the file path.
 *
 * <p>The source intentionally returns an empty map from {@link #load()} because
 * format-specific parsing is delegated to plugin-side configuration managers.
 * The primary purpose of this type is to carry file reference metadata through
 * the API boundary while remaining format-agnostic.
 * </p>
 *
 * @author Arléon Zemtsop (Cerberus)
 */
public class PathFileScenarioSource implements ScenarioSource {

    /**
     * Type identifier exposed to consumers of {@link ScenarioSource#getType()}.
     */
    public static final String TYPE = "path-file";

    private final Path path;

    /**
     * Creates a path-backed scenario source.
     *
     * @param path scenario file path reference
     */
    public PathFileScenarioSource(Path path) {
        this.path = path;
    }

    /**
     * Returns the canonical source type identifier.
     *
     * @return {@value #TYPE}
     */
    @Override
    public String getType() {
        return TYPE;
    }

    /**
     * Returns the raw path string used as scenario reference.
     *
     * @return path string representation
     */
    @Override
    public String getReference() {
        return path.toString();
    }

    /**
     * Returns an empty payload map.
     * <p>
     * Parsing is intentionally deferred to plugin-specific loaders that have
     * format context.
     * </p>
     *
     * @return an immutable empty map
     */
    @Override
    public Map<String, Object> load() {
        return Map.of();
    }
}
