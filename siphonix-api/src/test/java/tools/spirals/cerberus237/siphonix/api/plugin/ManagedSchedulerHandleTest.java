package tools.spirals.cerberus237.siphonix.api.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ManagedSchedulerHandleTest {

    @Test
    public void startInvokesStartMethodWhenPresent() {
        StartableScheduler scheduler = new StartableScheduler();

        ManagedSchedulerHandle handle = new ManagedSchedulerHandle(scheduler);
        handle.start();

        assertTrue("Expected start() to be invoked", scheduler.started);
    }

    @Test
    public void startDoesNothingWhenMethodIsAbsent() {
        ManagedSchedulerHandle handle = new ManagedSchedulerHandle(new NoLifecycleScheduler());

        handle.start();
    }

    @Test
    public void stopPrefersStopMethodWhenMultipleCandidatesExist() {
        MultiStopScheduler scheduler = new MultiStopScheduler();

        ManagedSchedulerHandle handle = new ManagedSchedulerHandle(scheduler);
        handle.stop();

        assertEquals("Expected only stop() to be called", "stop", scheduler.lastCall);
        assertEquals("Expected single lifecycle call", 1, scheduler.callCount);
    }

    @Test
    public void stopFallsBackToShutdownWhenStopMethodIsMissing() {
        ShutdownOnlyScheduler scheduler = new ShutdownOnlyScheduler();

        ManagedSchedulerHandle handle = new ManagedSchedulerHandle(scheduler);
        handle.stop();

        assertTrue("Expected shutdown() to be invoked", scheduler.shutdownCalled);
    }

    @Test
    public void stopDoesNothingWhenNoSupportedMethodExists() {
        ManagedSchedulerHandle handle = new ManagedSchedulerHandle(new NoLifecycleScheduler());

        handle.stop();
    }

    @Test
    public void startWrapsUnderlyingInvocationFailure() {
        ManagedSchedulerHandle handle = new ManagedSchedulerHandle(new FailingStartScheduler());

        IllegalStateException ex = assertThrows(IllegalStateException.class, handle::start);

        assertTrue("Expected method name in error message", ex.getMessage().contains("start"));
    }

    @Test
    public void stopWrapsUnderlyingInvocationFailure() {
        ManagedSchedulerHandle handle = new ManagedSchedulerHandle(new FailingShutdownScheduler());

        IllegalStateException ex = assertThrows(IllegalStateException.class, handle::stop);

        assertTrue("Expected method name in error message", ex.getMessage().contains("shutdown"));
    }

    public static class StartableScheduler {
        private boolean started;

        public void start() {
            started = true;
        }
    }

    public static class NoLifecycleScheduler {
    }

    public static class MultiStopScheduler {
        private String lastCall;
        private int callCount;

        public void stop() {
            lastCall = "stop";
            callCount++;
        }

        public void shutdown() {
            lastCall = "shutdown";
            callCount++;
        }
    }

    public static class ShutdownOnlyScheduler {
        private boolean shutdownCalled;

        public void shutdown() {
            shutdownCalled = true;
        }
    }

    public static class FailingStartScheduler {
        public void start() {
            throw new RuntimeException("boom-start");
        }
    }

    public static class FailingShutdownScheduler {
        public void shutdown() {
            throw new RuntimeException("boom-shutdown");
        }
    }
}
