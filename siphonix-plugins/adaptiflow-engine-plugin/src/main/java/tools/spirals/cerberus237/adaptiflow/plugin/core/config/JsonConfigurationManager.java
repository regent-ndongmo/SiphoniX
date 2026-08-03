package tools.spirals.cerberus237.adaptiflow.plugin.core.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * JSON-backed configuration manager for scenarios.
 * <p>
 * JSON is parsed through SnakeYAML because JSON is a YAML subset. The resulting structure is
 * validated and mapped by {@link ScenarioConfigurationManager}.
 * </p>
 *
 * @author Arléon Zemtsop (Cerberus)
 */
public class JsonConfigurationManager extends ScenarioConfigurationManager implements ConfigurationManager {

    /**
     * Loads JSON scenario configuration from disk.
     *
     * @param path JSON file path
     * @return parsed configuration; empty model when file does not exist
     * @throws IOException when file cannot be read
     */
    @Override
    public AdaptiflowConfiguration load(Path path) throws IOException {
        if (!Files.exists(path)) {
            return new AdaptiflowConfiguration();
        }

        // SnakeYAML can parse JSON because JSON is a subset of YAML.
        Yaml yaml = new Yaml();
        try (InputStream inputStream = Files.newInputStream(path)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> root = yaml.loadAs(inputStream, Map.class);
            return loadFromMap(root == null ? new LinkedHashMap<>() : root);
        }
    }

    /**
     * Saves configuration to disk using block-style YAML writer compatible with JSON-like objects.
     *
     * @param path destination file path
     * @param configuration configuration model to persist
     * @throws IOException when file cannot be written
     */
    @Override
    public void save(Path path, AdaptiflowConfiguration configuration) throws IOException {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);

        Yaml yaml = new Yaml(options);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        try (Writer writer = Files.newBufferedWriter(path)) {
            yaml.dump(toMap(configuration), writer);
        }
    }
}