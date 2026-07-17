package tools.spirals.cerberus237.siphonix.kernel.loading;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import tools.spirals.cerberus237.siphonix.api.plugin.management.ScenarioSource;

public class XmlFileScenarioSource implements ScenarioSource {

    private final Path path;

    public XmlFileScenarioSource(Path path) {
        this.path = path;
    }

    @Override
    public String getType() {
        return "xml-file";
    }

    @Override
    public String getReference() {
        return path.toString();
    }

    @Override
    public Map<String, Object> load() throws IOException {
        return XmlScenarioMapParser.parse(path);
    }
}