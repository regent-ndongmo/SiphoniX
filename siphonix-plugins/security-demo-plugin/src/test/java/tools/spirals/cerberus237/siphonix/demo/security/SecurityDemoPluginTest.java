package tools.spirals.cerberus237.siphonix.demo.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Test;

import com.sun.net.httpserver.HttpServer;

import tools.spirals.cerberus237.siphonix.api.plugin.PluginContext;
import tools.spirals.cerberus237.siphonix.api.plugin.PluginState;

public class SecurityDemoPluginTest {

    @After
    public void clearConfiguration() {
        System.clearProperty("siphonix.demo.root");
        System.clearProperty("siphonix.demo.confirm");
        System.clearProperty("siphonix.demo.target-url");
    }

    @Test
    public void provesFixtureAndCrossApplicationAccessWithoutDockerControl() throws Exception {
        Path root = createFixture(true);
        AtomicBoolean targetCalled = new AtomicBoolean();
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/admin/stop", exchange -> {
            targetCalled.set(true);
            Files.writeString(root.resolve("containers/target.status"),
                    "STOPPED_BY_UNTRUSTED_PLUGIN_HTTP\n", StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().close();
        });
        server.start();

        try {
            configure(root, "http://localhost:" + server.getAddress().getPort());
            SecurityDemoPlugin plugin = new SecurityDemoPlugin();
            plugin.initialize(new EmptyContext());
            plugin.start();

            assertEquals(PluginState.RUNNING, plugin.getState());
            assertTrue(targetCalled.get());
            assertEquals("STOPPED_BY_UNTRUSTED_PLUGIN_HTTP\n",
                    Files.readString(root.resolve("containers/target.status")));
            assertFalse(Files.exists(root.resolve("database/records.txt")));
            String report = Files.readString(root.resolve("security-demo.log"));
            assertTrue(report.contains("secretRead=SUCCEEDED"));
            assertTrue(report.contains("crossContainerHttpControl=SUCCEEDED"));
            assertTrue(report.contains("dockerSocketAction=NOT_ATTEMPTED_BY_DESIGN"));
            assertFalse(report.contains("DEMO_ONLY_FAKE_SECRET"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void refusesAnUnmarkedDirectoryEvenWhenConfirmed() throws Exception {
        Path root = createFixture(false);
        configure(root, "http://localhost:8080");
        SecurityDemoPlugin plugin = new SecurityDemoPlugin();

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> plugin.initialize(new EmptyContext()));

        assertTrue(exception.getMessage().contains("marker file is missing"));
        assertEquals(PluginState.FAILED, plugin.getState());
    }

    @Test
    public void refusesAConfiguredRemoteTarget() throws Exception {
        Path root = createFixture(true);
        configure(root, "http://example.test:8080");
        SecurityDemoPlugin plugin = new SecurityDemoPlugin();

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> plugin.initialize(new EmptyContext()));

        assertTrue(exception.getMessage().contains("Refusing non-local demo target"));
    }

    private Path createFixture(boolean withMarker) throws Exception {
        Path root = Files.createTempDirectory("siphonix-security-demo");
        Files.createDirectories(root.resolve("containers"));
        Files.createDirectories(root.resolve("database"));
        Files.createDirectories(root.resolve("secrets"));
        Files.writeString(root.resolve("containers/target.status"), "RUNNING\n");
        Files.writeString(root.resolve("database/records.txt"), "id=42\n");
        Files.writeString(root.resolve("secrets/demo.secret"), "DEMO_ONLY_FAKE_SECRET\n");
        if (withMarker) {
            Files.writeString(root.resolve(".siphonix-security-demo"), "disposable=true\n");
        }
        return root;
    }

    private void configure(Path root, String targetUrl) {
        System.setProperty("siphonix.demo.root", root.toString());
        System.setProperty("siphonix.demo.confirm", "YES");
        System.setProperty("siphonix.demo.target-url", targetUrl);
    }

    private static final class EmptyContext implements PluginContext {
        @Override
        public String getTargetServiceUrl() {
            return "http://localhost:8080";
        }
    }
}
