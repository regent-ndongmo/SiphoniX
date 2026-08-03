package tools.spirals.cerberus237.adaptiflow.plugin.core.config;

/**
 * Raised when scenario configuration content is syntactically parseable but semantically invalid.
 * <p>
 * Typical causes include missing mandatory fields, unsupported component types, or invalid
 * constructor metadata.
 * </p>
 *
 * @author Arléon Zemtsop (Cerberus)
 */
public class InvalidConfigurationException extends RuntimeException {

    /**
     * Creates a new semantic configuration exception.
     *
     * @param message validation or mapping failure message
     */
    public InvalidConfigurationException(String message) {
        super(message);
    }
}
