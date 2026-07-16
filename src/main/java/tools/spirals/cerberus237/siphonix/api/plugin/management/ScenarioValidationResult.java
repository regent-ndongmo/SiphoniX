package tools.spirals.cerberus237.siphonix.api.plugin.management;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScenarioValidationResult {

    private final boolean valid;
    private final List<String> errors;

    public ScenarioValidationResult(boolean valid, List<String> errors) {
        this.valid = valid;
        this.errors = errors == null ? new ArrayList<>() : new ArrayList<>(errors);
    }

    public static ScenarioValidationResult success() {
        return new ScenarioValidationResult(true, List.of());
    }

    public static ScenarioValidationResult failure(List<String> errors) {
        return new ScenarioValidationResult(false, errors);
    }

    public boolean isValid() {
        return valid;
    }

    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }
}
