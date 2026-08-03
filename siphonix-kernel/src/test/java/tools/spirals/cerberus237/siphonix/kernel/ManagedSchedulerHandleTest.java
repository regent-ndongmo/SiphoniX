package tools.spirals.cerberus237.siphonix.kernel;

import tools.spirals.cerberus237.siphonix.api.plugin.ManagedSchedulerHandle;

import org.junit.Assert;
import org.junit.Test;

public class ManagedSchedulerHandleTest {

    @Test
    public void shouldInvokeStartAndShutdownMethodsWhenPresent() {
        SchedulerWithShutdown scheduler = new SchedulerWithShutdown();
        ManagedSchedulerHandle handle = new ManagedSchedulerHandle(scheduler);

        handle.start();
        handle.stop();

        Assert.assertTrue(scheduler.started);
        Assert.assertTrue(scheduler.shutdown);
    }

    @Test
    public void shouldInvokeStopBeforeOtherFallbackMethods() {
        SchedulerWithStopAndShutdown scheduler = new SchedulerWithStopAndShutdown();
        ManagedSchedulerHandle handle = new ManagedSchedulerHandle(scheduler);

        handle.stop();

        Assert.assertTrue(scheduler.stopped);
        Assert.assertFalse(scheduler.shutdown);
    }

    @Test
    public void shouldIgnoreStopWhenNoCompatibleMethodExists() {
        SchedulerWithoutStop scheduler = new SchedulerWithoutStop();
        ManagedSchedulerHandle handle = new ManagedSchedulerHandle(scheduler);

        handle.stop();

        Assert.assertFalse(scheduler.started);
    }

    public static class SchedulerWithShutdown {
        boolean started;
        boolean shutdown;

        public void start() {
            this.started = true;
        }

        public void shutdown() {
            this.shutdown = true;
        }
    }

    public static class SchedulerWithStopAndShutdown {
        boolean stopped;
        boolean shutdown;

        public void stop() {
            this.stopped = true;
        }

        public void shutdown() {
            this.shutdown = true;
        }
    }

    public static class SchedulerWithoutStop {
        boolean started;

        public void start() {
            this.started = true;
        }
    }
}
