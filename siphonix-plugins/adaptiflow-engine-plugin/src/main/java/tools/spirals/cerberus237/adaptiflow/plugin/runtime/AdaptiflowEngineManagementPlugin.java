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
import tools.spirals.cerberus237.adaptiflow.plugin.core.config.ConfigurationManager;
import tools.spirals.cerberus237.adaptiflow.plugin.core.config.JsonConfigurationManager;
import tools.spirals.cerberus237.adaptiflow.plugin.core.config.ScenarioConfigurationManager;
import tools.spirals.cerberus237.adaptiflow.plugin.core.config.XmlConfigurationManager;
import tools.spirals.cerberus237.adaptiflow.plugin.core.config.YamlConfigurationManager;
import tools.spirals.cerberus237.adaptiflow.plugin.core.scenarios.ScenarioDefinition;

/**
 * Scenario management plugin implementation for AdaptiFlow.
 * <p>
 * This plugin stores scenario definitions, materializes per-scenario runtime plugins, and exposes
 * service/REST/CLI control planes through the {@link ScenarioManagementPlugin} contract.
 * </p>
 * <p>
 * Source-format awareness (YAML/JSON/XML/path) is preserved so updates continue using the most
 * appropriate configuration manager.
 * </p>
 *
 * @author Arléon Zemtsop (Cerberus)
 */
public class AdaptiflowEngineManagementPlugin implements ScenarioManagementPlugin {

    private static final String YAML_FILE_SOURCE_TYPE = "yaml-file";
    private static final String JSON_FILE_SOURCE_TYPE = "json-file";
    private static final String XML_FILE_SOURCE_TYPE = "xml-file";
    private static final String PATH_FILE_SOURCE_TYPE = "path-file";

    private final Map<String, ScenarioDefinition> scenarios = new LinkedHashMap<>();
    private final ScenarioRuntimeFactory runtimeFactory = new ScenarioRuntimeFactoryImpl();
    private final Map<String, String> scenarioSourceTypes = new LinkedHashMap<>();
    private final Map<String, ScenarioPlugin> runtimePlugins = new LinkedHashMap<>();

    private final ScenarioManagementService service = new ServiceImpl();
    private final ScenarioManagementRestApi restApi = new RestImpl();
    private final ScenarioManagementCli cli = new CliImpl();

    private final ScenarioConfigurationManager scenarioConfigurationManager = new ScenarioConfigurationManager();
    private final Map<String, ConfigurationManager> configurationManagers = createConfigurationManagers();

    private PluginState state = PluginState.CREATED;
    private PluginContext pluginContext;

    /**
     * @return unique plugin id for scenario management runtime
     */
    @Override
    public synchronized String getId() {
        return "adaptiflow.engine";
    }

    /**
     * @return implementation version
     */
    @Override
    public synchronized String getVersion() {
        return "1.0.0";
    }

    /**
     * @return current plugin lifecycle state
     */
    @Override
    public synchronized PluginState getState() {
        return state;
    }

    /**
     * Initializes all already-registered scenarios and prepares runtime materialization.
     *
     * @param context plugin context
     */
    @Override
    public synchronized void initialize(PluginContext context) {
        pluginContext = context;
        for (ScenarioDefinition scenario : scenarios.values()) {
            ensureMaterialized(scenario);
        }
        state = PluginState.INITIALIZED;
    }

    /**
     * Starts every materialized scenario runtime plugin.
     */
    @Override
    public synchronized void start() {
        for (ScenarioPlugin plugin : runtimePlugins.values()) {
            if (plugin.getState() != PluginState.RUNNING) {
                plugin.start();
            }
        }
        state = PluginState.RUNNING;
    }

    /**
     * Stops every materialized scenario runtime plugin.
     */
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

    /**
     * Inserts or updates scenarios from one configuration payload.
     *
     * @param configuration parsed configuration payload
     * @param failOnDuplicate whether duplicates should be rejected
     */
    private synchronized void upsertScenarios(AdaptiflowConfiguration configuration, boolean failOnDuplicate) {
        upsertScenarios(configuration, failOnDuplicate, YAML_FILE_SOURCE_TYPE);
    }

    /**
     * Inserts or updates scenarios from one configuration payload while preserving source type.
     *
     * @param configuration parsed configuration payload
     * @param failOnDuplicate whether duplicates should be rejected
     * @param sourceType source type tag used to choose runtime wrapper and manager
     */
    private synchronized void upsertScenarios(AdaptiflowConfiguration configuration, boolean failOnDuplicate,
            String sourceType) {
        for (Map.Entry<String, ScenarioDefinition> entry : configuration.getScenarios().entrySet()) {
            String scenarioId = entry.getKey();
            if (failOnDuplicate && scenarios.containsKey(scenarioId)) {
                throw new IllegalArgumentException("Scenario already exists: " + scenarioId);
            }

            ScenarioDefinition definition = entry.getValue();
            scenarios.put(scenarioId, definition);
            scenarioSourceTypes.put(scenarioId, normalizeSourceType(sourceType));
            ensureMaterialized(definition);
        }
    }

    /**
     * Rebuilds one scenario runtime plugin according to its latest definition and source type.
     *
     * @param scenario scenario definition to materialize
     */
    private void ensureMaterialized(ScenarioDefinition scenario) {
        String scenarioId = scenario.getId();

        ScenarioPlugin previous = runtimePlugins.remove(scenarioId);
        if (previous != null && previous.getState() == PluginState.RUNNING) {
            previous.stop();
        }

        if (!scenario.isEnabled()) {
            return;
        }

        String sourceType = scenarioSourceTypes.getOrDefault(scenarioId, YAML_FILE_SOURCE_TYPE);
        ScenarioPlugin plugin;
        if (XML_FILE_SOURCE_TYPE.equals(sourceType)) {
            plugin = new XmlScenarioPlugin(scenario);
        } else if (YAML_FILE_SOURCE_TYPE.equals(sourceType)) {
            plugin = new YamlScenarioPlugin(scenario);
        } else {
            plugin = new ScenarioPlugin(scenario, runtimeFactory);
        }

        if (pluginContext != null) {
            plugin.initialize(pluginContext);
            if (state == PluginState.RUNNING) {
                plugin.start();
            }
        }

        runtimePlugins.put(scenarioId, plugin);
    }

    /**
     * Loads configuration using source metadata and available managers.
     *
     * @param source scenario source payload
     * @return parsed configuration
     * @throws IOException when source cannot be read
     */
    private synchronized AdaptiflowConfiguration loadConfiguration(ScenarioSource source) throws IOException {
        if (isFileSource(source) && source.getReference() != null) {
            Path sourcePath = Path.of(source.getReference());
            if (Files.exists(sourcePath)) {
                return managerForSourceType(resolveSourceType(source)).load(sourcePath);
            }
        }
        return scenarioConfigurationManager.loadFromMap(source.load());
    }

    private String resolveSourceType(ScenarioSource source) {
        String sourceType = source.getType();
        if (XML_FILE_SOURCE_TYPE.equals(sourceType)
                || JSON_FILE_SOURCE_TYPE.equals(sourceType)
                || YAML_FILE_SOURCE_TYPE.equals(sourceType)) {
            return sourceType;
        }

        if (source.getReference() != null && !source.getReference().trim().isEmpty()) {
            return detectSourceType(Path.of(source.getReference()));
        }

        return normalizeSourceType(sourceType);
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
            upsertScenarios(loadConfiguration(source), true, resolveSourceType(source));
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
            scenarioSourceTypes.put(scenarioId, normalizeSourceType(resolveSourceType(source)));
            ensureMaterialized(replacement);
        }

        @Override
        public synchronized void deleteScenario(String scenarioId) {
            requireScenario(scenarioId);
            scenarios.remove(scenarioId);
            scenarioSourceTypes.remove(scenarioId);
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
        return YAML_FILE_SOURCE_TYPE.equals(source.getType())
                || JSON_FILE_SOURCE_TYPE.equals(source.getType())
                || XML_FILE_SOURCE_TYPE.equals(source.getType())
                || PATH_FILE_SOURCE_TYPE.equals(source.getType());
    }

    private String detectSourceType(Path path) {
        String fileName = path.getFileName() == null ? "" : path.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".json")) {
            return JSON_FILE_SOURCE_TYPE;
        }
        if (fileName.endsWith(".xml")) {
            return XML_FILE_SOURCE_TYPE;
        }
        return YAML_FILE_SOURCE_TYPE;
    }

    private String normalizeSourceType(String sourceType) {
        if (XML_FILE_SOURCE_TYPE.equals(sourceType)) {
            return XML_FILE_SOURCE_TYPE;
        }
        if (JSON_FILE_SOURCE_TYPE.equals(sourceType)) {
            return JSON_FILE_SOURCE_TYPE;
        }
        return YAML_FILE_SOURCE_TYPE;
    }

    private Map<String, ConfigurationManager> createConfigurationManagers() {
        Map<String, ConfigurationManager> managers = new LinkedHashMap<>();
        managers.put(YAML_FILE_SOURCE_TYPE, new YamlConfigurationManager());
        managers.put(JSON_FILE_SOURCE_TYPE, new JsonConfigurationManager());
        managers.put(XML_FILE_SOURCE_TYPE, new XmlConfigurationManager());
        return managers;
    }

    private ConfigurationManager managerForSourceType(String sourceType) {
        String normalizedType = normalizeSourceType(sourceType);
        return configurationManagers.get(normalizedType);
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
