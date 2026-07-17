package tools.spirals.cerberus237.adaptiflow.plugin.runtime;

import tools.spirals.cerberus237.adaptiflow.plugin.core.scenarios.ScenarioDefinition;

/**
 * XML-backed scenario runtime plugin.
 */
public class XmlScenarioPlugin extends ScenarioPlugin {

    public XmlScenarioPlugin(ScenarioDefinition scenario) {
        super(scenario, new ScenarioRuntimeFactoryImpl());
    }
}