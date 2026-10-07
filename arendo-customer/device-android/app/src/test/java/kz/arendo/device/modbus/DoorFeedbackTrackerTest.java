package kz.arendo.device.modbus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class DoorFeedbackTrackerTest {
    @Test
    public void outageInvalidatesFeedbackAndRecoveryOnlyEstablishesANewBaseline() {
        DoorFeedbackTracker tracker = new DoorFeedbackTracker(ModbusCellMap.exhibition10());
        boolean[] inputs = new boolean[10];
        inputs[0] = true;

        DoorFeedbackTracker.Snapshot first = tracker.update(inputs);
        assertEquals("closed", first.getState(0));
        assertTrue(first.hasChanged(0));
        assertFalse(first.hasTransition(0));
        assertEquals("unknown", first.getState(4));

        inputs[0] = false;
        DoorFeedbackTracker.Snapshot transition = tracker.update(inputs);
        assertEquals("open", transition.getState(0));
        assertTrue(transition.hasTransition(0));

        DoorFeedbackTracker.Snapshot unavailable = tracker.markUnavailable();
        assertEquals("unknown", unavailable.getState(0));
        assertTrue(unavailable.hasChanged(0));
        assertEquals("unknown", tracker.getState(0));

        inputs[0] = true;
        DoorFeedbackTracker.Snapshot recovered = tracker.update(inputs);
        assertEquals("closed", recovered.getState(0));
        assertTrue(recovered.hasChanged(0));
        assertFalse(recovered.hasTransition(0));
    }
}
