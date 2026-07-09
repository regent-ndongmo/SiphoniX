package tools.spirals.cerberus237.siphonix.strategies.adaptiflow;

import java.util.HashMap;
import java.util.List;

import tools.spirals.cerberus237.adaptationactionsbase.core.IAdaptationAction;
import tools.spirals.cerberus237.adaptationactionsbase.core.RestAdaptationAction;
import tools.spirals.cerberus237.adaptiflow.events.ConditionalEvent;
import tools.spirals.cerberus237.adaptiflow.interfaces.Observer;
import tools.spirals.cerberus237.adaptiflow.operators.DecreaseResourceUsageEvaluator;
import tools.spirals.cerberus237.adaptiflow.operators.IncreaseResourceUsageEvaluator;
import tools.spirals.cerberus237.adaptiflow.subscriptions.ContinuousObservationScheduler;
import tools.spirals.cerberus237.adaptiflow.subscriptions.subscribers.EventSubscriber;
import tools.spirals.cerberus237.metricscollectorbase.IMetricsCollector;
import tools.spirals.cerberus237.metricscollectorbase.metrics.cpu.ResourceUsageCollector;

public class BeninTrafficObservation {
    public static final int EVENT_LISTENING_INTERVAL_MS = 5000;
    public static ContinuousObservationScheduler beninTrafficObservationScheduler = null;
    private static final String TARGET_SERVICE_URL = System.getenv().getOrDefault("TARGET_URL", "http://image:8080/tools.descartes.teastore.image/rest");


    private static BeninTrafficObservation instance = null;

    private BeninTrafficObservation() {
        setupBeninTrafficObservation();
    }

    /**
     * Returns the instance for this singleton.
     *
     * @return An instance of {@link BeninTrafficObservation}
     */
    public static BeninTrafficObservation getInstance() {
        if (instance == null) {
            instance = new BeninTrafficObservation();
        }
        return instance;
    }

    public void setupBeninTrafficObservation() {
        List<IAdaptationAction> trafficIncreaseActionList = List.of(new RestAdaptationAction(List.of("EnableExternalImageProvider"), TARGET_SERVICE_URL + "/adapt", "EnableExternalImageProvider"));
        List<IAdaptationAction> trafficDecreaseActionList = List.of(new RestAdaptationAction(List.of("DisableExternalImageProvider"), TARGET_SERVICE_URL + "/adapt", "DisableExternalImageProvider"));

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

        beninTrafficObservationScheduler = new ContinuousObservationScheduler(
                List.of(trafficIncreaseEvent, trafficDecreaseEvent), EVENT_LISTENING_INTERVAL_MS);
    }
}
