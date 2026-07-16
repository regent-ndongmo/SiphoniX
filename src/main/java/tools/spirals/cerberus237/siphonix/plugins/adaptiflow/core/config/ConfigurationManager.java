package tools.spirals.cerberus237.siphonix.plugins.adaptiflow.core.config;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Contract for loading and saving SiphoniX runtime configuration.
 */
public interface ConfigurationManager {

    AdaptiflowConfiguration load(Path path) throws IOException;

    void save(Path path, AdaptiflowConfiguration configuration) throws IOException;
}
