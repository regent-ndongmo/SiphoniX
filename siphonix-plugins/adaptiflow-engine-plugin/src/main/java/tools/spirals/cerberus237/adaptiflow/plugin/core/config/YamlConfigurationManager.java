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
 * YAML-backed configuration manager for scenarios.
 */
public class YamlConfigurationManager extends ScenarioConfigurationManager implements ConfigurationManager {

    @Override
    public AdaptiflowConfiguration load(Path path) throws IOException {
        if (!Files.exists(path)) {
            return new AdaptiflowConfiguration();
        }

        Yaml yaml = new Yaml();
        try (InputStream inputStream = Files.newInputStream(path)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> root = yaml.loadAs(inputStream, Map.class);
            return loadFromMap(root == null ? new LinkedHashMap<>() : root);
        }
    }

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
