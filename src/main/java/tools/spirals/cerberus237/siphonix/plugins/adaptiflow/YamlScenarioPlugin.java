package tools.spirals.cerberus237.siphonix.plugins.adaptiflow;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import tools.spirals.cerberus237.adaptationactionsbase.core.IAdaptationAction;
import tools.spirals.cerberus237.adaptiflow.events.ConditionalEvent;
import tools.spirals.cerberus237.adaptiflow.events.Event;
import tools.spirals.cerberus237.adaptiflow.interfaces.ConditionEvaluator;
import tools.spirals.cerberus237.adaptiflow.interfaces.Observer;
import tools.spirals.cerberus237.adaptiflow.subscriptions.subscribers.EventSubscriber;
import tools.spirals.cerberus237.metricscollectorbase.IMetricsCollector;
import tools.spirals.cerberus237.siphonix.config.InvalidConfigurationException;
import tools.spirals.cerberus237.siphonix.kernel.ManagedSchedulerHandle;
import tools.spirals.cerberus237.siphonix.kernel.Plugin;
import tools.spirals.cerberus237.siphonix.kernel.PluginContext;
import tools.spirals.cerberus237.siphonix.kernel.PluginState;
import tools.spirals.cerberus237.siphonix.scenarios.ActionDefinition;
import tools.spirals.cerberus237.siphonix.scenarios.ConditionalEvaluatorDefinition;
import tools.spirals.cerberus237.siphonix.scenarios.EventDefinition;
import tools.spirals.cerberus237.siphonix.scenarios.MetricCollectorDefinition;
import tools.spirals.cerberus237.siphonix.scenarios.ObservationSchedulerDefinition;
import tools.spirals.cerberus237.siphonix.scenarios.ScenarioDefinition;

/**
 * Runtime plugin that materializes Adaptiflow events from YAML scenarios.
 */
public class YamlScenarioPlugin implements Plugin {

    private static final String CONSTRUCTOR_ARG_TYPES = "constructorArgTypes";
    private static final String CONSTRUCTOR_ARGS = "constructorArgs";
    private static final String FACTORY_METHOD = "factoryMethod";
    private static final String FACTORY_ARG_TYPES = "factoryArgTypes";
    private static final String FACTORY_ARGS = "factoryArgs";

            private static final String DEFAULT_SCHEDULER_CLASS =
                "tools.spirals.cerberus237.adaptiflow.subscriptions.ContinuousObservationScheduler";
                private static final String DEFAULT_EVENT_CLASS =
                    "tools.spirals.cerberus237.adaptiflow.events.ConditionalEvent";

    private static final List<String> COLLECTOR_PACKAGES = List.of(
            "tools.spirals.cerberus237.metricscollectorbase.metrics.cpu",
            "tools.spirals.cerberus237.metricscollectorbase.core",
            "tools.spirals.cerberus237.metricscollectorbase");

    private static final List<String> EVALUATOR_PACKAGES = List.of(
            "tools.spirals.cerberus237.adaptiflow.operators",
            "tools.spirals.cerberus237.adaptiflow.interfaces");

    private static final List<String> ACTION_PACKAGES = List.of(
            "tools.spirals.cerberus237.adaptationactionsbase.core",
            "tools.spirals.cerberus237.adaptationactionsbase.docker");

    private static final List<String> SCHEDULER_PACKAGES = List.of(
            "tools.spirals.cerberus237.adaptiflow.subscriptions");

        private static final List<String> EVENT_PACKAGES = List.of(
            "tools.spirals.cerberus237.adaptiflow.events");

    private final ScenarioDefinition scenario;

    private PluginState state = PluginState.CREATED;
    private ManagedSchedulerHandle schedulerHandle;

    public YamlScenarioPlugin(ScenarioDefinition scenario) {
        this.scenario = scenario;
    }

    @Override
    public String getId() {
        return "scenario." + scenario.getId();
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public PluginState getState() {
        return state;
    }

    @Override
    public void initialize(PluginContext context) {
        List<Event> events = new ArrayList<>();
        for (EventDefinition eventDefinition : scenario.getEvents()) {
            events.add(buildEvent(eventDefinition, context));
        }

        Object scheduler = createScheduler(events, scenario.getIntervalMs());
        schedulerHandle = new ManagedSchedulerHandle(scheduler);
        state = PluginState.INITIALIZED;
    }

    @Override
    public void start() {
        if (schedulerHandle == null) {
            throw new IllegalStateException("Plugin " + getId() + " is not initialized");
        }
        schedulerHandle.start();
        state = PluginState.RUNNING;
    }

    @Override
    public void stop() {
        if (schedulerHandle == null) {
            return;
        }
        schedulerHandle.stop();
        state = PluginState.STOPPED;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Event buildEvent(EventDefinition eventDefinition, PluginContext context) {
        Event event = createEvent(eventDefinition, context);

        List<IAdaptationAction> actions = createActions(eventDefinition.getActions(), context);
        List<Observer<Object>> subscribers = List.of(new EventSubscriber(actions));
        event.subscribeAll(subscribers);
        return event;
    }

    private Event createEvent(EventDefinition eventDefinition, PluginContext context) {
        ComponentSpec spec = resolveEventSpec(eventDefinition, context);
        return createComponent(spec.className, spec.parameters, Event.class);
    }

    private IMetricsCollector<?> createCollector(MetricCollectorDefinition definition, PluginContext context) {
        ComponentSpec spec = resolveCollectorSpec(definition);
        return createComponent(spec.className, spec.parameters, IMetricsCollector.class);
    }

    private ConditionEvaluator<?> createEvaluator(ConditionalEvaluatorDefinition definition) {
        ComponentSpec spec = resolveEvaluatorSpec(definition);
        return createComponent(spec.className, spec.parameters, ConditionEvaluator.class);
    }

    private List<IAdaptationAction> createActions(List<ActionDefinition> actionDefinitions, PluginContext context) {
        List<IAdaptationAction> actions = new ArrayList<>();
        for (ActionDefinition actionDefinition : actionDefinitions) {
            ComponentSpec spec = resolveActionSpec(actionDefinition);
            actions.add(createComponent(spec.className, spec.parameters, IAdaptationAction.class));
        }
        return actions;
    }

    private ComponentSpec resolveCollectorSpec(MetricCollectorDefinition definition) {
        String className = resolveClassName(definition.getType(), COLLECTOR_PACKAGES);
        Map<String, Object> parameters = cloneParameters(definition.getParameters());

        return new ComponentSpec(className, parameters);
    }

    private ComponentSpec resolveEvaluatorSpec(ConditionalEvaluatorDefinition definition) {
        String className = resolveClassName(definition.getType(), EVALUATOR_PACKAGES);
        Map<String, Object> parameters = cloneParameters(definition.getParameters());

        return new ComponentSpec(className, parameters);
    }

    private ComponentSpec resolveActionSpec(ActionDefinition definition) {
        String className = resolveClassName(definition.getType(), ACTION_PACKAGES);
        Map<String, Object> parameters = cloneParameters(definition.getParameters());

        return new ComponentSpec(className, parameters);
    }

    private ComponentSpec resolveEventSpec(EventDefinition definition, PluginContext context) {
        String configuredType = stringOrDefault(definition.getType(), DEFAULT_EVENT_CLASS);
        String className = resolveClassName(configuredType, EVENT_PACKAGES);
        Map<String, Object> parameters = cloneParameters(definition.getParameters());

        if (isConditionalEventClass(className)
                && !parameters.containsKey(CONSTRUCTOR_ARG_TYPES)
                && !parameters.containsKey(CONSTRUCTOR_ARGS)) {
            IMetricsCollector<?> collector = createCollector(definition.getCollector(), context);
            ConditionalEvaluatorDefinition primaryEvaluator = definition.getEvaluators().get(0);
            ConditionEvaluator<?> evaluator = createEvaluator(primaryEvaluator);

            parameters.put(CONSTRUCTOR_ARG_TYPES, List.of(
                    "tools.spirals.cerberus237.metricscollectorbase.IMetricsCollector",
                    "tools.spirals.cerberus237.adaptiflow.interfaces.ConditionEvaluator"));
            parameters.put(CONSTRUCTOR_ARGS, List.of(collector, evaluator));
        }

        return new ComponentSpec(className, parameters);
    }

    private <T> T createComponent(String className, Map<String, Object> parameters, Class<T> expectedType) {
        try {
            Class<?> rawClass = Class.forName(className);
            Object instance;

            if (parameters.containsKey(FACTORY_METHOD)) {
                instance = invokeFactoryMethod(rawClass, parameters);
            } else {
                instance = instantiateClass(rawClass, parameters);
            }

            if (!expectedType.isInstance(instance)) {
                throw new InvalidConfigurationException(
                        "Class '" + className + "' is not assignable to " + expectedType.getName());
            }
            return expectedType.cast(instance);
        } catch (InvalidConfigurationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InvalidConfigurationException(
                    "Failed to instantiate class '" + className + "' for scenario '" + scenario.getId() + "': " + ex.getMessage());
        }
    }

    private Object instantiateClass(Class<?> rawClass, Map<String, Object> parameters) throws Exception {
        if (parameters.containsKey(CONSTRUCTOR_ARG_TYPES) || parameters.containsKey(CONSTRUCTOR_ARGS)) {
            return invokeConstructor(rawClass, parameters);
        }

        try {
            Constructor<?> mapConstructor = rawClass.getConstructor(Map.class);
            return mapConstructor.newInstance(parameters);
        } catch (NoSuchMethodException ex) {
            // Fall back to default constructor and optional setter injection.
        }

        Constructor<?> defaultConstructor = rawClass.getConstructor();
        Object instance = defaultConstructor.newInstance();
        applySetters(instance, parameters);
        return instance;
    }

    private Object invokeConstructor(Class<?> rawClass, Map<String, Object> parameters) throws Exception {
        List<String> typeNames = stringList(parameters.getOrDefault(CONSTRUCTOR_ARG_TYPES, List.of()));
        List<Object> rawArgs = objectList(parameters.getOrDefault(CONSTRUCTOR_ARGS, List.of()));
        if (typeNames.size() != rawArgs.size()) {
            throw new InvalidConfigurationException("constructorArgTypes and constructorArgs size mismatch");
        }

        Class<?>[] argTypes = new Class<?>[typeNames.size()];
        Object[] convertedArgs = new Object[typeNames.size()];
        for (int i = 0; i < typeNames.size(); i++) {
            argTypes[i] = resolveType(typeNames.get(i));
            convertedArgs[i] = convertArgument(rawArgs.get(i), argTypes[i]);
        }

        Constructor<?> constructor = rawClass.getConstructor(argTypes);
        return constructor.newInstance(convertedArgs);
    }

    private Object invokeFactoryMethod(Class<?> rawClass, Map<String, Object> parameters) throws Exception {
        String methodName = requireString(parameters, FACTORY_METHOD, rawClass.getName());
        List<String> typeNames = stringList(parameters.getOrDefault(FACTORY_ARG_TYPES, List.of()));
        List<Object> rawArgs = objectList(parameters.getOrDefault(FACTORY_ARGS, List.of()));
        if (typeNames.size() != rawArgs.size()) {
            throw new InvalidConfigurationException("factoryArgTypes and factoryArgs size mismatch");
        }

        Class<?>[] argTypes = new Class<?>[typeNames.size()];
        Object[] convertedArgs = new Object[typeNames.size()];
        for (int i = 0; i < typeNames.size(); i++) {
            argTypes[i] = resolveType(typeNames.get(i));
            convertedArgs[i] = convertArgument(rawArgs.get(i), argTypes[i]);
        }

        Method method = rawClass.getMethod(methodName, argTypes);
        Object target = Modifier.isStatic(method.getModifiers())
                ? null
                : rawClass.getConstructor().newInstance();
        return method.invoke(target, convertedArgs);
    }

    private void applySetters(Object instance, Map<String, Object> parameters) throws Exception {
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String key = entry.getKey();
            if (isReservedParameter(key)) {
                continue;
            }

            String setterName = "set" + Character.toUpperCase(key.charAt(0)) + key.substring(1);
            Method[] methods = instance.getClass().getMethods();
            for (Method method : methods) {
                if (!method.getName().equals(setterName) || method.getParameterCount() != 1) {
                    continue;
                }
                Object converted = convertArgument(entry.getValue(), method.getParameterTypes()[0]);
                method.invoke(instance, converted);
                break;
            }
        }
    }

    private boolean isReservedParameter(String key) {
        return CONSTRUCTOR_ARG_TYPES.equals(key)
                || CONSTRUCTOR_ARGS.equals(key)
                || FACTORY_METHOD.equals(key)
                || FACTORY_ARG_TYPES.equals(key)
                || FACTORY_ARGS.equals(key);
    }

    private Object createScheduler(List<Event> events, int intervalMs) {
        ObservationSchedulerDefinition schedulerDefinition = scenario.getScheduler();
        String schedulerType = schedulerDefinition == null
                ? DEFAULT_SCHEDULER_CLASS
                : schedulerDefinition.getType();

        Map<String, Object> parameters = cloneParameters(
                schedulerDefinition == null ? null : schedulerDefinition.getParameters());

        String className = resolveClassName(schedulerType, SCHEDULER_PACKAGES);

        if (!parameters.containsKey(CONSTRUCTOR_ARG_TYPES) && !parameters.containsKey(CONSTRUCTOR_ARGS)) {
            parameters.put(CONSTRUCTOR_ARG_TYPES, List.of("java.util.List", "int"));
            parameters.put(CONSTRUCTOR_ARGS, List.of(events, intervalMs));
        }

        return createComponent(className, parameters, Object.class);
    }

    private String resolveClassName(String configuredType, List<String> packageCandidates) {
        String type = stringOrDefault(configuredType, "");
        if (type.isEmpty()) {
            throw new InvalidConfigurationException("Missing class name in scenario component definition");
        }

        if (type.contains(".")) {
            ensureClassExists(type);
            return type;
        }

        for (String packageName : packageCandidates) {
            String candidate = packageName + "." + type;
            if (classExists(candidate)) {
                return candidate;
            }
        }

        throw new InvalidConfigurationException(
                "Unable to resolve class name '" + type + "' in candidate packages " + packageCandidates);
    }

    private boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    private void ensureClassExists(String className) {
        if (!classExists(className)) {
            throw new InvalidConfigurationException("Class not found: " + className);
        }
    }

    private boolean isConditionalEventClass(String className) {
        return className.endsWith("ConditionalEvent");
    }

    private Class<?> resolveType(String typeName) throws ClassNotFoundException {
        if ("int".equals(typeName)) {
            return int.class;
        }
        if ("long".equals(typeName)) {
            return long.class;
        }
        if ("double".equals(typeName)) {
            return double.class;
        }
        if ("boolean".equals(typeName)) {
            return boolean.class;
        }
        return Class.forName(typeName);
    }

    private Object convertArgument(Object value, Class<?> targetType) throws ClassNotFoundException {
        if (value == null) {
            return null;
        }
        if (targetType.isInstance(value)) {
            return value;
        }
        if (targetType == String.class) {
            return String.valueOf(value);
        }
        if (targetType == int.class || targetType == Integer.class) {
            return Integer.parseInt(String.valueOf(value));
        }
        if (targetType == long.class || targetType == Long.class) {
            return Long.parseLong(String.valueOf(value));
        }
        if (targetType == double.class || targetType == Double.class) {
            return Double.parseDouble(String.valueOf(value));
        }
        if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(String.valueOf(value));
        }
        if (targetType == Class.class) {
            return Class.forName(String.valueOf(value));
        }
        if (targetType == Supplier.class) {
            final Object fixedValue = value;
            return (Supplier<Object>) () -> fixedValue;
        }
        return value;
    }

    private List<String> stringList(Object rawList) {
        List<Object> objects = objectList(rawList);
        List<String> values = new ArrayList<>();
        for (Object object : objects) {
            values.add(String.valueOf(object));
        }
        return values;
    }

    private List<Object> objectList(Object rawList) {
        if (rawList == null) {
            return List.of();
        }
        if (!(rawList instanceof List)) {
            throw new InvalidConfigurationException("Expected list value in component instantiation parameters");
        }
        @SuppressWarnings("unchecked")
        List<Object> values = (List<Object>) rawList;
        return values;
    }

    private Map<String, Object> cloneParameters(Map<String, Object> parameters) {
        if (parameters == null) {
            return new LinkedHashMap<>();
        }
        return new LinkedHashMap<>(parameters);
    }

    private String requireString(Map<String, Object> parameters, String key, String type) {
        Object value = parameters.get(key);
        if (value == null || String.valueOf(value).trim().isEmpty()) {
            throw new InvalidConfigurationException("Missing required parameter '" + key + "' for type '" + type + "'");
        }
        return String.valueOf(value);
    }

    private String stringOrDefault(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String parsed = String.valueOf(value).trim();
        return parsed.isEmpty() ? defaultValue : parsed;
    }

    private static final class ComponentSpec {
        private final String className;
        private final Map<String, Object> parameters;

        private ComponentSpec(String className, Map<String, Object> parameters) {
            this.className = className;
            this.parameters = parameters;
        }
    }
}
