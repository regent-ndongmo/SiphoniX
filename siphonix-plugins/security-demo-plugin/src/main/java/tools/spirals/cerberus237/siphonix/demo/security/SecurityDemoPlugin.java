package tools.spirals.cerberus237.siphonix.demo.security;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import tools.spirals.cerberus237.siphonix.api.plugin.Plugin;
import tools.spirals.cerberus237.siphonix.api.plugin.PluginContext;
import tools.spirals.cerberus237.siphonix.api.plugin.PluginState;

/**
 * Demonstrates the privileges inherited by an untrusted plugin using a disposable fixture.
 *
 * <p>The probe is deliberately constrained: it requires an explicit confirmation and a marker
 * file, accepts only a local demo target, and never invokes the Docker API. It still proves that
 * plugin code can read files, delete application data, inspect processes and control another
 * container over the compose network.</p>
 */
public final class SecurityDemoPlugin implements Plugin {
    private static final String ROOT_ENV = "SIPHONIX_DEMO_ROOT";
    private static final String CONFIRM_ENV = "SIPHONIX_DEMO_CONFIRM";
    private static final String TARGET_ENV = "SIPHONIX_DEMO_TARGET_URL";
    private static final String ROOT_PROPERTY = "siphonix.demo.root";
    private static final String CONFIRM_PROPERTY = "siphonix.demo.confirm";
    private static final String TARGET_PROPERTY = "siphonix.demo.target-url";
    private static final String MARKER_FILE = ".siphonix-security-demo";
    private static final Set<String> ALLOWED_TARGET_HOSTS = Set.of(
            "target", "localhost", "127.0.0.1", "::1", "0:0:0:0:0:0:0:1");

    private PluginState state = PluginState.CREATED;
    private Path root;
    private URI targetUri;

    @Override public String getId() { return "demo-security-plugin"; }
    @Override public String getVersion() { return "2.0.0"; }
    @Override public PluginState getState() { return state; }

    @Override
    public void initialize(PluginContext context) {
        String configuredRoot = configuredValue(ROOT_PROPERTY, ROOT_ENV, null);
        if (configuredRoot == null || configuredRoot.isBlank()) {
            fail(ROOT_ENV + " (or -D" + ROOT_PROPERTY + ") is required");
        }

        if (!"YES".equals(configuredValue(CONFIRM_PROPERTY, CONFIRM_ENV, null))) {
            fail("Refusing demo: set " + CONFIRM_ENV + "=YES only for the disposable fixture");
        }

        try {
            root = Path.of(configuredRoot).toRealPath();
        } catch (IOException exception) {
            fail("Cannot resolve demo root: " + configuredRoot, exception);
        }

        Path marker = root.resolve(MARKER_FILE);
        if (!Files.isRegularFile(marker, LinkOption.NOFOLLOW_LINKS)) {
            fail("Refusing demo: marker file is missing: " + marker);
        }

        String configuredTarget = configuredValue(TARGET_PROPERTY, TARGET_ENV, context.getTargetServiceUrl());
        try {
            targetUri = URI.create(configuredTarget);
        } catch (IllegalArgumentException exception) {
            fail("Invalid demo target URL: " + configuredTarget, exception);
        }
        requireAllowedDemoTarget(targetUri);
        state = PluginState.INITIALIZED;
    }

    @Override
    public void start() {
        List<String> findings = new ArrayList<>();
        try {
            findings.add("simulationOnly=true");
            findings.add("pluginPid=" + ProcessHandle.current().pid());
            findings.add("visibleProcesses=" + ProcessHandle.allProcesses().count());

            Path fakeSecret = safeFixturePath("secrets/demo.secret");
            byte[] secret = Files.readAllBytes(fakeSecret);
            findings.add("secretRead=SUCCEEDED");
            findings.add("secretSha256=" + sha256(secret));

            HttpResponse<String> response = requestTargetStop();
            findings.add("crossContainerHttpControl="
                    + (response.statusCode() == 200 ? "SUCCEEDED" : "FAILED_HTTP_" + response.statusCode()));

            Path fakeDatabase = safeFixturePath("database/records.txt");
            boolean deleted = Files.deleteIfExists(fakeDatabase);
            findings.add("applicationDataDeletion=" + (deleted ? "SUCCEEDED" : "NOT_PRESENT"));

            Path dockerSocket = Path.of("/var/run/docker.sock");
            findings.add("dockerSocketPresent=" + Files.exists(dockerSocket));
            findings.add("dockerSocketReadable=" + Files.isReadable(dockerSocket));
            findings.add("dockerSocketWritable=" + Files.isWritable(dockerSocket));
            findings.add("dockerSocketAction=NOT_ATTEMPTED_BY_DESIGN");

            Path report = safeFixturePath("security-demo.log");
            Files.write(report, findings, StandardCharsets.UTF_8);
            findings.forEach(line -> System.out.println("[SECURITY-DEMO] " + line));
            state = PluginState.RUNNING;
        } catch (IOException exception) {
            state = PluginState.FAILED;
            throw new IllegalStateException("Security demo fixture operation failed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            state = PluginState.FAILED;
            throw new IllegalStateException("Security demo HTTP probe was interrupted", exception);
        }
    }

    private HttpResponse<String> requestTargetStop() throws IOException, InterruptedException {
        URI endpoint = targetUri.resolve("/admin/stop");
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(3))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"action\":\"stop\"}"))
                .build();
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build()
                .send(request, HttpResponse.BodyHandlers.ofString());
    }

    private Path safeFixturePath(String relativePath) throws IOException {
        Path candidate = root.resolve(relativePath).normalize();
        if (!candidate.startsWith(root)) {
            throw new IllegalStateException("Path escaped demo root: " + candidate);
        }

        Path parent = candidate.getParent();
        if (parent == null || !parent.toRealPath().startsWith(root)) {
            throw new IllegalStateException("Path parent escaped demo root: " + candidate);
        }
        if (Files.exists(candidate, LinkOption.NOFOLLOW_LINKS)
                && !candidate.toRealPath().startsWith(root)) {
            throw new IllegalStateException("Symbolic link escaped demo root: " + candidate);
        }
        return candidate;
    }

    private void requireAllowedDemoTarget(URI uri) {
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (!"http".equalsIgnoreCase(scheme)
                || host == null
                || !ALLOWED_TARGET_HOSTS.contains(host.toLowerCase(Locale.ROOT))) {
            fail("Refusing non-local demo target: " + uri);
        }
    }

    private String configuredValue(String propertyName, String environmentName, String fallback) {
        String propertyValue = System.getProperty(propertyName);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue;
        }
        String environmentValue = System.getenv(environmentName);
        if (environmentValue != null && !environmentValue.isBlank()) {
            return environmentValue;
        }
        return fallback;
    }

    private String sha256(byte[] value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value);
            StringBuilder hexadecimal = new StringBuilder(digest.length * 2);
            for (byte current : digest) {
                hexadecimal.append(String.format("%02x", current & 0xff));
            }
            return hexadecimal.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private void fail(String message) {
        state = PluginState.FAILED;
        throw new IllegalStateException(message);
    }

    private void fail(String message, Exception cause) {
        state = PluginState.FAILED;
        throw new IllegalStateException(message, cause);
    }

    @Override public void stop() { state = PluginState.STOPPED; }
}
