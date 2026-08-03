package tools.spirals.cerberus237.siphonix.api.plugin.management;

import java.io.IOException;

public interface ScenarioManagementCli {

    void applyScenario(String sourcePath) throws IOException;

    void updateScenario(String scenarioId, String sourcePath) throws IOException;

    void disableScenario(String scenarioId);
}
