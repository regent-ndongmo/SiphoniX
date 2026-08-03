package tools.spirals.cerberus237.siphonix.api.plugin.management;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ScenarioManagementRestApiTest {

    @Test
    public void exposesExpectedEndpointPaths() {
        assertEquals("/api/scenarios", ScenarioManagementRestApi.CREATE_SCENARIO_PATH);
        assertEquals("/api/scenarios/{id}", ScenarioManagementRestApi.UPDATE_SCENARIO_PATH);
        assertEquals("/api/scenarios/{id}", ScenarioManagementRestApi.DELETE_SCENARIO_PATH);
        assertEquals("/api/scenarios/{id}/enable", ScenarioManagementRestApi.ENABLE_SCENARIO_PATH);
        assertEquals("/api/scenarios/{id}/disable", ScenarioManagementRestApi.DISABLE_SCENARIO_PATH);
    }
}
