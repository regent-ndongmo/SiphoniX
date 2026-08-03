package tools.spirals.cerberus237.adaptiflow.plugin.core.scenarios;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.spirals.cerberus237.siphonix.api.plugin.Plugin;
import tools.spirals.cerberus237.siphonix.api.plugin.PluginRegistryView;
import tools.spirals.cerberus237.siphonix.api.plugin.PluginState;
import tools.spirals.cerberus237.adaptiflow.plugin.core.config.AdaptiflowConfiguration;

/**
 * Applies scenario definitions onto registered plugins.
 */
public class ScenarioOrchestrator {

    private static final Logger LOG = LoggerFactory.getLogger(ScenarioOrchestrator.class);

    private final PluginRegistryView pluginRegistry;

    public ScenarioOrchestrator(PluginRegistryView pluginRegistry) {
        this.pluginRegistry = pluginRegistry;
    }

    public void applyConfiguration(AdaptiflowConfiguration configuration) {
        Map<String, Plugin> pluginsById = new HashMap<>();
        for (Plugin plugin : pluginRegistry.list()) {
            pluginsById.put(plugin.getId(), plugin);
        }

        Set<String> expectedRunningPluginIds = new HashSet<>();
        for (ScenarioDefinition scenario : configuration.getScenarios().values()) {
            String pluginId = scenario.getPluginId();
            Plugin plugin = pluginsById.get(pluginId);
            if (plugin == null) {
                LOG.warn("No registered plugin found for scenario '{}' with pluginId '{}'", scenario.getId(), pluginId);
                continue;
            }
            if (scenario.isEnabled()) {
                expectedRunningPluginIds.add(pluginId);
                if (plugin.getState() != PluginState.RUNNING) {
                    plugin.start();
                }
            }
        }

        for (Plugin plugin : pluginRegistry.list()) {
            if (!expectedRunningPluginIds.contains(plugin.getId()) && plugin.getState() == PluginState.RUNNING) {
                plugin.stop();
            }
        }
    }
}
