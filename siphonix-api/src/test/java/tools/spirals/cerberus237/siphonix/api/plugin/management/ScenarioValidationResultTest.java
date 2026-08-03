package tools.spirals.cerberus237.siphonix.api.plugin.management;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class ScenarioValidationResultTest {

    @Test
    public void successFactoryReturnsValidResultWithNoErrors() {
        ScenarioValidationResult result = ScenarioValidationResult.success();

        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void failureFactoryReturnsInvalidResultWithProvidedErrors() {
        List<String> errors = List.of("missing id", "invalid schema");

        ScenarioValidationResult result = ScenarioValidationResult.failure(errors);

        assertEquals(2, result.getErrors().size());
        assertEquals("missing id", result.getErrors().get(0));
        assertEquals("invalid schema", result.getErrors().get(1));
    }

    @Test
    public void constructorDefensivelyCopiesInputList() {
        List<String> mutableErrors = new ArrayList<>();
        mutableErrors.add("first");

        ScenarioValidationResult result = new ScenarioValidationResult(false, mutableErrors);
        mutableErrors.add("second");

        assertEquals(1, result.getErrors().size());
        assertEquals("first", result.getErrors().get(0));
    }

    @Test
    public void constructorTreatsNullErrorsAsEmptyList() {
        ScenarioValidationResult result = new ScenarioValidationResult(false, null);

        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void getErrorsReturnsUnmodifiableList() {
        ScenarioValidationResult result = ScenarioValidationResult.failure(List.of("error"));

        assertThrows(UnsupportedOperationException.class, () -> result.getErrors().add("other"));
    }
}
