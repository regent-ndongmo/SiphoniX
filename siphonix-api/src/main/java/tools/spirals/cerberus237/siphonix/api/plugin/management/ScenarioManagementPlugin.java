package tools.spirals.cerberus237.siphonix.api.plugin.management;

import tools.spirals.cerberus237.siphonix.api.plugin.Plugin;

/**
 * Specialization of {@link Plugin} exposing scenario-management capabilities.
 * <p>
 * This contract groups service-level, REST-facing, and CLI-facing entry points behind one plugin
 * abstraction so host applications can wire whichever control plane they need.
 * </p>
 *
 * @author Arléon Zemtsop (Cerberus)
 */
public interface ScenarioManagementPlugin extends Plugin {

    /**
     * Returns the core scenario management service.
     *
     * @return service entry point for programmatic scenario lifecycle operations
     */
    ScenarioManagementService getScenarioManagementService();

    /**
     * Returns the REST API facade used by HTTP transport layers.
     *
     * @return REST-facing scenario management endpoint contract
     */
    ScenarioManagementRestApi getScenarioManagementRestApi();

    /**
     * Returns the CLI facade used by command-line integrations.
     *
     * @return command-line scenario management entry point
     */
    ScenarioManagementCli getScenarioManagementCli();
}
