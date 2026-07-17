package tools.spirals.cerberus237.adaptiflow.plugin.core.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import tools.spirals.cerberus237.adaptiflow.plugin.runtime.XmlScenarioMapParser;

/**
 * XML-backed configuration manager for scenarios.
 */
public class XmlConfigurationManager extends ScenarioConfigurationManager implements ConfigurationManager {

    @Override
    public AdaptiflowConfiguration load(Path path) throws IOException {
        if (!Files.exists(path)) {
            return new AdaptiflowConfiguration();
        }
        return loadFromMap(XmlScenarioMapParser.parse(path));
    }

    @Override
    public void save(Path path, AdaptiflowConfiguration configuration) {
        throw new UnsupportedOperationException("XML save is not supported yet");
    }
}