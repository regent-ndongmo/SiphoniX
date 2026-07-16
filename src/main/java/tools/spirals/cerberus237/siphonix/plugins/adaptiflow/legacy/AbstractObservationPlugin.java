package tools.spirals.cerberus237.siphonix.plugins.adaptiflow.legacy;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import tools.spirals.cerberus237.siphonix.kernel.ManagedSchedulerHandle;
import tools.spirals.cerberus237.siphonix.kernel.Plugin;
import tools.spirals.cerberus237.siphonix.kernel.PluginContext;
import tools.spirals.cerberus237.siphonix.kernel.PluginState;

/**
 * Reflection-backed adapter for legacy observation classes still using
 * singleton/static scheduler patterns.
 */
public abstract class AbstractObservationPlugin implements Plugin {

    private PluginState state = PluginState.CREATED;
    private ManagedSchedulerHandle schedulerHandle;

    @Override
    public PluginState getState() {
        return state;
    }

    @Override
    public void initialize(PluginContext context) {
        try {
            Method getInstanceMethod = getObservationClass().getMethod("getInstance");
            getInstanceMethod.invoke(null);

            Field schedulerField = getObservationClass().getField(getSchedulerFieldName());
            Object scheduler = schedulerField.get(null);
            if (scheduler == null) {
                throw new IllegalStateException("Scheduler field '" + getSchedulerFieldName() + "' is null");
            }
            schedulerHandle = new ManagedSchedulerHandle(scheduler);
            state = PluginState.INITIALIZED;
        } catch (NoSuchMethodException | NoSuchFieldException | IllegalAccessException | InvocationTargetException ex) {
            state = PluginState.FAILED;
            throw new IllegalStateException("Failed to initialize plugin " + getId(), ex);
        }
    }

    @Override
    public void start() {
        if (schedulerHandle == null) {
            throw new IllegalStateException("Plugin " + getId() + " is not initialized");
        }
        schedulerHandle.start();
        state = PluginState.RUNNING;
    }

    @Override
    public void stop() {
        if (schedulerHandle == null) {
            return;
        }
        schedulerHandle.stop();
        state = PluginState.STOPPED;
    }

    protected abstract Class<?> getObservationClass();

    protected abstract String getSchedulerFieldName();
}
