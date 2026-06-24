package tools.spirals.cerberus237.siphonix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.spirals.cerberus237.siphonix.strategies.adaptiflow.BeninTrafficObservation;
import tools.spirals.cerberus237.siphonix.strategies.adaptiflow.DatabaseAvailabilityObservation;

public class SiphoniX {

    protected static final Logger logger = LoggerFactory.getLogger(SiphoniX.class);

    private static final String TARGET_SERVICE_URL = System.getenv().getOrDefault("TARGET_URL", "http://adaptable-teastore-image:8080/tools.descartes.teastore.image/rest");
    
    public static void main(String[] args) {
        logger.info("[SiphoniX] Starting Autonomic Manager Sidecar...");
        logger.info("[SiphoniX] Monitoring Target: {}", TARGET_SERVICE_URL);

        if (BeninTrafficObservation.beninTrafficObservationScheduler == null)
            BeninTrafficObservation.getInstance();
        BeninTrafficObservation.beninTrafficObservationScheduler.start();
        logger.info("[SiphoniX] Traffic Surge Observation Start");

        if (DatabaseAvailabilityObservation.databaseAvailabilityObservationScheduler == null)
            DatabaseAvailabilityObservation.getInstance();
        DatabaseAvailabilityObservation.databaseAvailabilityObservationScheduler.start();
        logger.info("[SiphoniX] Database Availability Observation Start");
    }
}