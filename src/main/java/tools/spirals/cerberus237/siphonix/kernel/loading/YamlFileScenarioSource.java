package tools.spirals.cerberus237.siphonix.kernel.loading;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import tools.spirals.cerberus237.siphonix.api.plugin.management.ScenarioSource;

public class YamlFileScenarioSource implements ScenarioSource {

    private final Path path;

    public YamlFileScenarioSource(Path path) {
        this.path = path;
    }

    @Override
    public String getType() {
        return "yaml-file";
    }

    @Override
    public String getReference() {
        return path.toString();
    }

    @Override
    public Map<String, Object> load() throws IOException {
        if (!Files.exists(path)) {
            return new LinkedHashMap<>();
        }

        Yaml yaml = new Yaml();
        try (InputStream inputStream = Files.newInputStream(path)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> root = yaml.loadAs(inputStream, Map.class);
            return root == null ? new LinkedHashMap<>() : root;
        }
    }
}
