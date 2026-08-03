package tools.spirals.cerberus237.adaptiflow.operators;

import tools.spirals.cerberus237.adaptiflow.interfaces.ConditionEvaluator;

/**
 * Test-only evaluator used to verify dynamic class resolution by short name.
 */
public class TestExtensibleEvaluator implements ConditionEvaluator<Object> {

    @Override
    public boolean test(Object value) {
        return true;
    }
}
