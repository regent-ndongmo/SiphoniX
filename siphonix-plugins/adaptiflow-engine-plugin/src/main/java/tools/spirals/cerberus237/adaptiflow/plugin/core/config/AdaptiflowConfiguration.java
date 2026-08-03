package tools.spirals.cerberus237.adaptiflow.plugin.core.config;

import java.util.LinkedHashMap;
import java.util.Map;

import tools.spirals.cerberus237.adaptiflow.plugin.core.scenarios.ScenarioDefinition;

/**
 * Root configuration model for SiphoniX runtime configuration files.
 */
public class AdaptiflowConfiguration {

    private Map<String, ScenarioDefinition> scenarios = new LinkedHashMap<>();

    public Map<String, ScenarioDefinition> getScenarios() {
        return scenarios;
    }

    public void setScenarios(Map<String, ScenarioDefinition> scenarios) {
        this.scenarios = scenarios;
    }
}
