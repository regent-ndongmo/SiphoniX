package tools.spirals.cerberus237.siphonix.kernel;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reflection-based scheduler handle to avoid compile-time coupling to a
 * specific stop method name in external scheduler libraries.
 */
public final class ManagedSchedulerHandle {

    private static final Logger LOG = LoggerFactory.getLogger(ManagedSchedulerHandle.class);
    private static final List<String> STOP_METHOD_CANDIDATES =
            Arrays.asList("stop", "shutdown", "close", "cancel", "interrupt");

    private final Object scheduler;

    public ManagedSchedulerHandle(Object scheduler) {
        this.scheduler = scheduler;
    }

    public void start() {
        invokeIfPresent("start");
    }

    public void stop() {
        for (String methodName : STOP_METHOD_CANDIDATES) {
            if (invokeIfPresent(methodName)) {
                return;
            }
        }
        LOG.warn("No compatible stop method found on scheduler type {}", scheduler.getClass().getName());
    }

    private boolean invokeIfPresent(String methodName) {
        try {
            Method method = scheduler.getClass().getMethod(methodName);
            method.invoke(scheduler);
            return true;
        } catch (NoSuchMethodException ex) {
            return false;
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new IllegalStateException(
                    "Failed to invoke method '" + methodName + "' on scheduler " + scheduler.getClass().getName(),
                    ex);
        }
    }
}
