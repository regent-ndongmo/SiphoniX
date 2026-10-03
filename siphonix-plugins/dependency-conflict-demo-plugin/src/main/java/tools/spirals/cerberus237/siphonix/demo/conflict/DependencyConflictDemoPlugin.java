package tools.spirals.cerberus237.siphonix.demo.conflict;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.event.SubstituteLoggingEvent;

import tools.spirals.cerberus237.siphonix.api.plugin.Plugin;
import tools.spirals.cerberus237.siphonix.api.plugin.PluginContext;
import tools.spirals.cerberus237.siphonix.api.plugin.PluginState;

/**
 * Demonstrates a real parent/plugin dependency collision.
 *
 * <p>This artifact deliberately embeds SLF4J 1.7.30 while SiphoniX embeds SLF4J 2.0.0. Because
 * {@code URLClassLoader} is parent-first, the host copy wins. The plugin stays operational so the
 * class origins and Maven versions can be observed in the runtime logs.</p>
 */
public final class DependencyConflictDemoPlugin implements Plugin {
    private static final String COORDINATE = "org.slf4j:slf4j-api";
    private static final String COMPILED_VERSION = "1.7.30";
    private static final String CLASS_RESOURCE = "org/slf4j/Logger.class";
    private static final String VERSION_RESOURCE = "META-INF/maven/org.slf4j/slf4j-api/pom.properties";

    private PluginState state = PluginState.CREATED;

    @Override public String getId() { return "demo-dependency-conflict-plugin"; }
    @Override public String getVersion() { return "2.0.0"; }
    @Override public PluginState getState() { return state; }
    @Override public void initialize(PluginContext context) { state = PluginState.INITIALIZED; }

    @Override public void start() {
        try {
            ClassLoader pluginClassLoader = getClass().getClassLoader();
            List<URL> classCopies = resources(pluginClassLoader, CLASS_RESOURCE);
            List<DependencyVersion> declaredVersions = dependencyVersions(pluginClassLoader);
            URL resolvedClass = Logger.class.getResource("Logger.class");
            String resolvedVersion = resolvedVersion(resolvedClass, declaredVersions);
            Set<String> classOrigins = new LinkedHashSet<>();
            for (URL copy : classCopies) {
                classOrigins.add(archiveOrigin(copy));
            }

            boolean conflictDetected = classOrigins.size() > 1
                    || declaredVersions.stream().map(version -> version.version).distinct().count() > 1;
            System.out.println("[DEPENDENCY-CONFLICT] status="
                    + (conflictDetected ? "DETECTED" : "NOT_DETECTED")
                    + " coordinate=" + COORDINATE
                    + " pluginCompiledAgainst=" + COMPILED_VERSION
                    + " runtimeResolvedVersion=" + resolvedVersion
                    + " classLoading=PARENT_FIRST");
            System.out.println("[DEPENDENCY-CONFLICT] resolvedClass=" + resolvedClass);
            for (URL copy : classCopies) {
                System.out.println("[DEPENDENCY-CONFLICT] duplicateClassCandidate=" + copy);
            }
            for (DependencyVersion version : declaredVersions) {
                System.out.println("[DEPENDENCY-CONFLICT] declaredVersion=" + version.version
                        + " origin=" + version.origin);
            }
            demonstrateBinaryIncompatibility();
            state = PluginState.RUNNING;
        } catch (IOException exception) {
            state = PluginState.FAILED;
            throw new IllegalStateException("Unable to inspect the dependency conflict", exception);
        }
    }

    /**
     * Calls an API that existed in SLF4J 1.7.30 but was removed in SLF4J 2.0.0.
     *
     * <p>The plugin compiles because its own dependency exposes {@code getMarker()}. At runtime,
     * parent-first delegation supplies the host's {@code SubstituteLoggingEvent} 2.0.0 class,
     * whose API exposes {@code getMarkers()} instead. The resulting {@link NoSuchMethodError} is
     * deliberately contained so the remainder of the risk demonstration can still run.</p>
     */
    private void demonstrateBinaryIncompatibility() {
        System.out.println("[DEPENDENCY-LINKAGE-ERROR] attemptedMethod="
                + "org.slf4j.event.SubstituteLoggingEvent.getMarker()");
        try {
            SubstituteLoggingEvent event = new SubstituteLoggingEvent();
            event.getMarker();
            System.out.println("[DEPENDENCY-LINKAGE-ERROR] status=NOT_REPRODUCED");
        } catch (NoSuchMethodError error) {
            System.out.println("[DEPENDENCY-LINKAGE-ERROR] status=EXPECTED_ERROR"
                    + " exception=" + error.getClass().getName()
                    + " message=" + error.getMessage());
            error.printStackTrace(System.out);
        }
    }

    private List<URL> resources(ClassLoader classLoader, String name) throws IOException {
        Enumeration<URL> enumeration = classLoader.getResources(name);
        List<URL> resources = Collections.list(enumeration);
        resources.sort((left, right) -> left.toExternalForm().compareTo(right.toExternalForm()));
        return resources;
    }

    private List<DependencyVersion> dependencyVersions(ClassLoader classLoader) throws IOException {
        List<DependencyVersion> versions = new ArrayList<>();
        for (URL resource : resources(classLoader, VERSION_RESOURCE)) {
            Properties properties = new Properties();
            try (InputStream input = resource.openStream()) {
                properties.load(input);
            }
            versions.add(new DependencyVersion(
                    properties.getProperty("version", "unknown"),
                    archiveOrigin(resource)));
        }
        return versions;
    }

    private String resolvedVersion(URL resolvedClass, List<DependencyVersion> versions) {
        String resolvedOrigin = archiveOrigin(resolvedClass);
        for (DependencyVersion version : versions) {
            if (version.origin.equals(resolvedOrigin)) {
                return version.version;
            }
        }
        Package loggerPackage = Logger.class.getPackage();
        if (loggerPackage != null && loggerPackage.getImplementationVersion() != null) {
            return loggerPackage.getImplementationVersion();
        }
        return "unknown";
    }

    private String archiveOrigin(URL resource) {
        if (resource == null) {
            return "unknown";
        }
        String external = resource.toExternalForm();
        int entrySeparator = external.indexOf("!/");
        return entrySeparator >= 0 ? external.substring(0, entrySeparator) : external;
    }

    private static final class DependencyVersion {
        private final String version;
        private final String origin;

        private DependencyVersion(String version, String origin) {
            this.version = version;
            this.origin = origin;
        }
    }

    @Override public void stop() { state = PluginState.STOPPED; }
}
