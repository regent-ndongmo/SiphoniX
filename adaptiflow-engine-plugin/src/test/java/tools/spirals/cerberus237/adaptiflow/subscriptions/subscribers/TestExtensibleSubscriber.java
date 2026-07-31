package tools.spirals.cerberus237.adaptiflow.subscriptions.subscribers;

import java.util.List;

import tools.spirals.cerberus237.adaptationactionsbase.core.IAdaptationAction;

/**
 * Test-only subscriber used to verify dynamic class resolution by short name.
 */
public class TestExtensibleSubscriber extends EventSubscriber<Object> {

    public TestExtensibleSubscriber(List<IAdaptationAction> actions) {
        super(actions);
    }
}
