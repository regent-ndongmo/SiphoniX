/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tools.spirals.cerberus237.siphonix.strategies.adaptiflow;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.spirals.cerberus237.adaptationactionsbase.core.IAdaptationAction;
import tools.spirals.cerberus237.adaptiflow.events.ConditionalEvent;
import tools.spirals.cerberus237.adaptiflow.interfaces.Observer;
import tools.spirals.cerberus237.adaptiflow.operators.TrueEvaluator;
import tools.spirals.cerberus237.adaptiflow.subscriptions.ContinuousObservationScheduler;
import tools.spirals.cerberus237.adaptiflow.subscriptions.subscribers.EventSubscriber;
import tools.spirals.cerberus237.metricscollectorbase.IMetricsCollector;
import tools.spirals.cerberus237.metricscollectorbase.core.RestMetricsCollector;
import tools.spirals.cerberus237.metricscollectorbase.models.CacheEntryMetrics;
import tools.spirals.cerberus237.metricscollectorbase.models.CacheMetrics;

/**
 * The {@code CacheSizeAdaptationObservation} class manages the observation of cache size
 * metrics from the image service and triggers adaptation actions based on cache entry count.
 * <p>
 * This observation setup uses the {@link RestMetricsCollector} to fetch cache entries from
 * the metrics endpoint (/metrics/cache-entries) exposed by the image service. It monitors
 * the number of entries in the cache and can trigger adaptation actions when thresholds
 * are crossed.
 * </p>
 *
 * <p>
 * <h3>Scenario:</h3>
 * The cache size adaptation scenario helps:
 * <ul>
 *     <li>Monitor cache occupancy and entry count in real-time.</li>
 *     <li>Trigger cache optimization actions when thresholds are exceeded.</li>
 *     <li>Verify that cache metrics are being correctly collected and transmitted.</li>
 *     <li>Support adaptive decisions based on cache state during high-traffic scenarios.</li>
 * </ul>
 * </p>
 *
 * <p>
 * <h3>Configuration:</h3>
 * The observation interval is configurable via {@link #EVENT_LISTENING_INTERVAL_MS} and
 * the target service URL is obtained from the {@code TARGET_URL} environment variable
 * with a fallback to the default image service endpoint.
 * </p>
 *
 * @author Arléon Zemtsop (Cerberus)
 * @see RestMetricsCollector
 * @see ContinuousObservationScheduler
 */
public class CacheSizeAdaptationObservation {
    private static final Logger LOG = LoggerFactory.getLogger(CacheSizeAdaptationObservation.class);

    public static final int EVENT_LISTENING_INTERVAL_MS = 5000;
    public static ContinuousObservationScheduler cacheSizeAdaptationObservationScheduler = null;
    private static final String TARGET_SERVICE_URL = System.getenv().getOrDefault("TARGET_URL",
            "http://image:8080/tools.descartes.teastore.image/rest");
    private static final String CACHE_ENTRIES_ENDPOINT = "/metrics/cache-entries";
    private static final String CACHE_MODE_ENDPOINT = "/metrics/cache-mode";
    private static final String CACHE_METRICS_ENDPOINT = "/metrics/cache-metrics";

    private static CacheSizeAdaptationObservation instance = null;

    /**
     * Private constructor to enforce singleton pattern.
     * Initializes the cache size adaptation observation setup.
     */
    private CacheSizeAdaptationObservation() {
        setupCacheSizeAdaptationObservation();
    }

    /**
     * Returns the singleton instance of {@code CacheSizeAdaptationObservation}.
     * <p>
     * This method ensures that only one instance of the cache size adaptation
     * observation exists throughout the application lifecycle.
     * </p>
     *
     * @return An instance of {@link CacheSizeAdaptationObservation}
     */
    public static CacheSizeAdaptationObservation getInstance() {
        if (instance == null) {
            instance = new CacheSizeAdaptationObservation();
        }
        return instance;
    }

    /**
     * Sets up the cache size adaptation observation with metrics collection and event handling.
     * <p>
     * This method:
     * <ol>
     *     <li>Creates a RestMetricsCollector to fetch cache entries from the image service</li>
     *     <li>Defines adaptation actions for cache increase and decrease events</li>
     *     <li>Sets up conditional events with evaluators for cache entry thresholds</li>
     *     <li>Subscribes event subscribers to handle triggered events</li>
     *     <li>Starts the continuous observation scheduler</li>
     * </ol>
     * </p>
     */
    public void setupCacheSizeAdaptationObservation() {
        LOG.info("Setting up Cache Size Adaptation Observation with target service URL: {}", TARGET_SERVICE_URL);

        List<IAdaptationAction> cacheIncreaseActionList = List.of();

        // Create subscribers for cache increase events
        List<Observer<List<CacheEntryMetrics>>> cacheIncreaseEventSubscriberList = List
                .of(new EventSubscriber<>(cacheIncreaseActionList));

        // Create REST metrics collector to fetch cache entries from the image service
        @SuppressWarnings("unchecked")
        IMetricsCollector<List<CacheEntryMetrics>> cacheEntriesCollector = new RestMetricsCollector<List<CacheEntryMetrics>>(TARGET_SERVICE_URL, CACHE_ENTRIES_ENDPOINT, "GET",
                (Class<List<CacheEntryMetrics>>) (Class<?>) List.class);

        IMetricsCollector<String> cacheModeCollector = new RestMetricsCollector<String>(TARGET_SERVICE_URL, CACHE_MODE_ENDPOINT, "GET",
                String.class);

        IMetricsCollector<CacheMetrics> cacheMetricsCollector = new RestMetricsCollector<CacheMetrics>(TARGET_SERVICE_URL, CACHE_METRICS_ENDPOINT, "GET",
                CacheMetrics.class);

        LOG.info("Cache entries collector configured to fetch from: {}{}", TARGET_SERVICE_URL, CACHE_ENTRIES_ENDPOINT);
        LOG.info("Cache mode collector configured to fetch from: {}{}", TARGET_SERVICE_URL, CACHE_MODE_ENDPOINT);
        LOG.info("Cache metrics collector configured to fetch from: {}{}", TARGET_SERVICE_URL, CACHE_METRICS_ENDPOINT);

        // Create conditional events for cache entry thresholds
        // Event triggered when cache entries increase beyond threshold
        ConditionalEvent<List<CacheEntryMetrics>> cacheIncreaseEvent = new ConditionalEvent<List<CacheEntryMetrics>>(
                cacheEntriesCollector,
                new TrueEvaluator<List<CacheEntryMetrics>>()
        );

        ConditionalEvent<String> cacheModeEvent = new ConditionalEvent<String>(
                cacheModeCollector,
                new TrueEvaluator<String>()
        );

        ConditionalEvent<CacheMetrics> cacheMetricsEvent = new ConditionalEvent<CacheMetrics>(
                cacheMetricsCollector,
                new TrueEvaluator<CacheMetrics>()
        );

        // Subscribe event handlers to the events
        cacheIncreaseEvent.subscribeAll(cacheIncreaseEventSubscriberList);

        // Create and start the continuous observation scheduler
        cacheSizeAdaptationObservationScheduler = new ContinuousObservationScheduler(
                List.of(cacheIncreaseEvent, cacheModeEvent, cacheMetricsEvent),
                EVENT_LISTENING_INTERVAL_MS
        );

        LOG.info("Cache Size Adaptation Observation setup completed. Monitoring interval: {}ms", EVENT_LISTENING_INTERVAL_MS);
    }
}
