package tools.spirals.cerberus237.adaptiflow.plugin.runtime;

import tools.spirals.cerberus237.adaptiflow.plugin.core.scenarios.ScenarioDefinition;

/**
 * XML-backed scenario runtime plugin.
 * <p>
 * This thin wrapper exists to keep source-format semantics explicit while delegating runtime
 * construction to {@link ScenarioPlugin}.
 * </p>
 *
 * @author Arléon Zemtsop (Cerberus)
 */
public class XmlScenarioPlugin extends ScenarioPlugin {

    /**
     * Creates an XML scenario plugin runtime wrapper.
     *
     * @param scenario scenario definition
     */
    public XmlScenarioPlugin(ScenarioDefinition scenario) {
        super(scenario, new ScenarioRuntimeFactoryImpl());
    }
}