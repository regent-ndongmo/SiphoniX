package tools.spirals.cerberus237.siphonix.plugins.adaptiflow.core.scenarios;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Event definition binding metrics collection, conditional evaluators and
 * adaptation actions.
 */
public class EventDefinition {

    private String id;
    private String type;
    private Map<String, Object> parameters = new LinkedHashMap<>();
    private MetricCollectorDefinition collector;
    private List<ConditionalEvaluatorDefinition> evaluators = new ArrayList<>();
    private List<ActionDefinition> actions = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public MetricCollectorDefinition getCollector() {
        return collector;
    }

    public void setCollector(MetricCollectorDefinition collector) {
        this.collector = collector;
    }

    public List<ConditionalEvaluatorDefinition> getEvaluators() {
        return evaluators;
    }

    public void setEvaluators(List<ConditionalEvaluatorDefinition> evaluators) {
        this.evaluators = evaluators;
    }

    public List<ActionDefinition> getActions() {
        return actions;
    }

    public void setActions(List<ActionDefinition> actions) {
        this.actions = actions;
    }
}
