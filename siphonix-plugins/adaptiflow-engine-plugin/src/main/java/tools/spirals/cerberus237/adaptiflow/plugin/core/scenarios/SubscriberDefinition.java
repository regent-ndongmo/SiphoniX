package tools.spirals.cerberus237.adaptiflow.plugin.core.scenarios;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Declarative event subscriber definition.
 * <p>
 * A subscriber defines the observer implementation and the action chain to execute when the
 * underlying event condition is satisfied.
 * </p>
 *
 * @author Arléon Zemtsop (Cerberus)
 */
public class SubscriberDefinition {

    private String type;
    private Map<String, Object> parameters = new LinkedHashMap<>();
    private List<ActionDefinition> actions = new ArrayList<>();

    /**
     * @return subscriber type identifier or fully-qualified class name
     */
    public String getType() {
        return type;
    }

    /**
     * @param type subscriber type identifier or fully-qualified class name
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return subscriber materialization parameters
     */
    public Map<String, Object> getParameters() {
        return parameters;
    }

    /**
     * @param parameters subscriber materialization parameters
     */
    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    /**
     * @return ordered actions attached to this subscriber
     */
    public List<ActionDefinition> getActions() {
        return actions;
    }

    /**
     * @param actions ordered actions attached to this subscriber
     */
    public void setActions(List<ActionDefinition> actions) {
        this.actions = actions;
    }
}