package tools.spirals.cerberus237.adaptiflow.plugin.runtime;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import tools.spirals.cerberus237.adaptationactionsbase.core.IAdaptationAction;
import tools.spirals.cerberus237.adaptiflow.events.Event;
import tools.spirals.cerberus237.adaptiflow.interfaces.ConditionEvaluator;
import tools.spirals.cerberus237.adaptiflow.interfaces.Observer;
import tools.spirals.cerberus237.adaptiflow.plugin.core.config.InvalidConfigurationException;
import tools.spirals.cerberus237.adaptiflow.plugin.core.scenarios.ActionDefinition;
import tools.spirals.cerberus237.adaptiflow.plugin.core.scenarios.ConditionalEvaluatorDefinition;
import tools.spirals.cerberus237.adaptiflow.plugin.core.scenarios.EventDefinition;
import tools.spirals.cerberus237.adaptiflow.plugin.core.scenarios.MetricCollectorDefinition;
import tools.spirals.cerberus237.adaptiflow.plugin.core.scenarios.ObservationSchedulerDefinition;
import tools.spirals.cerberus237.adaptiflow.plugin.core.scenarios.ScenarioDefinition;
import tools.spirals.cerberus237.adaptiflow.plugin.core.scenarios.SubscriberDefinition;
import tools.spirals.cerberus237.adaptiflow.subscriptions.subscribers.EventSubscriber;
import tools.spirals.cerberus237.metricscollectorbase.IMetricsCollector;
import tools.spirals.cerberus237.siphonix.api.plugin.PluginContext;

public class ScenarioRuntimeFactoryImpl implements ScenarioRuntimeFactory {

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

        private static final List<String> SUBSCRIBER_PACKAGES = List.of(
            "tools.spirals.cerberus237.adaptiflow.subscriptions.subscribers");

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Event buildEvent(ScenarioDefinition scenario, EventDefinition eventDefinition, PluginContext context) {
        Event event = createEvent(scenario, eventDefinition, context);

        List<Observer<Object>> subscribers = createSubscribers(eventDefinition.getSubscribers(), scenario.getId());
        event.subscribeAll(subscribers);
        return event;
    }

    @Override
    public Object createScheduler(ScenarioDefinition scenario, List<Event> events) {
        ObservationSchedulerDefinition schedulerDefinition = scenario.getScheduler();
        String schedulerType = schedulerDefinition == null
                ? DEFAULT_SCHEDULER_CLASS
                : schedulerDefinition.getType();

        Map<String, Object> parameters = cloneParameters(
                schedulerDefinition == null ? null : schedulerDefinition.getParameters());

        String className = resolveClassName(schedulerType, SCHEDULER_PACKAGES);

        if (!parameters.containsKey(CONSTRUCTOR_ARG_TYPES) && !parameters.containsKey(CONSTRUCTOR_ARGS)) {
            populateSchedulerConstructorMetadata(className, scenario, events, parameters);
        }

        return createComponent(className, parameters, Object.class, scenario.getId());
    }

    private void populateSchedulerConstructorMetadata(String className, ScenarioDefinition scenario, List<Event> events,
            Map<String, Object> parameters) {
        try {
            Class<?> schedulerClass = Class.forName(className);

            for (Constructor<?> constructor : schedulerClass.getConstructors()) {
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                if (parameterTypes.length == 3
                        && isCompatibleParameter(parameterTypes[0], events)
                        && isCompatibleParameter(parameterTypes[1], Integer.valueOf(scenario.getIntervalMs()))
                        && isCompatibleStringParameter(parameterTypes[2])) {
                    parameters.put(CONSTRUCTOR_ARG_TYPES, List.of(
                            typeNameFor(parameterTypes[0]),
                            typeNameFor(parameterTypes[1]),
                            typeNameFor(parameterTypes[2])));
                    parameters.put(CONSTRUCTOR_ARGS, List.of(events, scenario.getIntervalMs(), scenario.getPluginId()));
                    return;
                }
            }
        } catch (ClassNotFoundException ex) {
            throw new InvalidConfigurationException("Class not found: " + className);
        }

        parameters.put(CONSTRUCTOR_ARG_TYPES, List.of("java.util.List", "int"));
        parameters.put(CONSTRUCTOR_ARGS, List.of(events, scenario.getIntervalMs()));
    }

    private Event createEvent(ScenarioDefinition scenario, EventDefinition eventDefinition, PluginContext context) {
        ComponentSpec spec = resolveEventSpec(scenario, eventDefinition, context);
        return createComponent(spec.className, spec.parameters, Event.class, scenario.getId());
    }

    private IMetricsCollector<?> createCollector(ScenarioDefinition scenario, MetricCollectorDefinition definition,
            PluginContext context) {
        ComponentSpec spec = resolveCollectorSpec(definition);
        return createComponent(spec.className, spec.parameters, IMetricsCollector.class, scenario.getId());
    }

    private ConditionEvaluator<?> createEvaluator(ScenarioDefinition scenario,
            ConditionalEvaluatorDefinition definition) {
        ComponentSpec spec = resolveEvaluatorSpec(definition);
        return createComponent(spec.className, spec.parameters, ConditionEvaluator.class, scenario.getId());
    }

    private List<IAdaptationAction> createActions(List<ActionDefinition> actionDefinitions, String scenarioId) {
        List<IAdaptationAction> actions = new ArrayList<>();
        if (actionDefinitions == null) {
            return actions;
        }
        for (ActionDefinition actionDefinition : actionDefinitions) {
            ComponentSpec spec = resolveActionSpec(actionDefinition);
            actions.add(createComponent(spec.className, spec.parameters, IAdaptationAction.class, scenarioId));
        }
        return actions;
    }

    private List<Observer<Object>> createSubscribers(List<SubscriberDefinition> subscriberDefinitions,
            String scenarioId) {
        if (subscriberDefinitions == null || subscriberDefinitions.isEmpty()) {
            return List.of(new EventSubscriber(List.of()));
        }

        List<Observer<Object>> subscribers = new ArrayList<>();
        for (SubscriberDefinition subscriberDefinition : subscriberDefinitions) {
            List<IAdaptationAction> actions = createActions(subscriberDefinition.getActions(), scenarioId);
            ComponentSpec spec = resolveSubscriberSpec(subscriberDefinition, actions);
            subscribers.add(createComponent(spec.className, spec.parameters, Observer.class, scenarioId));
        }
        return subscribers;
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

    private ComponentSpec resolveSubscriberSpec(SubscriberDefinition definition, List<IAdaptationAction> actions) {
        String className = resolveClassName(definition.getType(), SUBSCRIBER_PACKAGES);
        Map<String, Object> parameters = cloneParameters(definition.getParameters());

        List<String> typeNames = new ArrayList<>();
        typeNames.add("java.util.List");
        typeNames.addAll(stringList(parameters.getOrDefault(CONSTRUCTOR_ARG_TYPES, List.of())));

        List<Object> constructorArgs = new ArrayList<>();
        constructorArgs.add(actions);
        constructorArgs.addAll(objectList(parameters.getOrDefault(CONSTRUCTOR_ARGS, List.of())));

        parameters.put(CONSTRUCTOR_ARG_TYPES, typeNames);
        parameters.put(CONSTRUCTOR_ARGS, constructorArgs);

        return new ComponentSpec(className, parameters);
    }

    private ComponentSpec resolveEventSpec(ScenarioDefinition scenario, EventDefinition definition,
            PluginContext context) {
        String configuredType = stringOrDefault(definition.getType(), DEFAULT_EVENT_CLASS);
        String className = resolveClassName(configuredType, EVENT_PACKAGES);
        Map<String, Object> parameters = cloneParameters(definition.getParameters());

        populateEventConstructorMetadata(className, scenario, definition, context, parameters);

        return new ComponentSpec(className, parameters);
    }

    private void populateEventConstructorMetadata(String className, ScenarioDefinition scenario,
            EventDefinition definition, PluginContext context, Map<String, Object> parameters) {
        if (parameters.containsKey(CONSTRUCTOR_ARG_TYPES) || parameters.containsKey(CONSTRUCTOR_ARGS)) {
            return;
        }
        if (definition.getCollector() == null || definition.getEvaluators() == null || definition.getEvaluators().isEmpty()) {
            return;
        }

        IMetricsCollector<?> collector = createCollector(scenario, definition.getCollector(), context);
        ConditionEvaluator<?> evaluator = createEvaluator(scenario, definition.getEvaluators().get(0));
        String eventId = stringOrDefault(definition.getId(), "");

        try {
            Class<?> eventClass = Class.forName(className);
            ConstructorMatch match = findCompatibleEventConstructor(eventClass, eventId, collector, evaluator);
            if (match == null) {
                return;
            }

            parameters.put(CONSTRUCTOR_ARG_TYPES, match.typeNames);
            parameters.put(CONSTRUCTOR_ARGS, match.arguments);
        } catch (ClassNotFoundException ex) {
            throw new InvalidConfigurationException("Class not found: " + className);
        }
    }

    private ConstructorMatch findCompatibleEventConstructor(Class<?> eventClass, String eventId,
            IMetricsCollector<?> collector, ConditionEvaluator<?> evaluator) {
        if (!eventId.isEmpty()) {
            for (Constructor<?> constructor : eventClass.getConstructors()) {
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                if (parameterTypes.length != 3) {
                    continue;
                }
                if (isCompatibleStringParameter(parameterTypes[0])
                        && isCompatibleParameter(parameterTypes[1], collector)
                        && isCompatibleParameter(parameterTypes[2], evaluator)) {
                    List<String> typeNames = List.of(
                            typeNameFor(parameterTypes[0]),
                            typeNameFor(parameterTypes[1]),
                            typeNameFor(parameterTypes[2]));
                    return new ConstructorMatch(typeNames, List.of(eventId, collector, evaluator));
                }
            }
        }

        for (Constructor<?> constructor : eventClass.getConstructors()) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            if (parameterTypes.length != 2) {
                continue;
            }
            if (isCompatibleParameter(parameterTypes[0], collector)
                    && isCompatibleParameter(parameterTypes[1], evaluator)) {
                List<String> typeNames = List.of(typeNameFor(parameterTypes[0]), typeNameFor(parameterTypes[1]));
                return new ConstructorMatch(typeNames, List.of(collector, evaluator));
            }
        }

        return null;
    }

    private boolean isCompatibleStringParameter(Class<?> parameterType) {
        return parameterType == String.class
                || parameterType.isAssignableFrom(String.class)
                || "java.lang.String".equals(parameterType.getName());
    }

    private boolean isCompatibleParameter(Class<?> parameterType, Object value) {
        if (value == null) {
            return !parameterType.isPrimitive();
        }
        if (isPrimitiveWrapperCompatible(parameterType, value.getClass())) {
            return true;
        }
        if (parameterType.isInstance(value) || parameterType.isAssignableFrom(value.getClass())) {
            return true;
        }

        String targetTypeName = parameterType.getName();
        Class<?> inspected = value.getClass();
        while (inspected != null) {
            if (targetTypeName.equals(inspected.getName())) {
                return true;
            }
            for (Class<?> iface : inspected.getInterfaces()) {
                if (targetTypeName.equals(iface.getName())) {
                    return true;
                }
            }
            inspected = inspected.getSuperclass();
        }
        return false;
    }

    private boolean isPrimitiveWrapperCompatible(Class<?> parameterType, Class<?> valueType) {
        if (parameterType == int.class) {
            return valueType == Integer.class;
        }
        if (parameterType == long.class) {
            return valueType == Long.class;
        }
        if (parameterType == double.class) {
            return valueType == Double.class;
        }
        if (parameterType == boolean.class) {
            return valueType == Boolean.class;
        }
        return false;
    }

    private String typeNameFor(Class<?> type) {
        if (type == int.class) {
            return "int";
        }
        if (type == long.class) {
            return "long";
        }
        if (type == double.class) {
            return "double";
        }
        if (type == boolean.class) {
            return "boolean";
        }
        return type.getName();
    }

    private <T> T createComponent(String className, Map<String, Object> parameters, Class<T> expectedType,
            String scenarioId) {
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
                    "Failed to instantiate class '" + className + "' for scenario '" + scenarioId + "': "
                            + ex.getMessage());
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
        if (isFunctionalInterface(targetType)) {
            return toFunctionalInterface(targetType, value);
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

    private boolean isFunctionalInterface(Class<?> type) {
        return type.isInterface() && findSamMethod(type) != null;
    }

    private Object toFunctionalInterface(Class<?> functionalType, Object rawValue) throws ClassNotFoundException {
        Method samMethod = findSamMethod(functionalType);
        if (samMethod == null) {
            throw new InvalidConfigurationException(
                    "Type '" + functionalType.getName() + "' is not a functional interface");
        }

        final Object lambdaResult;
        if (samMethod.getReturnType() == void.class) {
            lambdaResult = null;
        } else {
            lambdaResult = convertSimpleValue(rawValue, samMethod.getReturnType());
        }

        InvocationHandler handler = (proxy, method, args) -> {
            if (isObjectMethod(method, "hashCode", 0)) {
                return System.identityHashCode(proxy);
            }
            if (isObjectMethod(method, "equals", 1)) {
                return proxy == args[0];
            }
            if (isObjectMethod(method, "toString", 0)) {
                return functionalType.getSimpleName() + "(" + String.valueOf(rawValue) + ")";
            }
            if (isSameSignature(method, samMethod)) {
                return lambdaResult;
            }
            throw new UnsupportedOperationException("Unsupported method on lambda proxy: " + method.getName());
        };

        return Proxy.newProxyInstance(
                functionalType.getClassLoader(),
                new Class<?>[] { functionalType },
                handler);
    }

    private Method findSamMethod(Class<?> functionalType) {
        Method candidate = null;
        for (Method method : functionalType.getMethods()) {
            if (method.isDefault() || Modifier.isStatic(method.getModifiers())
                    || method.getDeclaringClass() == Object.class) {
                continue;
            }
            if (candidate != null && !isSameSignature(candidate, method)) {
                return null;
            }
            candidate = method;
        }
        return candidate;
    }

    private boolean isSameSignature(Method left, Method right) {
        if (!left.getName().equals(right.getName())) {
            return false;
        }
        Class<?>[] leftParams = left.getParameterTypes();
        Class<?>[] rightParams = right.getParameterTypes();
        if (leftParams.length != rightParams.length) {
            return false;
        }
        for (int i = 0; i < leftParams.length; i++) {
            if (!leftParams[i].equals(rightParams[i])) {
                return false;
            }
        }
        return true;
    }

    private boolean isObjectMethod(Method method, String name, int parameterCount) {
        return method.getDeclaringClass() == Object.class
                && method.getName().equals(name)
                && method.getParameterCount() == parameterCount;
    }

    private Object convertSimpleValue(Object value, Class<?> targetType) throws ClassNotFoundException {
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
        if (targetType.isEnum()) {
            @SuppressWarnings({ "rawtypes", "unchecked" })
            Enum enumValue = Enum.valueOf((Class<? extends Enum>) targetType, String.valueOf(value));
            return enumValue;
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
            throw new InvalidConfigurationException(
                    "Missing required parameter '" + key + "' for type '" + type + "'");
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

    private static final class ConstructorMatch {
        private final List<String> typeNames;
        private final List<Object> arguments;

        private ConstructorMatch(List<String> typeNames, List<Object> arguments) {
            this.typeNames = typeNames;
            this.arguments = arguments;
        }
    }
}