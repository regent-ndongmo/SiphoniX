package tools.spirals.cerberus237.siphonix;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Waits until the monitored HTTP service is ready before SiphoniX starts its plugins.
 */
final class RemoteServiceReadinessChecker {

    private static final Logger logger =
            LoggerFactory.getLogger(RemoteServiceReadinessChecker.class);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(3);
    private static final long RETRY_DELAY_MILLIS = 1000L;

    private final HttpClient httpClient;
    private final long retryDelayMillis;

    RemoteServiceReadinessChecker() {
        this(RETRY_DELAY_MILLIS);
    }

    RemoteServiceReadinessChecker(long retryDelayMillis) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        this.retryDelayMillis = retryDelayMillis;
    }

    void awaitReady(String readinessUrl) throws InterruptedException {
        URI readinessUri = toHttpUri(readinessUrl);
        logger.info("[Readiness] Waiting for {}", readinessUri);

        while (!isReady(readinessUri)) {
            Thread.sleep(retryDelayMillis);
        }

        logger.info("[Readiness] Monitored service is ready");
    }

    boolean isReady(URI readinessUri) throws InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(readinessUri)
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();
        try {
            int status = httpClient.send(
                    request, HttpResponse.BodyHandlers.discarding()).statusCode();
            if (status >= 200 && status < 300) {
                return true;
            }
            logger.info("[Readiness] Monitored service is not ready yet (HTTP {}); waiting",
                    status);
        } catch (IOException ex) {
            logger.info("[Readiness] Monitored service is not ready yet (unreachable: {}); waiting",
                    ex.getMessage());
        }
        return false;
    }

    private static URI toHttpUri(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("The readiness URL must not be blank");
        }

        URI uri = URI.create(value.trim());
        String scheme = uri.getScheme();
        if (uri.getHost() == null
                || (!"http".equalsIgnoreCase(scheme)
                && !"https".equalsIgnoreCase(scheme))) {
            throw new IllegalArgumentException(
                    "The readiness URL must be an absolute HTTP(S) URL: " + value);
        }
        return uri;
    }
}
