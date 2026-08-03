package tools.spirals.cerberus237.adaptiflow.plugin.runtime;

import tools.spirals.cerberus237.adaptiflow.plugin.core.scenarios.ScenarioDefinition;

/**
 * Backward-compatible alias kept while the runtime plugin is generalized around
 * ScenarioPlugin and a dedicated YAML materialization strategy.
 *
 * @author Arléon Zemtsop (Cerberus)
 */
@Deprecated
public class YamlScenarioPlugin extends ScenarioPlugin {

    /**
     * Creates a YAML scenario plugin runtime wrapper.
     *
     * @param scenario scenario definition
     */
    public YamlScenarioPlugin(ScenarioDefinition scenario) {
        super(scenario, new ScenarioRuntimeFactoryImpl());
    }
}
