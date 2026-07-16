package tools.spirals.cerberus237.siphonix.api.plugin.management;

import java.io.IOException;

public interface ScenarioManagementRestApi {

    String CREATE_SCENARIO_PATH = "/api/scenarios";
    String UPDATE_SCENARIO_PATH = "/api/scenarios/{id}";
    String DELETE_SCENARIO_PATH = "/api/scenarios/{id}";
    String ENABLE_SCENARIO_PATH = "/api/scenarios/{id}/enable";
    String DISABLE_SCENARIO_PATH = "/api/scenarios/{id}/disable";

    void postScenario(ScenarioSource source) throws IOException;

    void putScenario(String scenarioId, ScenarioSource source) throws IOException;

    void deleteScenario(String scenarioId);

    void enableScenario(String scenarioId);

    void disableScenario(String scenarioId);
}
