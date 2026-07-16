package tools.spirals.cerberus237.siphonix.plugins.adaptiflow;

import tools.spirals.cerberus237.siphonix.strategies.adaptiflow.DatabaseAvailabilityObservation;

public class DatabaseAvailabilityObservationPlugin extends AbstractObservationPlugin {

    @Override
    public String getId() {
        return "adaptiflow.database-availability";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    protected Class<?> getObservationClass() {
        return DatabaseAvailabilityObservation.class;
    }

    @Override
    protected String getSchedulerFieldName() {
        return "databaseAvailabilityObservationScheduler";
    }
}
