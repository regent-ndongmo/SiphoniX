package tools.spirals.cerberus237.adaptiflow.plugin.core.config;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Contract for loading and saving SiphoniX runtime configuration.
 *
 * @author Arléon Zemtsop (Cerberus)
 */
public interface ConfigurationManager {

    /**
     * Loads configuration from a source path.
     *
     * @param path source path
     * @return parsed configuration model
     * @throws IOException when the source cannot be read or parsed
     */
    AdaptiflowConfiguration load(Path path) throws IOException;

    /**
     * Saves a configuration model to a destination path.
     *
     * @param path destination path
     * @param configuration configuration model to persist
     * @throws IOException when persistence fails
     */
    void save(Path path, AdaptiflowConfiguration configuration) throws IOException;
}
