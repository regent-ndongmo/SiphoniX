package tools.spirals.cerberus237.siphonix.kernel.loading;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

final class TestPluginJarBuilder {

    private TestPluginJarBuilder() {
    }

    static Path createBasicPluginJar(Path rootDir, String className, String pluginId) throws IOException {
        String source = "package test.plugins;\n"
                + "import tools.spirals.cerberus237.siphonix.api.plugin.Plugin;\n"
                + "import tools.spirals.cerberus237.siphonix.api.plugin.PluginContext;\n"
                + "import tools.spirals.cerberus237.siphonix.api.plugin.PluginState;\n"
                + "public class " + className + " implements Plugin {\n"
                + "  private PluginState state = PluginState.CREATED;\n"
                + "  public String getId() { return \"" + pluginId + "\"; }\n"
                + "  public String getVersion() { return \"1.0.0-test\"; }\n"
                + "  public PluginState getState() { return state; }\n"
                + "  public void initialize(PluginContext context) { state = PluginState.INITIALIZED; }\n"
                + "  public void start() { state = PluginState.RUNNING; }\n"
                + "  public void stop() { state = PluginState.STOPPED; }\n"
                + "}\n";
        return createPluginJar(rootDir, className, source,
                "tools.spirals.cerberus237.siphonix.api.plugin.Plugin");
    }

    static Path createScenarioManagementPluginJar(Path rootDir, String className, String pluginId)
            throws IOException {
        String source = "package test.plugins;\n"
                + "import java.io.IOException;\n"
                + "import java.util.ArrayList;\n"
                + "import java.util.List;\n"
                + "import tools.spirals.cerberus237.siphonix.api.plugin.PluginContext;\n"
                + "import tools.spirals.cerberus237.siphonix.api.plugin.PluginState;\n"
                + "import tools.spirals.cerberus237.siphonix.api.plugin.management.ScenarioManagementCli;\n"
                + "import tools.spirals.cerberus237.siphonix.api.plugin.management.ScenarioManagementPlugin;\n"
                + "import tools.spirals.cerberus237.siphonix.api.plugin.management.ScenarioManagementRestApi;\n"
                + "import tools.spirals.cerberus237.siphonix.api.plugin.management.ScenarioManagementService;\n"
                + "import tools.spirals.cerberus237.siphonix.api.plugin.management.ScenarioSource;\n"
                + "import tools.spirals.cerberus237.siphonix.api.plugin.management.ScenarioValidationResult;\n"
                + "public class " + className + " implements ScenarioManagementPlugin {\n"
                + "  private PluginState state = PluginState.CREATED;\n"
                + "  private final InMemoryService service = new InMemoryService();\n"
                + "  private final InMemoryRestApi restApi = new InMemoryRestApi(service);\n"
                + "  private final InMemoryCli cli = new InMemoryCli(service);\n"
                + "  public String getId() { return \"" + pluginId + "\"; }\n"
                + "  public String getVersion() { return \"1.0.0-test\"; }\n"
                + "  public PluginState getState() { return state; }\n"
                + "  public void initialize(PluginContext context) { state = PluginState.INITIALIZED; }\n"
                + "  public void start() { state = PluginState.RUNNING; }\n"
                + "  public void stop() { state = PluginState.STOPPED; }\n"
                + "  public ScenarioManagementService getScenarioManagementService() { return service; }\n"
                + "  public ScenarioManagementRestApi getScenarioManagementRestApi() { return restApi; }\n"
                + "  public ScenarioManagementCli getScenarioManagementCli() { return cli; }\n"
                + "  static class InMemoryService implements ScenarioManagementService {\n"
                + "    private final List<String> scenarios = new ArrayList<>();\n"
                + "    public void createScenario(ScenarioSource source) throws IOException { scenarios.add(source.getReference()); }\n"
                + "    public void updateScenario(String scenarioId, ScenarioSource source) throws IOException { }\n"
                + "    public void deleteScenario(String scenarioId) { scenarios.remove(scenarioId); }\n"
                + "    public void enableScenario(String scenarioId) { }\n"
                + "    public void disableScenario(String scenarioId) { }\n"
                + "    public List<String> listScenarios() { return new ArrayList<>(scenarios); }\n"
                + "    public ScenarioValidationResult validateScenario(ScenarioSource source) throws IOException { return ScenarioValidationResult.success(); }\n"
                + "  }\n"
                + "  static class InMemoryRestApi implements ScenarioManagementRestApi {\n"
                + "    private final ScenarioManagementService service;\n"
                + "    InMemoryRestApi(ScenarioManagementService service) { this.service = service; }\n"
                + "    public void postScenario(ScenarioSource source) throws IOException { service.createScenario(source); }\n"
                + "    public void putScenario(String scenarioId, ScenarioSource source) throws IOException { service.updateScenario(scenarioId, source); }\n"
                + "    public void deleteScenario(String scenarioId) { service.deleteScenario(scenarioId); }\n"
                + "    public void enableScenario(String scenarioId) { service.enableScenario(scenarioId); }\n"
                + "    public void disableScenario(String scenarioId) { service.disableScenario(scenarioId); }\n"
                + "  }\n"
                + "  static class InMemoryCli implements ScenarioManagementCli {\n"
                + "    private final ScenarioManagementService service;\n"
                + "    InMemoryCli(ScenarioManagementService service) { this.service = service; }\n"
                + "    public void applyScenario(String sourcePath) throws IOException { }\n"
                + "    public void updateScenario(String scenarioId, String sourcePath) throws IOException { }\n"
                + "    public void disableScenario(String scenarioId) { service.disableScenario(scenarioId); }\n"
                + "  }\n"
                + "}\n";
        return createPluginJar(rootDir, className, source,
                "tools.spirals.cerberus237.siphonix.api.plugin.management.ScenarioManagementPlugin");
    }

    static Path createEmptyJar(Path rootDir, String jarName) throws IOException {
        Path jarPath = rootDir.resolve(jarName);
        try (OutputStream outputStream = Files.newOutputStream(jarPath);
                JarOutputStream jarOutputStream = new JarOutputStream(outputStream)) {
            jarOutputStream.putNextEntry(new JarEntry("META-INF/"));
            jarOutputStream.closeEntry();
        }
        return jarPath;
    }

    private static Path createPluginJar(Path rootDir, String className, String source, String serviceType)
            throws IOException {
        Path sourceRoot = rootDir.resolve("source-" + className);
        Path classesRoot = rootDir.resolve("classes-" + className);
        Files.createDirectories(sourceRoot);
        Files.createDirectories(classesRoot);

        Path packageDir = sourceRoot.resolve("test/plugins");
        Files.createDirectories(packageDir);
        Path javaFile = packageDir.resolve(className + ".java");
        Files.writeString(javaFile, source, StandardCharsets.UTF_8);

        compile(javaFile, classesRoot);
        writeServicesFile(classesRoot, serviceType, "test.plugins." + className);

        Path jarPath = rootDir.resolve(className + ".jar");
        writeJar(classesRoot, jarPath);
        return jarPath;
    }

    private static void compile(Path javaFile, Path classesRoot) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("No Java compiler available in current runtime");
        }

        String classpath = System.getProperty("java.class.path");
        int result = compiler.run(
                null,
                null,
                null,
                "-classpath",
                classpath,
                "-d",
                classesRoot.toString(),
                javaFile.toString());

        if (result != 0) {
            throw new IllegalStateException("Failed to compile test plugin source " + javaFile + " (exit " + result + ")");
        }
    }

    private static void writeServicesFile(Path classesRoot, String serviceType, String implementationClass)
            throws IOException {
        Path servicesDir = classesRoot.resolve("META-INF/services");
        Files.createDirectories(servicesDir);
        Path serviceFile = servicesDir.resolve(serviceType);
        Files.writeString(serviceFile, implementationClass + "\n", StandardCharsets.UTF_8);
    }

    private static void writeJar(Path classesRoot, Path jarPath) throws IOException {
        List<Path> files = new ArrayList<>();
        Files.walk(classesRoot)
                .filter(Files::isRegularFile)
                .forEach(files::add);

        try (OutputStream outputStream = Files.newOutputStream(jarPath);
                JarOutputStream jarOutputStream = new JarOutputStream(outputStream)) {
            for (Path file : files) {
                String entryName = classesRoot.relativize(file).toString().replace('\\', '/');
                jarOutputStream.putNextEntry(new JarEntry(entryName));
                Files.copy(file, jarOutputStream);
                jarOutputStream.closeEntry();
            }
        }
    }
}
