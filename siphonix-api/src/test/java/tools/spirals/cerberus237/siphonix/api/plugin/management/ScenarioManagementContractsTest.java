package tools.spirals.cerberus237.siphonix.api.plugin.management;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import tools.spirals.cerberus237.siphonix.api.plugin.PluginContext;
import tools.spirals.cerberus237.siphonix.api.plugin.PluginState;

public class ScenarioManagementContractsTest {

    @Test
    public void scenarioSourceCanExposeTypeReferenceAndPayload() throws IOException {
        ScenarioSource source = new InMemoryScenarioSource("yaml", "file:scenario.yaml", Map.of("id", "s-1"));

        assertEquals("yaml", source.getType());
        assertEquals("file:scenario.yaml", source.getReference());
        assertEquals("s-1", source.load().get("id"));
    }

    @Test
    public void scenarioManagementServiceTracksCoreOperations() throws IOException {
        InMemoryScenarioManagementService service = new InMemoryScenarioManagementService();
        ScenarioSource source = new InMemoryScenarioSource("json", "ref-1", Map.of("id", "scenario-A"));

        service.createScenario(source);
        service.updateScenario("scenario-A", source);
        service.enableScenario("scenario-A");
        service.disableScenario("scenario-A");

        assertEquals(List.of("scenario-A"), service.listScenarios());
        assertTrue(service.lastValidationResult.isValid());

        service.deleteScenario("scenario-A");
        assertTrue(service.listScenarios().isEmpty());
    }

    @Test
    public void scenarioManagementCliDelegatesToService() throws IOException {
        InMemoryScenarioManagementService service = new InMemoryScenarioManagementService();
        ScenarioManagementCli cli = new InMemoryScenarioManagementCli(service);

        cli.applyScenario("file:scenario-1.yaml");
        cli.updateScenario("scenario-1", "file:scenario-2.yaml");
        cli.disableScenario("scenario-1");

        assertEquals(1, service.applyPaths.size());
        assertEquals("file:scenario-1.yaml", service.applyPaths.get(0));
        assertEquals("file:scenario-2.yaml", service.updatePaths.get(0));
        assertEquals("scenario-1", service.disabledScenarios.get(0));
    }

    @Test
    public void scenarioManagementPluginExposesAllFacadeEndpoints() {
        InMemoryScenarioManagementService service = new InMemoryScenarioManagementService();
        ScenarioManagementRestApi restApi = new InMemoryScenarioManagementRestApi(service);
        ScenarioManagementCli cli = new InMemoryScenarioManagementCli(service);
        ScenarioManagementPlugin plugin = new InMemoryScenarioManagementPlugin(service, restApi, cli);

        PluginContext context = () -> "http://localhost:8080";
        plugin.initialize(context);
        plugin.start();
        plugin.stop();

        assertSame(service, plugin.getScenarioManagementService());
        assertSame(restApi, plugin.getScenarioManagementRestApi());
        assertSame(cli, plugin.getScenarioManagementCli());
        assertEquals(PluginState.STOPPED, plugin.getState());
    }

    private static final class InMemoryScenarioSource implements ScenarioSource {
        private final String type;
        private final String reference;
        private final Map<String, Object> payload;

        private InMemoryScenarioSource(String type, String reference, Map<String, Object> payload) {
            this.type = type;
            this.reference = reference;
            this.payload = payload;
        }

        @Override
        public String getType() {
            return type;
        }

        @Override
        public String getReference() {
            return reference;
        }

        @Override
        public Map<String, Object> load() {
            return payload;
        }
    }

    private static final class InMemoryScenarioManagementService implements ScenarioManagementService {
        private final List<String> scenarios = new ArrayList<>();
        private final List<String> applyPaths = new ArrayList<>();
        private final List<String> updatePaths = new ArrayList<>();
        private final List<String> disabledScenarios = new ArrayList<>();
        private ScenarioValidationResult lastValidationResult = ScenarioValidationResult.success();

        @Override
        public void createScenario(ScenarioSource source) throws IOException {
            String id = String.valueOf(source.load().get("id"));
            scenarios.add(id);
            applyPaths.add(source.getReference());
            lastValidationResult = ScenarioValidationResult.success();
        }

        @Override
        public void updateScenario(String scenarioId, ScenarioSource source) {
            if (!scenarios.contains(scenarioId)) {
                scenarios.add(scenarioId);
            }
            updatePaths.add(source.getReference());
        }

        @Override
        public void deleteScenario(String scenarioId) {
            scenarios.remove(scenarioId);
        }

        @Override
        public void enableScenario(String scenarioId) {
            if (!scenarios.contains(scenarioId)) {
                scenarios.add(scenarioId);
            }
        }

        @Override
        public void disableScenario(String scenarioId) {
            disabledScenarios.add(scenarioId);
        }

        @Override
        public List<String> listScenarios() {
            return new ArrayList<>(scenarios);
        }

        @Override
        public ScenarioValidationResult validateScenario(ScenarioSource source) {
            if (source == null) {
                lastValidationResult = ScenarioValidationResult.failure(List.of("source is null"));
                return lastValidationResult;
            }
            lastValidationResult = ScenarioValidationResult.success();
            return lastValidationResult;
        }
    }

    private static final class InMemoryScenarioManagementCli implements ScenarioManagementCli {
        private final InMemoryScenarioManagementService service;

        private InMemoryScenarioManagementCli(InMemoryScenarioManagementService service) {
            this.service = service;
        }

        @Override
        public void applyScenario(String sourcePath) throws IOException {
            service.createScenario(new InMemoryScenarioSource("yaml", sourcePath, Map.of("id", "scenario-1")));
        }

        @Override
        public void updateScenario(String scenarioId, String sourcePath) throws IOException {
            service.updateScenario(
                    scenarioId,
                    new InMemoryScenarioSource("yaml", sourcePath, Map.of("id", scenarioId)));
        }

        @Override
        public void disableScenario(String scenarioId) {
            service.disableScenario(scenarioId);
        }
    }

    private static final class InMemoryScenarioManagementRestApi implements ScenarioManagementRestApi {
        private final ScenarioManagementService service;

        private InMemoryScenarioManagementRestApi(ScenarioManagementService service) {
            this.service = service;
        }

        @Override
        public void postScenario(ScenarioSource source) throws IOException {
            service.createScenario(source);
        }

        @Override
        public void putScenario(String scenarioId, ScenarioSource source) throws IOException {
            service.updateScenario(scenarioId, source);
        }

        @Override
        public void deleteScenario(String scenarioId) {
            service.deleteScenario(scenarioId);
        }

        @Override
        public void enableScenario(String scenarioId) {
            service.enableScenario(scenarioId);
        }

        @Override
        public void disableScenario(String scenarioId) {
            service.disableScenario(scenarioId);
        }
    }

    private static final class InMemoryScenarioManagementPlugin implements ScenarioManagementPlugin {
        private final ScenarioManagementService service;
        private final ScenarioManagementRestApi restApi;
        private final ScenarioManagementCli cli;
        private PluginState state = PluginState.CREATED;

        private InMemoryScenarioManagementPlugin(
                ScenarioManagementService service,
                ScenarioManagementRestApi restApi,
                ScenarioManagementCli cli) {
            this.service = service;
            this.restApi = restApi;
            this.cli = cli;
        }

        @Override
        public ScenarioManagementService getScenarioManagementService() {
            return service;
        }

        @Override
        public ScenarioManagementRestApi getScenarioManagementRestApi() {
            return restApi;
        }

        @Override
        public ScenarioManagementCli getScenarioManagementCli() {
            return cli;
        }

        @Override
        public String getId() {
            return "scenario-management-plugin";
        }

        @Override
        public String getVersion() {
            return "1.0.0-test";
        }

        @Override
        public PluginState getState() {
            return state;
        }

        @Override
        public void initialize(PluginContext context) {
            state = PluginState.INITIALIZED;
        }

        @Override
        public void start() {
            state = PluginState.RUNNING;
        }

        @Override
        public void stop() {
            state = PluginState.STOPPED;
        }
    }
}
