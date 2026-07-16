package tools.spirals.cerberus237.siphonix.plugins.adaptiflow;

import tools.spirals.cerberus237.siphonix.strategies.adaptiflow.CacheSizeAdaptationObservation;

public class CacheSizeObservationPlugin extends AbstractObservationPlugin {

    @Override
    public String getId() {
        return "adaptiflow.cache-size";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    protected Class<?> getObservationClass() {
        return CacheSizeAdaptationObservation.class;
    }

    @Override
    protected String getSchedulerFieldName() {
        return "cacheSizeAdaptationObservationScheduler";
    }
}
