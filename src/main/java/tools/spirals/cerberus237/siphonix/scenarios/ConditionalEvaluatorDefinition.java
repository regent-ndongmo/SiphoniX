package tools.spirals.cerberus237.siphonix.scenarios;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Declarative conditional evaluator definition. Thresholds are modeled as a
 * specific evaluator type with dedicated parameters.
 */
public class ConditionalEvaluatorDefinition {

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
