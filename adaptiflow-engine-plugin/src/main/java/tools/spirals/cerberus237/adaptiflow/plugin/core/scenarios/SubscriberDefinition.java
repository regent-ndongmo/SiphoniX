package tools.spirals.cerberus237.adaptiflow.plugin.core.scenarios;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Declarative event subscriber definition.
 */
public class SubscriberDefinition {

    private String type;
    private Map<String, Object> parameters = new LinkedHashMap<>();
    private List<ActionDefinition> actions = new ArrayList<>();

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

    public List<ActionDefinition> getActions() {
        return actions;
    }

    public void setActions(List<ActionDefinition> actions) {
        this.actions = actions;
    }
}