package tools.spirals.cerberus237.siphonix;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public class RemoteServiceReadinessCheckerTest {

    private HttpServer server;
    private RemoteServiceReadinessChecker checker;

    @Before
    public void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ready", exchange -> respond(exchange, 204));
        server.createContext("/not-ready", exchange -> respond(exchange, 503));
        server.start();
        checker = new RemoteServiceReadinessChecker(1L);
    }

    @After
    public void tearDown() {
        server.stop(0);
    }

    @Test
    public void acceptsAnySuccessfulHttpStatus() throws InterruptedException {
        assertTrue(checker.isReady(uri("/ready")));
    }

    @Test
    public void rejectsNonSuccessfulHttpStatus() throws InterruptedException {
        assertFalse(checker.isReady(uri("/not-ready")));
    }

    @Test
    public void waitsUntilEndpointBecomesReady() throws InterruptedException {
        AtomicInteger attempts = new AtomicInteger();
        server.createContext("/eventually-ready", exchange ->
                respond(exchange, attempts.incrementAndGet() == 1 ? 503 : 204));

        checker.awaitReady(uri("/eventually-ready").toString());

        assertEquals(2, attempts.get());
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + path);
    }

    private static void respond(HttpExchange exchange, int status) throws IOException {
        exchange.sendResponseHeaders(status, -1);
        exchange.close();
    }
}
