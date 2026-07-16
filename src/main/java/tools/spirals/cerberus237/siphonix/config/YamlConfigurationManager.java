package tools.spirals.cerberus237.siphonix.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import tools.spirals.cerberus237.siphonix.scenarios.ScenarioDefinition;

/**
 * YAML-backed configuration manager for scenarios.
 */
public class YamlConfigurationManager implements ConfigurationManager {

    @Override
    public SiphonixConfiguration load(Path path) throws IOException {
        if (!Files.exists(path)) {
            return new SiphonixConfiguration();
        }

        Yaml yaml = new Yaml();
        try (InputStream inputStream = Files.newInputStream(path)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> root = yaml.loadAs(inputStream, Map.class);
            return fromMap(root == null ? new LinkedHashMap<>() : root);
        }
    }

    @Override
    public void save(Path path, SiphonixConfiguration configuration) throws IOException {
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

    private SiphonixConfiguration fromMap(Map<String, Object> root) {
        SiphonixConfiguration configuration = new SiphonixConfiguration();

        @SuppressWarnings("unchecked")
        Map<String, Object> rawScenarios = (Map<String, Object>) root.getOrDefault("scenarios", new LinkedHashMap<>());

        Map<String, ScenarioDefinition> scenarios = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : rawScenarios.entrySet()) {
            String scenarioId = entry.getKey();
            @SuppressWarnings("unchecked")
            Map<String, Object> scenarioMap = (Map<String, Object>) entry.getValue();
            ScenarioDefinition definition = mapToScenario(scenarioId, scenarioMap);
            scenarios.put(scenarioId, definition);
        }

        configuration.setScenarios(scenarios);
        return configuration;
    }

    private ScenarioDefinition mapToScenario(String scenarioId, Map<String, Object> scenarioMap) {
        ScenarioDefinition scenario = new ScenarioDefinition();
        scenario.setId(scenarioId);
        scenario.setPluginId(stringValue(scenarioMap.get("pluginId")));
        scenario.setEnabled(booleanValue(scenarioMap.get("enabled"), true));
        scenario.setIntervalMs(intValue(scenarioMap.get("intervalMs"), 5000));

        @SuppressWarnings("unchecked")
        Map<String, Object> thresholds = (Map<String, Object>) scenarioMap.getOrDefault("thresholds", new LinkedHashMap<>());
        scenario.setThresholds(thresholds);

        @SuppressWarnings("unchecked")
        List<String> actions = (List<String>) scenarioMap.getOrDefault("actions", List.of());
        scenario.setActions(actions);

        validateScenario(scenario);
        return scenario;
    }

    private Map<String, Object> toMap(SiphonixConfiguration configuration) {
        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> rawScenarios = new LinkedHashMap<>();

        for (Map.Entry<String, ScenarioDefinition> entry : configuration.getScenarios().entrySet()) {
            ScenarioDefinition scenario = entry.getValue();
            validateScenario(scenario);

            Map<String, Object> scenarioMap = new LinkedHashMap<>();
            scenarioMap.put("pluginId", scenario.getPluginId());
            scenarioMap.put("enabled", scenario.isEnabled());
            scenarioMap.put("intervalMs", scenario.getIntervalMs());
            scenarioMap.put("thresholds", scenario.getThresholds());
            scenarioMap.put("actions", scenario.getActions());
            rawScenarios.put(entry.getKey(), scenarioMap);
        }

        root.put("scenarios", rawScenarios);
        return root;
    }

    private void validateScenario(ScenarioDefinition scenario) {
        if (scenario.getPluginId() == null || scenario.getPluginId().trim().isEmpty()) {
            throw new InvalidConfigurationException(
                    "Scenario '" + scenario.getId() + "' must define a non-empty pluginId");
        }
        if (scenario.getIntervalMs() <= 0) {
            throw new InvalidConfigurationException(
                    "Scenario '" + scenario.getId() + "' must define a strictly positive intervalMs");
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private boolean booleanValue(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private int intValue(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return Integer.parseInt(String.valueOf(value));
    }
}
