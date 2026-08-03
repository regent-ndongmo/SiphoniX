package tools.spirals.cerberus237.siphonix.api.plugin.management;

import tools.spirals.cerberus237.siphonix.api.plugin.Plugin;

public interface ScenarioManagementPlugin extends Plugin {

    ScenarioManagementService getScenarioManagementService();

    ScenarioManagementRestApi getScenarioManagementRestApi();

    ScenarioManagementCli getScenarioManagementCli();
}
