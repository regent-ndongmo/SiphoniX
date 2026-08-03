package tools.spirals.cerberus237.siphonix.api.plugin.management;

import java.io.IOException;

/**
 * Command-line oriented contract for scenario management operations.
 * <p>
 * Implementations should provide user-friendly validation and error reporting semantics suited for
 * interactive or scripted shell usage.
 * </p>
 *
 * @author Arléon Zemtsop (Cerberus)
 */
public interface ScenarioManagementCli {

    /**
     * Creates or applies a scenario from a local or remote source path.
     *
     * @param sourcePath path or locator used to resolve scenario content
     * @throws IOException when the source cannot be read or applied
     */
    void applyScenario(String sourcePath) throws IOException;

    /**
     * Updates an existing scenario from a source path.
     *
     * @param scenarioId identifier of the scenario to update
     * @param sourcePath path or locator used to resolve replacement scenario content
     * @throws IOException when source loading or update processing fails
     */
    void updateScenario(String scenarioId, String sourcePath) throws IOException;

    /**
     * Disables a scenario from command-line workflows.
     *
     * @param scenarioId identifier of the scenario to disable
     */
    void disableScenario(String scenarioId);
}
