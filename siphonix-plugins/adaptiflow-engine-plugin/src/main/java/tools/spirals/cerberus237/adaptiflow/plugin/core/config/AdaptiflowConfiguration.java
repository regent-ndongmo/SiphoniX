package tools.spirals.cerberus237.adaptiflow.plugin.core.config;

import java.util.LinkedHashMap;
import java.util.Map;

import tools.spirals.cerberus237.adaptiflow.plugin.core.scenarios.ScenarioDefinition;

/**
 * Root configuration model for SiphoniX runtime configuration files.
 * <p>
 * This model keeps scenarios in insertion order to preserve deterministic serialization and
 * execution diagnostics.
 * </p>
 *
 * @author Arléon Zemtsop (Cerberus)
 */
public class AdaptiflowConfiguration {

    private Map<String, ScenarioDefinition> scenarios = new LinkedHashMap<>();

    /**
     * @return scenarios keyed by scenario id
     */
    public Map<String, ScenarioDefinition> getScenarios() {
        return scenarios;
    }

    /**
     * @param scenarios scenarios keyed by scenario id
     */
    public void setScenarios(Map<String, ScenarioDefinition> scenarios) {
        this.scenarios = scenarios;
    }
}
