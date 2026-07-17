package tools.spirals.cerberus237.siphonix.kernel.loading;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class JsonFileScenarioSourceTest {

    @Test
    public void shouldExposeJsonTypeAndParseScenarioMap() throws IOException {
        Path jsonPath = createScenarioJson();

        JsonFileScenarioSource source = new JsonFileScenarioSource(jsonPath);
        Map<String, Object> root = source.load();

        Assert.assertEquals("json-file", source.getType());
        Assert.assertEquals(jsonPath.toString(), source.getReference());
        Assert.assertTrue(root.containsKey("scenarios"));
    }

    private Path createScenarioJson() throws IOException {
        Path baseDirectory = Paths.get("target", "test-tmp", "json-source");
        Files.createDirectories(baseDirectory);
        Path jsonPath = Files.createTempFile(baseDirectory, "scenario-", ".json");

        Files.writeString(jsonPath,
                "{\n"
                        + "  \"scenarios\": {\n"
                        + "    \"json-smoke\": {\n"
                        + "      \"pluginId\": \"adaptiflow.dynamic\",\n"
                        + "      \"enabled\": true,\n"
                        + "      \"intervalMs\": 1000,\n"
                        + "      \"events\": [\n"
                        + "        {\n"
                        + "          \"id\": \"cpu-watch\",\n"
                        + "          \"collector\": {\n"
                        + "            \"type\": \"ResourceUsageCollector\"\n"
                        + "          },\n"
                        + "          \"evaluators\": [\n"
                        + "            {\n"
                        + "              \"type\": \"TrueEvaluator\"\n"
                        + "            }\n"
                        + "          ],\n"
                        + "          \"actions\": [\n"
                        + "            {\n"
                        + "              \"type\": \"RestAdaptationAction\"\n"
                        + "            }\n"
                        + "          ]\n"
                        + "        }\n"
                        + "      ]\n"
                        + "    }\n"
                        + "  }\n"
                        + "}\n");
        return jsonPath;
    }
}