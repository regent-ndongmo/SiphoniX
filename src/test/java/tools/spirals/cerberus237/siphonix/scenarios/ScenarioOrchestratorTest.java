package tools.spirals.cerberus237.siphonix.scenarios;

import org.junit.Assert;
import org.junit.Test;

import tools.spirals.cerberus237.siphonix.config.SiphonixConfiguration;
import tools.spirals.cerberus237.siphonix.kernel.DefaultPluginContext;
import tools.spirals.cerberus237.siphonix.kernel.Plugin;
import tools.spirals.cerberus237.siphonix.kernel.PluginContext;
import tools.spirals.cerberus237.siphonix.kernel.PluginRegistry;
import tools.spirals.cerberus237.siphonix.kernel.PluginState;

public class ScenarioOrchestratorTest {

    @Test
    public void shouldStartEnabledScenarioPluginsAndStopOthers() {
        PluginRegistry registry = new PluginRegistry();
        RecordingPlugin cachePlugin = new RecordingPlugin("adaptiflow.cache-size");
        RecordingPlugin dbPlugin = new RecordingPlugin("adaptiflow.database-availability");
        registry.register(cachePlugin);
        registry.register(dbPlugin);
        registry.initializeAll(new DefaultPluginContext());

        dbPlugin.start();
        Assert.assertEquals(PluginState.RUNNING, dbPlugin.getState());

        SiphonixConfiguration configuration = new SiphonixConfiguration();

        ScenarioDefinition enabledScenario = new ScenarioDefinition();
        enabledScenario.setId("cache-observation");
        enabledScenario.setPluginId("adaptiflow.cache-size");
        enabledScenario.setEnabled(true);

        ScenarioDefinition disabledScenario = new ScenarioDefinition();
        disabledScenario.setId("db-observation");
        disabledScenario.setPluginId("adaptiflow.database-availability");
        disabledScenario.setEnabled(false);

        configuration.getScenarios().put(enabledScenario.getId(), enabledScenario);
        configuration.getScenarios().put(disabledScenario.getId(), disabledScenario);

        ScenarioOrchestrator orchestrator = new ScenarioOrchestrator(registry);
        orchestrator.applyConfiguration(configuration);

        Assert.assertEquals(PluginState.RUNNING, cachePlugin.getState());
        Assert.assertEquals(PluginState.STOPPED, dbPlugin.getState());
        Assert.assertEquals(1, cachePlugin.startCalls);
        Assert.assertEquals(1, dbPlugin.stopCalls);
    }

    @Test
    public void shouldNotStartTheSamePluginTwiceWhenMultipleScenariosUseIt() {
        PluginRegistry registry = new PluginRegistry();
        RecordingPlugin cachePlugin = new RecordingPlugin("adaptiflow.cache-size");
        registry.register(cachePlugin);
        registry.initializeAll(new DefaultPluginContext());

        SiphonixConfiguration configuration = new SiphonixConfiguration();

        ScenarioDefinition scenarioA = new ScenarioDefinition();
        scenarioA.setId("cache-observation-a");
        scenarioA.setPluginId("adaptiflow.cache-size");
        scenarioA.setEnabled(true);

        ScenarioDefinition scenarioB = new ScenarioDefinition();
        scenarioB.setId("cache-observation-b");
        scenarioB.setPluginId("adaptiflow.cache-size");
        scenarioB.setEnabled(true);

        configuration.getScenarios().put(scenarioA.getId(), scenarioA);
        configuration.getScenarios().put(scenarioB.getId(), scenarioB);

        ScenarioOrchestrator orchestrator = new ScenarioOrchestrator(registry);
        orchestrator.applyConfiguration(configuration);

        Assert.assertEquals(1, cachePlugin.startCalls);
        Assert.assertEquals(PluginState.RUNNING, cachePlugin.getState());
    }

    private static class RecordingPlugin implements Plugin {
        private final String id;
        private PluginState state = PluginState.CREATED;
        private int startCalls;
        private int stopCalls;

        RecordingPlugin(String id) {
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getVersion() {
            return "test";
        }

        @Override
        public PluginState getState() {
            return state;
        }

        @Override
        public void initialize(PluginContext context) {
            state = PluginState.INITIALIZED;
        }

        @Override
        public void start() {
            startCalls++;
            state = PluginState.RUNNING;
        }

        @Override
        public void stop() {
            stopCalls++;
            state = PluginState.STOPPED;
        }
    }
}
