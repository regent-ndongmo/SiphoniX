package tools.spirals.cerberus237.adaptiflow.plugin.runtime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tools.spirals.cerberus237.siphonix.api.plugin.PluginContext;
import tools.spirals.cerberus237.siphonix.api.plugin.PluginState;
import tools.spirals.cerberus237.siphonix.api.plugin.management.ScenarioManagementCli;
import tools.spirals.cerberus237.siphonix.api.plugin.management.ScenarioManagementPlugin;
import tools.spirals.cerberus237.siphonix.api.plugin.management.ScenarioManagementRestApi;
import tools.spirals.cerberus237.siphonix.api.plugin.management.ScenarioManagementService;
import tools.spirals.cerberus237.siphonix.api.plugin.management.ScenarioSource;
import tools.spirals.cerberus237.siphonix.api.plugin.management.ScenarioValidationResult;
import tools.spirals.cerberus237.adaptiflow.plugin.core.config.AdaptiflowConfiguration;
import tools.spirals.cerberus237.adaptiflow.plugin.core.config.YamlConfigurationManager;
import tools.spirals.cerberus237.adaptiflow.plugin.core.scenarios.ScenarioDefinition;

public class AdaptiflowEngineManagementPlugin implements ScenarioManagementPlugin {

    private static final String YAML_FILE_SOURCE_TYPE = "yaml-file";
    private static final String JSON_FILE_SOURCE_TYPE = "json-file";

    private final Map<String, ScenarioDefinition> scenarios = new LinkedHashMap<>();
    private final ScenarioRuntimeFactory runtimeFactory = new YamlScenarioRuntimeFactory();
    private final Map<String, ScenarioPlugin> runtimePlugins = new LinkedHashMap<>();

    private final ScenarioManagementService service = new ServiceImpl();
    private final ScenarioManagementRestApi restApi = new RestImpl();
    private final ScenarioManagementCli cli = new CliImpl();

    private final YamlConfigurationManager configurationManager = new YamlConfigurationManager();

    private PluginState state = PluginState.CREATED;
    private PluginContext pluginContext;

    @Override
    public synchronized String getId() {
        return "adaptiflow.engine";
    }

    @Override
    public synchronized String getVersion() {
        return "1.0.0";
    }

    @Override
    public synchronized PluginState getState() {
        return state;
    }

    @Override
    public synchronized void initialize(PluginContext context) {
        pluginContext = context;
        for (ScenarioDefinition scenario : scenarios.values()) {
            ensureMaterialized(scenario);
        }
        state = PluginState.INITIALIZED;
    }

    @Override
    public synchronized void start() {
        for (ScenarioPlugin plugin : runtimePlugins.values()) {
            if (plugin.getState() != PluginState.RUNNING) {
                plugin.start();
            }
        }
        state = PluginState.RUNNING;
    }

    @Override
    public synchronized void stop() {
        for (ScenarioPlugin plugin : runtimePlugins.values()) {
            plugin.stop();
        }
        state = PluginState.STOPPED;
    }

    @Override
    public ScenarioManagementService getScenarioManagementService() {
        return service;
    }

    @Override
    public ScenarioManagementRestApi getScenarioManagementRestApi() {
        return restApi;
    }

    @Override
    public ScenarioManagementCli getScenarioManagementCli() {
        return cli;
    }

    private synchronized void upsertScenarios(AdaptiflowConfiguration configuration, boolean failOnDuplicate) {
        for (Map.Entry<String, ScenarioDefinition> entry : configuration.getScenarios().entrySet()) {
            String scenarioId = entry.getKey();
            if (failOnDuplicate && scenarios.containsKey(scenarioId)) {
                throw new IllegalArgumentException("Scenario already exists: " + scenarioId);
            }

            ScenarioDefinition definition = entry.getValue();
            scenarios.put(scenarioId, definition);
            ensureMaterialized(definition);
        }
    }

    private void ensureMaterialized(ScenarioDefinition scenario) {
        String scenarioId = scenario.getId();

        ScenarioPlugin previous = runtimePlugins.remove(scenarioId);
        if (previous != null && previous.getState() == PluginState.RUNNING) {
            previous.stop();
        }

        if (!scenario.isEnabled()) {
            return;
        }

        ScenarioPlugin plugin = new ScenarioPlugin(scenario, runtimeFactory);
        if (pluginContext != null) {
            plugin.initialize(pluginContext);
            if (state == PluginState.RUNNING) {
                plugin.start();
            }
        }

        runtimePlugins.put(scenarioId, plugin);
    }

    private synchronized AdaptiflowConfiguration loadConfiguration(ScenarioSource source) throws IOException {
        if (isFileSource(source) && source.getReference() != null) {
            Path sourcePath = Path.of(source.getReference());
            if (Files.exists(sourcePath)) {
                return configurationManager.load(sourcePath);
            }
        }
        return configurationManager.loadFromMap(source.load());
    }

    private synchronized ScenarioDefinition requireScenario(String scenarioId) {
        ScenarioDefinition definition = scenarios.get(scenarioId);
        if (definition == null) {
            throw new IllegalArgumentException("Scenario not found: " + scenarioId);
        }
        return definition;
    }

    private final class ServiceImpl implements ScenarioManagementService {

        @Override
        public synchronized void createScenario(ScenarioSource source) throws IOException {
            upsertScenarios(loadConfiguration(source), true);
        }

        @Override
        public synchronized void updateScenario(String scenarioId, ScenarioSource source) throws IOException {
            if (!scenarios.containsKey(scenarioId)) {
                throw new IllegalArgumentException("Scenario not found: " + scenarioId);
            }

            AdaptiflowConfiguration configuration = loadConfiguration(source);
            ScenarioDefinition replacement = configuration.getScenarios().get(scenarioId);
            if (replacement == null) {
                throw new IllegalArgumentException("Scenario id not found in source: " + scenarioId);
            }

            scenarios.put(scenarioId, replacement);
            ensureMaterialized(replacement);
        }

        @Override
        public synchronized void deleteScenario(String scenarioId) {
            requireScenario(scenarioId);
            scenarios.remove(scenarioId);
            ScenarioPlugin plugin = runtimePlugins.remove(scenarioId);
            if (plugin != null) {
                plugin.stop();
            }
        }

        @Override
        public synchronized void enableScenario(String scenarioId) {
            ScenarioDefinition definition = requireScenario(scenarioId);
            definition.setEnabled(true);
            ensureMaterialized(definition);
        }

        @Override
        public synchronized void disableScenario(String scenarioId) {
            ScenarioDefinition definition = requireScenario(scenarioId);
            definition.setEnabled(false);
            ensureMaterialized(definition);
        }

        @Override
        public synchronized List<String> listScenarios() {
            return new ArrayList<>(scenarios.keySet());
        }

        @Override
        public synchronized ScenarioValidationResult validateScenario(ScenarioSource source) throws IOException {
            try {
                loadConfiguration(source);
                return ScenarioValidationResult.success();
            } catch (RuntimeException ex) {
                return ScenarioValidationResult.failure(List.of(ex.getMessage()));
            }
        }
    }

    private final class RestImpl implements ScenarioManagementRestApi {

        @Override
        public void postScenario(ScenarioSource source) throws IOException {
            service.createScenario(source);
        }

        @Override
        public void putScenario(String scenarioId, ScenarioSource source) throws IOException {
            service.updateScenario(scenarioId, source);
        }

        @Override
        public void deleteScenario(String scenarioId) {
            service.deleteScenario(scenarioId);
        }

        @Override
        public void enableScenario(String scenarioId) {
            service.enableScenario(scenarioId);
        }

        @Override
        public void disableScenario(String scenarioId) {
            service.disableScenario(scenarioId);
        }
    }

    private final class CliImpl implements ScenarioManagementCli {

        @Override
        public void applyScenario(String sourcePath) throws IOException {
            Path path = Path.of(sourcePath);
            service.createScenario(new PathScenarioSource(path, detectSourceType(path)));
        }

        @Override
        public void updateScenario(String scenarioId, String sourcePath) throws IOException {
            Path path = Path.of(sourcePath);
            service.updateScenario(scenarioId, new PathScenarioSource(path, detectSourceType(path)));
        }

        @Override
        public void disableScenario(String scenarioId) {
            service.disableScenario(scenarioId);
        }
    }

    private boolean isFileSource(ScenarioSource source) {
        return YAML_FILE_SOURCE_TYPE.equals(source.getType()) || JSON_FILE_SOURCE_TYPE.equals(source.getType());
    }

    private String detectSourceType(Path path) {
        String fileName = path.getFileName() == null ? "" : path.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".json")) {
            return JSON_FILE_SOURCE_TYPE;
        }
        return YAML_FILE_SOURCE_TYPE;
    }

    private static final class PathScenarioSource implements ScenarioSource {

        private final Path path;
        private final String type;

        private PathScenarioSource(Path path, String type) {
            this.path = path;
            this.type = type;
        }

        @Override
        public String getType() {
            return type;
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
}
