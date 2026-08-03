package tools.spirals.cerberus237.adaptiflow.plugin.core.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * XML-backed configuration manager for scenarios.
 * <p>
 * XML documents are normalized to the shared map format by {@link XmlScenarioMapParser} before
 * domain mapping and validation.
 * </p>
 *
 * @author Arléon Zemtsop (Cerberus)
 */
public class XmlConfigurationManager extends ScenarioConfigurationManager implements ConfigurationManager {

    /**
     * Loads XML scenario configuration from disk.
     *
     * @param path XML file path
     * @return parsed configuration; empty model when file does not exist
     * @throws IOException when file cannot be read or parsed
     */
    @Override
    public AdaptiflowConfiguration load(Path path) throws IOException {
        if (!Files.exists(path)) {
            return new AdaptiflowConfiguration();
        }
        return loadFromMap(XmlScenarioMapParser.parse(path));
    }

    /**
     * XML serialization is intentionally not supported at the moment.
     *
     * @param path ignored
     * @param configuration ignored
     * @throws UnsupportedOperationException always
     */
    @Override
    public void save(Path path, AdaptiflowConfiguration configuration) {
        throw new UnsupportedOperationException("XML save is not supported yet");
    }
}