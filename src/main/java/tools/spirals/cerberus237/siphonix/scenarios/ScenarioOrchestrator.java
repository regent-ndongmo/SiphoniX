package tools.spirals.cerberus237.siphonix.scenarios;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.spirals.cerberus237.siphonix.config.SiphonixConfiguration;
import tools.spirals.cerberus237.siphonix.kernel.Plugin;
import tools.spirals.cerberus237.siphonix.kernel.PluginRegistry;
import tools.spirals.cerberus237.siphonix.kernel.PluginState;

/**
 * Applies scenario definitions onto registered plugins.
 */
public class ScenarioOrchestrator {

    private static final Logger LOG = LoggerFactory.getLogger(ScenarioOrchestrator.class);

    private final PluginRegistry pluginRegistry;

    public ScenarioOrchestrator(PluginRegistry pluginRegistry) {
        this.pluginRegistry = pluginRegistry;
    }

    public void applyConfiguration(SiphonixConfiguration configuration) {
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
