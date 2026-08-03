package tools.spirals.cerberus237.siphonix.api.plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reflection-based adapter for scheduler-like objects with heterogeneous lifecycle APIs.
 * <p>
 * This handle decouples plugin code from concrete scheduler implementations by discovering
 * lifecycle methods at runtime. It supports a strict {@code start()} invocation and a tolerant
 * shutdown strategy based on well-known method names.
 * </p>
 * <p>
 * It is particularly useful when plugin dependencies expose different stop semantics
 * (for example {@code shutdown()}, {@code close()}, or {@code cancel()}) and direct compile-time
 * binding is not desirable.
 * </p>
 *
 * @author Arléon Zemtsop (Cerberus)
 */
public final class ManagedSchedulerHandle {

    private static final Logger LOG = LoggerFactory.getLogger(ManagedSchedulerHandle.class);
    private static final List<String> STOP_METHOD_CANDIDATES =
            Arrays.asList("stop", "shutdown", "close", "cancel", "interrupt");

    private final Object scheduler;

    /**
     * Creates a handle around a scheduler-like object.
     *
     * @param scheduler target object exposing lifecycle methods through public no-arg methods
     */
    public ManagedSchedulerHandle(Object scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * Starts the wrapped scheduler by invoking a public no-argument {@code start} method.
     * <p>
     * If the method does not exist, the call is a no-op. If invocation fails, an
     * {@link IllegalStateException} is thrown.
     * </p>
     */
    public void start() {
        invokeIfPresent("start");
    }

    /**
     * Stops the wrapped scheduler by trying a sequence of common stop method names.
     * <p>
     * Method candidates are tried in declaration order. If none is available, a warning is logged
     * and execution continues without throwing.
     * </p>
     */
    public void stop() {
        for (String methodName : STOP_METHOD_CANDIDATES) {
            if (invokeIfPresent(methodName)) {
                return;
            }
        }
        LOG.warn("No compatible stop method found on scheduler type {}", scheduler.getClass().getName());
    }

    /**
     * Invokes a public no-argument method on the wrapped scheduler when present.
     *
     * @param methodName method name to resolve and invoke
     * @return {@code true} when the method exists and was invoked, {@code false} when it does not
     *         exist
     * @throws IllegalStateException when reflection invocation fails after method resolution
     */
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