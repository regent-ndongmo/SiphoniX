package tools.spirals.cerberus237.siphonix.api.plugin.management;

import java.io.IOException;

/**
 * REST-facing contract for scenario management endpoints.
 * <p>
 * This interface defines canonical endpoint paths and corresponding operations that can be mapped
 * by transport adapters such as JAX-RS, Spring MVC, or custom HTTP handlers.
 * </p>
 *
 * @author Arléon Zemtsop (Cerberus)
 */
public interface ScenarioManagementRestApi {

    /**
     * Endpoint path for scenario creation requests.
     */
    String CREATE_SCENARIO_PATH = "/api/scenarios";
    /**
     * Endpoint path for scenario update requests.
     */
    String UPDATE_SCENARIO_PATH = "/api/scenarios/{id}";
    /**
     * Endpoint path for scenario deletion requests.
     */
    String DELETE_SCENARIO_PATH = "/api/scenarios/{id}";
    /**
     * Endpoint path for scenario activation requests.
     */
    String ENABLE_SCENARIO_PATH = "/api/scenarios/{id}/enable";
    /**
     * Endpoint path for scenario deactivation requests.
     */
    String DISABLE_SCENARIO_PATH = "/api/scenarios/{id}/disable";

    /**
     * Handles scenario creation through the REST layer.
     *
     * @param source scenario payload source abstraction
     * @throws IOException when request payload cannot be read or creation fails
     */
    void postScenario(ScenarioSource source) throws IOException;

    /**
     * Handles scenario update through the REST layer.
     *
     * @param scenarioId path identifier of the scenario to update
     * @param source scenario payload source abstraction
     * @throws IOException when payload processing or update fails
     */
    void putScenario(String scenarioId, ScenarioSource source) throws IOException;

    /**
     * Handles scenario deletion through the REST layer.
     *
     * @param scenarioId path identifier of the scenario to delete
     */
    void deleteScenario(String scenarioId);

    /**
     * Handles scenario enablement through the REST layer.
     *
     * @param scenarioId path identifier of the scenario to enable
     */
    void enableScenario(String scenarioId);

    /**
     * Handles scenario disablement through the REST layer.
     *
     * @param scenarioId path identifier of the scenario to disable
     */
    void disableScenario(String scenarioId);
}
