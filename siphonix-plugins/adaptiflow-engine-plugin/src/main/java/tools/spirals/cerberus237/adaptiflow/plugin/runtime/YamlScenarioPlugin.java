package tools.spirals.cerberus237.adaptiflow.plugin.runtime;

import tools.spirals.cerberus237.adaptiflow.plugin.core.scenarios.ScenarioDefinition;

/**
 * Backward-compatible alias kept while the runtime plugin is generalized around
 * ScenarioPlugin and a dedicated YAML materialization strategy.
 */
@Deprecated
public class YamlScenarioPlugin extends ScenarioPlugin {

    public YamlScenarioPlugin(ScenarioDefinition scenario) {
        super(scenario, new ScenarioRuntimeFactoryImpl());
    }
}
