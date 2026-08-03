package tools.spirals.cerberus237.adaptiflow.plugin.core.config;

/**
 * Raised when a configuration file is structurally valid YAML but semantically invalid.
 */
public class InvalidConfigurationException extends RuntimeException {

    public InvalidConfigurationException(String message) {
        super(message);
    }
}
