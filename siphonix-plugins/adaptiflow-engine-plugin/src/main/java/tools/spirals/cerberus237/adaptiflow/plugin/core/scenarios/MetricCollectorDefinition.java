package tools.spirals.cerberus237.adaptiflow.plugin.core.scenarios;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Declarative metrics collector definition for a scenario event.
 *
 * @author Arléon Zemtsop (Cerberus)
 */
public class MetricCollectorDefinition {

    private String type;
    private Map<String, Object> parameters = new LinkedHashMap<>();

    /**
     * @return collector type identifier or fully-qualified class name
     */
    public String getType() {
        return type;
    }

    /**
     * @param type collector type identifier or fully-qualified class name
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return collector materialization parameters
     */
    public Map<String, Object> getParameters() {
        return parameters;
    }

    /**
     * @param parameters collector materialization parameters
     */
    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }
}
