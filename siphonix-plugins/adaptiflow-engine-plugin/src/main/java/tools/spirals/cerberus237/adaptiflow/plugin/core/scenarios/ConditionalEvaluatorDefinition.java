package tools.spirals.cerberus237.adaptiflow.plugin.core.scenarios;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Declarative conditional evaluator definition. Thresholds are modeled as a
 * specific evaluator type with dedicated parameters.
 *
 * @author Arléon Zemtsop (Cerberus)
 */
public class ConditionalEvaluatorDefinition {

    private String type;
    private Map<String, Object> parameters = new LinkedHashMap<>();

    /**
     * @return evaluator type identifier or fully-qualified class name
     */
    public String getType() {
        return type;
    }

    /**
     * @param type evaluator type identifier or fully-qualified class name
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return evaluator materialization parameters
     */
    public Map<String, Object> getParameters() {
        return parameters;
    }

    /**
     * @param parameters evaluator materialization parameters
     */
    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }
}
