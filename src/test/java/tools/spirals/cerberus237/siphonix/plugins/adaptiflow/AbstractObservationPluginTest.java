package tools.spirals.cerberus237.siphonix.plugins.adaptiflow;

import org.junit.Assert;
import org.junit.Test;

import tools.spirals.cerberus237.siphonix.kernel.DefaultPluginContext;
import tools.spirals.cerberus237.siphonix.kernel.PluginContext;
import tools.spirals.cerberus237.siphonix.kernel.PluginState;
import tools.spirals.cerberus237.siphonix.plugins.adaptiflow.legacy.AbstractObservationPlugin;

public class AbstractObservationPluginTest {

    @Test
    public void shouldTransitionThroughLifecycleStates() {
        FakeObservation.scheduler = null;
        TestObservationPlugin plugin = new TestObservationPlugin(FakeObservation.class, "scheduler");
        PluginContext context = new DefaultPluginContext();

        plugin.initialize(context);
        plugin.start();
        plugin.stop();

        Assert.assertEquals(PluginState.STOPPED, plugin.getState());
        Assert.assertTrue(FakeScheduler.started);
        Assert.assertTrue(FakeScheduler.stopped);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldFailInitializationWhenSchedulerFieldMissing() {
        TestObservationPlugin plugin = new TestObservationPlugin(FakeObservation.class, "missingSchedulerField");

        plugin.initialize(new DefaultPluginContext());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldRefuseStartWhenNotInitialized() {
        TestObservationPlugin plugin = new TestObservationPlugin(FakeObservation.class, "scheduler");

        plugin.start();
    }

    public static class FakeObservation {
        public static FakeScheduler scheduler;

        public static FakeObservation getInstance() {
            scheduler = new FakeScheduler();
            return new FakeObservation();
        }
    }

    public static class FakeScheduler {
        static boolean started;
        static boolean stopped;

        FakeScheduler() {
            started = false;
            stopped = false;
        }

        public void start() {
            started = true;
        }

        public void stop() {
            stopped = true;
        }
    }

    private static class TestObservationPlugin extends AbstractObservationPlugin {
        private final Class<?> observationClass;
        private final String schedulerFieldName;

        TestObservationPlugin(Class<?> observationClass, String schedulerFieldName) {
            this.observationClass = observationClass;
            this.schedulerFieldName = schedulerFieldName;
        }

        @Override
        public String getId() {
            return "test-plugin";
        }

        @Override
        public String getVersion() {
            return "test";
        }

        @Override
        protected Class<?> getObservationClass() {
            return observationClass;
        }

        @Override
        protected String getSchedulerFieldName() {
            return schedulerFieldName;
        }
    }
}
