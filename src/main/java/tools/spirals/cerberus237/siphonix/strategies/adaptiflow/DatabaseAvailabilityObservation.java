package tools.spirals.cerberus237.siphonix.strategies.adaptiflow;

import java.util.HashMap;
import java.util.List;

import tools.spirals.cerberus237.adaptationactionsbase.core.IAdaptationAction;
import tools.spirals.cerberus237.adaptationactionsbase.core.RestAdaptationAction;
import tools.spirals.cerberus237.adaptationactionsbase.docker.DockerActionFactory;
import tools.spirals.cerberus237.adaptiflow.events.ConditionalEvent;
import tools.spirals.cerberus237.adaptiflow.interfaces.Observer;
import tools.spirals.cerberus237.adaptiflow.operators.DecreaseResourceUsageEvaluator;
import tools.spirals.cerberus237.adaptiflow.operators.IncreaseResourceUsageEvaluator;
import tools.spirals.cerberus237.adaptiflow.subscriptions.ContinuousObservationScheduler;
import tools.spirals.cerberus237.adaptiflow.subscriptions.subscribers.EventSubscriber;
import tools.spirals.cerberus237.metricscollectorbase.IMetricsCollector;
import tools.spirals.cerberus237.metricscollectorbase.metrics.cpu.ResourceUsageCollector;

public class DatabaseAvailabilityObservation {
    public static final int EVENT_LISTENING_INTERVAL_MS = 5000;
    public static ContinuousObservationScheduler databaseAvailabilityObservationScheduler = null;
    private static final String TARGET_SERVICE_URL = System.getenv().getOrDefault("TARGET_URL", "http://adaptable-teastore-image:8080/tools.descartes.teastore.image/rest");

    private static DatabaseAvailabilityObservation instance = null;

    private DatabaseAvailabilityObservation() {
        setupDatabaseAvailabilityObservation();
    }

    /**
     * Returns the instance for this singleton.
     *
     * @return An instance of {@link DatabaseAvailabilityObservation}
     */
    public static DatabaseAvailabilityObservation getInstance() {
        if (instance == null) {
            instance = new DatabaseAvailabilityObservation();
        }
        return instance;
    }

    public void setupDatabaseAvailabilityObservation() {
        List<IAdaptationAction> trafficIncreaseActionList = List.of(new RestAdaptationAction(List.of("DatabaseAvailableEventBroadcast"), TARGET_SERVICE_URL + "/adapt", "EnableExternalImageProvider"));
        List<IAdaptationAction> trafficDecreaseActionList = List.of(new RestAdaptationAction(List.of("DatabaseUnavailableEventBroadcast"), TARGET_SERVICE_URL + "/adapt", "DisableExternalImageProvider"), new DockerActionFactory().unpauseContainer("adaptable_teastore_db"));

        List<Observer<HashMap<String, Double>>> trafficIncreaseEventSubscriberList = List
                .of(new EventSubscriber<>(trafficIncreaseActionList));
        List<Observer<HashMap<String, Double>>> trafficDecreaseEventSubscriberList = List
                .of(new EventSubscriber<>(trafficDecreaseActionList));

        IMetricsCollector<HashMap<String, Double>> collector = new ResourceUsageCollector();

        ConditionalEvent<HashMap<String, Double>> trafficIncreaseEvent = new ConditionalEvent<>(collector,
                new IncreaseResourceUsageEvaluator(() -> 75.0, () -> 80.0));
        ConditionalEvent<HashMap<String, Double>> trafficDecreaseEvent = new ConditionalEvent<>(collector,
                new DecreaseResourceUsageEvaluator(() -> 60.0, () -> 60.0));

        trafficIncreaseEvent.subscribeAll(trafficIncreaseEventSubscriberList);
        trafficDecreaseEvent.subscribeAll(trafficDecreaseEventSubscriberList);

        databaseAvailabilityObservationScheduler = new ContinuousObservationScheduler(
                List.of(trafficIncreaseEvent, trafficDecreaseEvent), EVENT_LISTENING_INTERVAL_MS);
    }
}
