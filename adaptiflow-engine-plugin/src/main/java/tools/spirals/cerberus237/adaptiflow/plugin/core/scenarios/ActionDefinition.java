package tools.spirals.cerberus237.adaptiflow.plugin.core.scenarios;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Declarative adaptation action definition for a scenario event.
 */
public class ActionDefinition {

    private String type;
    private Map<String, Object> parameters = new LinkedHashMap<>();

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
}
