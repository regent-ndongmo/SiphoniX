package tools.spirals.cerberus237.siphonix.plugins.adaptiflow.legacy;

import tools.spirals.cerberus237.siphonix.plugins.adaptiflow.legacy.observations.BeninTrafficObservation;

public class BeninTrafficObservationPlugin extends AbstractObservationPlugin {

    @Override
    public String getId() {
        return "adaptiflow.benin-traffic";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    protected Class<?> getObservationClass() {
        return BeninTrafficObservation.class;
    }

    @Override
    protected String getSchedulerFieldName() {
        return "beninTrafficObservationScheduler";
    }
}
