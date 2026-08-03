package tools.spirals.cerberus237.siphonix.api.plugin.management;

import java.io.IOException;
import java.util.List;

/**
 * Service contract for programmatic scenario lifecycle management.
 * <p>
 * Implementations are responsible for persistence, validation, activation state management, and
 * consistency guarantees around scenario operations.
 * </p>
 *
 * @author Arléon Zemtsop (Cerberus)
 */
public interface ScenarioManagementService {

    /**
     * Creates and stores a new scenario from the provided source.
     *
     * @param source source abstraction used to load scenario content
     * @throws IOException when source loading fails or the scenario cannot be persisted
     */
    void createScenario(ScenarioSource source) throws IOException;

    /**
     * Updates an existing scenario with new content.
     *
     * @param scenarioId unique identifier of the scenario to update
     * @param source source abstraction used to load replacement content
     * @throws IOException when loading, validation, or persistence fails
     */
    void updateScenario(String scenarioId, ScenarioSource source) throws IOException;

    /**
     * Deletes a scenario and its associated runtime metadata.
     *
     * @param scenarioId unique identifier of the scenario to remove
     */
    void deleteScenario(String scenarioId);

    /**
     * Enables a scenario so it can be considered for runtime orchestration.
     *
     * @param scenarioId unique identifier of the scenario to enable
     */
    void enableScenario(String scenarioId);

    /**
     * Disables a scenario to exclude it from runtime orchestration.
     *
     * @param scenarioId unique identifier of the scenario to disable
     */
    void disableScenario(String scenarioId);

    /**
     * Lists known scenario identifiers.
     *
     * @return identifiers for all managed scenarios
     */
    List<String> listScenarios();

    /**
     * Validates scenario content without necessarily mutating persisted state.
     *
     * @param source source abstraction used to load scenario content for validation
     * @return validation result with validity flag and detailed errors
     * @throws IOException when source loading fails
     */
    ScenarioValidationResult validateScenario(ScenarioSource source) throws IOException;
}
