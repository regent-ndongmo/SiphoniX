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
 *
 * @author Arléon Zemtsop (Cerberus)
 */
public class PathFileScenarioSource implements ScenarioSource {

    public static final String TYPE = "path-file";

    private final Path path;

    public PathFileScenarioSource(Path path) {
        this.path = path;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String getReference() {
        return path.toString();
    }

    @Override
    public Map<String, Object> load() {
        return Map.of();
    }
}
