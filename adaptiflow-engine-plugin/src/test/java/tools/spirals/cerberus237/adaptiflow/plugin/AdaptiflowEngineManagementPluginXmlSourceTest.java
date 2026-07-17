package tools.spirals.cerberus237.adaptiflow.plugin;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import tools.spirals.cerberus237.adaptiflow.plugin.runtime.AdaptiflowEngineManagementPlugin;
import tools.spirals.cerberus237.adaptiflow.plugin.runtime.XmlScenarioPlugin;
import tools.spirals.cerberus237.siphonix.api.plugin.management.ScenarioSource;

public class AdaptiflowEngineManagementPluginXmlSourceTest {

    @Test
    public void shouldCreateScenarioFromXmlFileSource() throws IOException {
        String scenarioId = "xml-service-source";
        Path xmlPath = writeXmlScenarioFile(scenarioId);

        AdaptiflowEngineManagementPlugin plugin = new AdaptiflowEngineManagementPlugin();
        plugin.getScenarioManagementService().createScenario(new FileScenarioSource("xml-file", xmlPath));

        Assert.assertTrue(plugin.getScenarioManagementService().listScenarios().contains(scenarioId));
        Assert.assertTrue(resolveRuntimePlugin(plugin, scenarioId) instanceof XmlScenarioPlugin);
    }

    @SuppressWarnings("unchecked")
    private Object resolveRuntimePlugin(AdaptiflowEngineManagementPlugin plugin, String scenarioId) {
        try {
            Field runtimePluginsField = AdaptiflowEngineManagementPlugin.class.getDeclaredField("runtimePlugins");
            runtimePluginsField.setAccessible(true);
            Map<String, Object> runtimePlugins = (Map<String, Object>) runtimePluginsField.get(plugin);
            return runtimePlugins.get(scenarioId);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("Unable to inspect runtime plugin registry", ex);
        }
    }

    @Test
    public void shouldCreateScenarioFromXmlPathThroughCli() throws IOException {
        Path xmlPath = writeXmlScenarioFile("xml-cli-source");

        AdaptiflowEngineManagementPlugin plugin = new AdaptiflowEngineManagementPlugin();
        plugin.getScenarioManagementCli().applyScenario(xmlPath.toString());

        Assert.assertTrue(plugin.getScenarioManagementService().listScenarios().contains("xml-cli-source"));
    }

    private Path writeXmlScenarioFile(String scenarioId) throws IOException {
        Path baseDirectory = Paths.get("target", "test-tmp", "management-xml");
        Files.createDirectories(baseDirectory);
        Path xmlPath = Files.createTempFile(baseDirectory, scenarioId + "-", ".xml");

        String xml = "<scenarios>\n"
                + "  <scenario id=\"" + scenarioId + "\" pluginId=\"adaptiflow.dynamic\" enabled=\"true\" intervalMs=\"1000\">\n"
                + "    <events>\n"
                + "      <event id=\"cpu-watch\" type=\"ConditionalEvent\">\n"
                + "        <collector type=\"ResourceUsageCollector\"/>\n"
                + "        <evaluators>\n"
                + "          <evaluator type=\"TrueEvaluator\"/>\n"
                + "        </evaluators>\n"
                + "        <actions>\n"
                + "          <action type=\"RestAdaptationAction\">\n"
                + "            <parameters>\n"
                + "              <parameter key=\"constructorArgTypes\">\n"
                + "                <list>\n"
                + "                  <value>java.util.List</value>\n"
                + "                  <value>java.lang.String</value>\n"
                + "                  <value>java.lang.String</value>\n"
                + "                </list>\n"
                + "              </parameter>\n"
                + "              <parameter key=\"constructorArgs\">\n"
                + "                <list>\n"
                + "                  <list>\n"
                + "                    <value>EnableExternalImageProvider</value>\n"
                + "                  </list>\n"
                + "                  <value>http://localhost/adapt</value>\n"
                + "                  <value>EnableExternalImageProvider</value>\n"
                + "                </list>\n"
                + "              </parameter>\n"
                + "            </parameters>\n"
                + "          </action>\n"
                + "        </actions>\n"
                + "      </event>\n"
                + "    </events>\n"
                + "  </scenario>\n"
                + "</scenarios>\n";

        Files.writeString(xmlPath, xml);
        return xmlPath;
    }

    private static final class FileScenarioSource implements ScenarioSource {

        private final String type;
        private final Path path;

        private FileScenarioSource(String type, Path path) {
            this.type = type;
            this.path = path;
        }

        @Override
        public String getType() {
            return type;
        }

        @Override
        public String getReference() {
            return path.toString();
        }

        @Override
        public Map<String, Object> load() {
            return Map.of();
        }
    }
}