package tools.spirals.cerberus237.siphonix.api.plugin.management;

import java.io.IOException;
import java.util.List;

public interface ScenarioManagementService {

    void createScenario(ScenarioSource source) throws IOException;

    void updateScenario(String scenarioId, ScenarioSource source) throws IOException;

    void deleteScenario(String scenarioId);

    void enableScenario(String scenarioId);

    void disableScenario(String scenarioId);

    List<String> listScenarios();

    ScenarioValidationResult validateScenario(ScenarioSource source) throws IOException;
}
