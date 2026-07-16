package tools.spirals.cerberus237.siphonix.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import tools.spirals.cerberus237.siphonix.scenarios.ActionDefinition;
import tools.spirals.cerberus237.siphonix.scenarios.ConditionalEvaluatorDefinition;
import tools.spirals.cerberus237.siphonix.scenarios.EventDefinition;
import tools.spirals.cerberus237.siphonix.scenarios.MetricCollectorDefinition;
import tools.spirals.cerberus237.siphonix.scenarios.ObservationSchedulerDefinition;
import tools.spirals.cerberus237.siphonix.scenarios.ScenarioDefinition;

/**
 * YAML-backed configuration manager for scenarios.
 */
public class YamlConfigurationManager implements ConfigurationManager {

    private static final String DEFAULT_EVENT_TYPE = "ConditionalEvent";

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

        scenario.setEvents(mapEvents(scenarioId, scenarioMap));
        scenario.setScheduler(mapScheduler(scenarioMap));

        validateScenario(scenario);
        return scenario;
    }

    private ObservationSchedulerDefinition mapScheduler(Map<String, Object> scenarioMap) {
        Object rawScheduler = scenarioMap.get("scheduler");
        if (rawScheduler == null) {
            return null;
        }
        if (!(rawScheduler instanceof Map)) {
            throw new InvalidConfigurationException("scheduler field must be an object");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> schedulerMap = (Map<String, Object>) rawScheduler;

        ObservationSchedulerDefinition scheduler = new ObservationSchedulerDefinition();
        scheduler.setType(stringValue(schedulerMap.get("type")));
        scheduler.setParameters(mapValue(schedulerMap.get("parameters")));
        return scheduler;
    }

    private List<EventDefinition> mapEvents(String scenarioId, Map<String, Object> scenarioMap) {
        Object rawEvents = scenarioMap.get("events");
        if (!(rawEvents instanceof List)) {
            throw new InvalidConfigurationException(
                    "Scenario '" + scenarioId + "' must define an events list");
        }

        List<?> eventsList = (List<?>) rawEvents;
        List<EventDefinition> events = new ArrayList<>();
        for (Object eventItem : eventsList) {
            if (!(eventItem instanceof Map)) {
                throw new InvalidConfigurationException(
                        "Scenario '" + scenarioId + "' contains an invalid event entry");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> eventMap = (Map<String, Object>) eventItem;
            events.add(mapEvent(scenarioId, eventMap));
        }
        return events;
    }

    private EventDefinition mapEvent(String scenarioId, Map<String, Object> eventMap) {
        EventDefinition event = new EventDefinition();
        event.setId(stringValue(eventMap.get("id")));
        event.setType(stringValue(eventMap.get("type")));
        event.setParameters(mapValue(eventMap.get("parameters")));
        event.setCollector(mapCollector(scenarioId, eventMap));
        event.setEvaluators(mapEvaluators(scenarioId, eventMap));
        event.setActions(mapActions(scenarioId, eventMap));
        return event;
    }

    private MetricCollectorDefinition mapCollector(String scenarioId, Map<String, Object> eventMap) {
        if (!requiresConditionalArtifacts(eventMap)) {
            return null;
        }
        Object rawCollector = eventMap.get("collector");
        if (!(rawCollector instanceof Map)) {
            throw new InvalidConfigurationException(
                    "Scenario '" + scenarioId + "' must define a collector object for each event");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> collectorMap = (Map<String, Object>) rawCollector;
        MetricCollectorDefinition collector = new MetricCollectorDefinition();
        collector.setType(stringValue(collectorMap.get("type")));
        collector.setParameters(mapValue(collectorMap.get("parameters")));
        return collector;
    }

    private List<ConditionalEvaluatorDefinition> mapEvaluators(String scenarioId, Map<String, Object> eventMap) {
        if (!requiresConditionalArtifacts(eventMap)) {
            return new ArrayList<>();
        }
        Object rawEvaluators = eventMap.get("evaluators");
        if (!(rawEvaluators instanceof List)) {
            throw new InvalidConfigurationException(
                    "Scenario '" + scenarioId + "' must define an evaluators list for each event");
        }

        List<?> evaluatorItems = (List<?>) rawEvaluators;
        List<ConditionalEvaluatorDefinition> evaluators = new ArrayList<>();
        for (Object evaluatorItem : evaluatorItems) {
            if (!(evaluatorItem instanceof Map)) {
                throw new InvalidConfigurationException(
                        "Scenario '" + scenarioId + "' contains an invalid evaluator entry");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> evaluatorMap = (Map<String, Object>) evaluatorItem;

            ConditionalEvaluatorDefinition evaluator = new ConditionalEvaluatorDefinition();
            evaluator.setType(stringValue(evaluatorMap.get("type")));
            evaluator.setParameters(mapValue(evaluatorMap.get("parameters")));
            evaluators.add(evaluator);
        }
        return evaluators;
    }

    private List<ActionDefinition> mapActions(String scenarioId, Map<String, Object> eventMap) {
        Object rawActions = eventMap.get("actions");
        if (!(rawActions instanceof List)) {
            throw new InvalidConfigurationException(
                    "Scenario '" + scenarioId + "' must define an actions list for each event");
        }

        List<?> actionItems = (List<?>) rawActions;
        List<ActionDefinition> actions = new ArrayList<>();
        for (Object actionItem : actionItems) {
            if (!(actionItem instanceof Map)) {
                throw new InvalidConfigurationException(
                        "Scenario '" + scenarioId + "' contains an invalid action entry");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> actionMap = (Map<String, Object>) actionItem;

            ActionDefinition action = new ActionDefinition();
            action.setType(stringValue(actionMap.get("type")));
            action.setParameters(mapValue(actionMap.get("parameters")));
            actions.add(action);
        }
        return actions;
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
            scenarioMap.put("events", toEventMapList(scenario.getEvents()));
            if (scenario.getScheduler() != null) {
                Map<String, Object> schedulerMap = new LinkedHashMap<>();
                schedulerMap.put("type", scenario.getScheduler().getType());
                schedulerMap.put("parameters", scenario.getScheduler().getParameters());
                scenarioMap.put("scheduler", schedulerMap);
            }
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
        if (scenario.getEvents() == null || scenario.getEvents().isEmpty()) {
            throw new InvalidConfigurationException(
                    "Scenario '" + scenario.getId() + "' must define at least one event");
        }
        if (scenario.getScheduler() != null
                && (scenario.getScheduler().getType() == null || scenario.getScheduler().getType().trim().isEmpty())) {
            throw new InvalidConfigurationException(
                    "Scenario '" + scenario.getId() + "' scheduler must define a non-empty type");
        }
        for (EventDefinition event : scenario.getEvents()) {
            validateEvent(scenario.getId(), event);
        }
    }

    private void validateEvent(String scenarioId, EventDefinition event) {
        if (event.getId() == null || event.getId().trim().isEmpty()) {
            throw new InvalidConfigurationException(
                    "Scenario '" + scenarioId + "' has an event with missing id");
        }

        if (isConditionalEventType(event.getType())) {
            MetricCollectorDefinition collector = event.getCollector();
            if (collector == null || collector.getType() == null || collector.getType().trim().isEmpty()) {
                throw new InvalidConfigurationException(
                        "Scenario '" + scenarioId + "' event '" + event.getId() + "' must define a collector type");
            }

            if (event.getEvaluators() == null || event.getEvaluators().isEmpty()) {
                throw new InvalidConfigurationException(
                        "Scenario '" + scenarioId + "' event '" + event.getId() + "' must define at least one evaluator");
            }
            for (ConditionalEvaluatorDefinition evaluator : event.getEvaluators()) {
                if (evaluator.getType() == null || evaluator.getType().trim().isEmpty()) {
                    throw new InvalidConfigurationException(
                            "Scenario '" + scenarioId + "' event '" + event.getId() + "' has an evaluator with missing type");
                }
            }
        }

        if (event.getActions() == null || event.getActions().isEmpty()) {
            throw new InvalidConfigurationException(
                    "Scenario '" + scenarioId + "' event '" + event.getId() + "' must define at least one action");
        }
        for (ActionDefinition action : event.getActions()) {
            if (action.getType() == null || action.getType().trim().isEmpty()) {
                throw new InvalidConfigurationException(
                        "Scenario '" + scenarioId + "' event '" + event.getId() + "' has an action with missing type");
            }
        }
    }

    private List<Map<String, Object>> toEventMapList(List<EventDefinition> events) {
        List<Map<String, Object>> rawEvents = new ArrayList<>();
        for (EventDefinition event : events) {
            Map<String, Object> eventMap = new LinkedHashMap<>();
            eventMap.put("id", event.getId());
            if (event.getType() != null && !event.getType().trim().isEmpty()) {
                eventMap.put("type", event.getType());
            }
            if (event.getParameters() != null && !event.getParameters().isEmpty()) {
                eventMap.put("parameters", event.getParameters());
            }

            if (event.getCollector() != null) {
                Map<String, Object> collectorMap = new LinkedHashMap<>();
                collectorMap.put("type", event.getCollector().getType());
                collectorMap.put("parameters", event.getCollector().getParameters());
                eventMap.put("collector", collectorMap);
            }

            if (event.getEvaluators() != null && !event.getEvaluators().isEmpty()) {
                List<Map<String, Object>> evaluatorMaps = new ArrayList<>();
                for (ConditionalEvaluatorDefinition evaluator : event.getEvaluators()) {
                    Map<String, Object> evaluatorMap = new LinkedHashMap<>();
                    evaluatorMap.put("type", evaluator.getType());
                    evaluatorMap.put("parameters", evaluator.getParameters());
                    evaluatorMaps.add(evaluatorMap);
                }
                eventMap.put("evaluators", evaluatorMaps);
            }

            List<Map<String, Object>> actionMaps = new ArrayList<>();
            for (ActionDefinition action : event.getActions()) {
                Map<String, Object> actionMap = new LinkedHashMap<>();
                actionMap.put("type", action.getType());
                actionMap.put("parameters", action.getParameters());
                actionMaps.add(actionMap);
            }
            eventMap.put("actions", actionMaps);

            rawEvents.add(eventMap);
        }
        return rawEvents;
    }

    private boolean requiresConditionalArtifacts(Map<String, Object> eventMap) {
        String type = stringValue(eventMap.get("type"));
        return isConditionalEventType(type);
    }

    private boolean isConditionalEventType(String type) {
        if (type == null || type.trim().isEmpty()) {
            return true;
        }
        String trimmed = type.trim();
        return DEFAULT_EVENT_TYPE.equals(trimmed)
                || "tools.spirals.cerberus237.adaptiflow.events.ConditionalEvent".equals(trimmed);
    }

    private Map<String, Object> mapValue(Object value) {
        if (value == null) {
            return new LinkedHashMap<>();
        }
        if (!(value instanceof Map)) {
            throw new InvalidConfigurationException("Expected a map for parameters field");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> rawMap = (Map<String, Object>) value;
        return rawMap;
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
