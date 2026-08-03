package tools.spirals.cerberus237.adaptiflow.plugin.core.scenarios;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Declarative adaptation action definition for a scenario event.
 * <p>
 * Each action captures a target implementation type and an optional parameter map used by the
 * runtime factory to instantiate the action.
 * </p>
 *
 * @author Arléon Zemtsop (Cerberus)
 */
public class ActionDefinition {

    private String type;
    private Map<String, Object> parameters = new LinkedHashMap<>();

    /**
     * Returns the action type name.
     *
     * @return action implementation identifier or class name
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the action type name.
     *
     * @param type action implementation identifier or class name
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Returns constructor or factory parameters used for action materialization.
     *
     * @return mutable action parameter map
     */
    public Map<String, Object> getParameters() {
        return parameters;
    }

    /**
     * Replaces action parameters.
     *
     * @param parameters action parameter map
     */
    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }
}
