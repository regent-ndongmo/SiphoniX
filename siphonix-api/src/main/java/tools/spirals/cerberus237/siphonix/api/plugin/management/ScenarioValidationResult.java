package tools.spirals.cerberus237.siphonix.api.plugin.management;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable value object describing the outcome of scenario validation.
 * <p>
 * A result combines a boolean validity marker and a list of human-readable validation errors.
 * Successful results typically expose an empty error list.
 * </p>
 *
 * @author Arléon Zemtsop (Cerberus)
 */
public class ScenarioValidationResult {

    private final boolean valid;
    private final List<String> errors;

    /**
     * Creates a new validation result.
     *
     * @param valid {@code true} when validation succeeded, {@code false} otherwise
     * @param errors validation error messages, null treated as an empty list
     */
    public ScenarioValidationResult(boolean valid, List<String> errors) {
        this.valid = valid;
        this.errors = errors == null ? new ArrayList<>() : new ArrayList<>(errors);
    }

    /**
     * Creates a successful validation result.
     *
     * @return a valid result with no errors
     */
    public static ScenarioValidationResult success() {
        return new ScenarioValidationResult(true, List.of());
    }

    /**
     * Creates a failed validation result.
     *
     * @param errors validation error messages
     * @return an invalid result containing provided errors
     */
    public static ScenarioValidationResult failure(List<String> errors) {
        return new ScenarioValidationResult(false, errors);
    }

    /**
     * Indicates whether validation succeeded.
     *
     * @return {@code true} when valid, {@code false} otherwise
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Returns an immutable view of validation errors.
     *
     * @return unmodifiable list of error messages
     */
    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }
}
