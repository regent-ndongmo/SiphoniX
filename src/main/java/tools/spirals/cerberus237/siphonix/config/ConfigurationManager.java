package tools.spirals.cerberus237.siphonix.config;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Contract for loading and saving SiphoniX runtime configuration.
 */
public interface ConfigurationManager {

    SiphonixConfiguration load(Path path) throws IOException;

    void save(Path path, SiphonixConfiguration configuration) throws IOException;
}
