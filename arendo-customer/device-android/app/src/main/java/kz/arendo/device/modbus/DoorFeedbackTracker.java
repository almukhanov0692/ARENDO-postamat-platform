package kz.arendo.device.modbus;

import java.util.Arrays;

/** Keeps door feedback honest across Modbus outages and reconnects. */
public final class DoorFeedbackTracker {
    public static final class Snapshot {
        private final String[] states;
        private final boolean[] changed;
        private final boolean[] transitions;

        private Snapshot(String[] states, boolean[] changed, boolean[] transitions) {
            this.states = states;
            this.changed = changed;
            this.transitions = transitions;
        }

        public String getState(int index) {
            return states[index];
        }

        public boolean hasChanged(int index) {
            return changed[index];
        }

        public boolean hasTransition(int index) {
            return transitions[index];
        }
    }

    private final ModbusCellMap cellMap;
    private final String[] states;
    private boolean initialized;

    public DoorFeedbackTracker(ModbusCellMap cellMap) {
        if (cellMap == null) {
            throw new IllegalArgumentException("Cell map is required");
        }
        this.cellMap = cellMap;
        states = new String[cellMap.getCells().size()];
        Arrays.fill(states, "unknown");
    }

    public synchronized Snapshot update(boolean[] inputs) {
        if (inputs == null || inputs.length != states.length) {
            throw new IllegalArgumentException("Input sample must match the cell map");
        }
        boolean[] changed = new boolean[states.length];
        boolean[] transitions = new boolean[states.length];
        for (int index = 0; index < states.length; index++) {
            if (!cellMap.getCell(index).hasDoorInput()) {
                continue;
            }
            String next = inputs[index] ? "closed" : "open";
            changed[index] = !next.equals(states[index]);
            transitions[index] = initialized && changed[index];
            states[index] = next;
        }
        initialized = true;
        return snapshot(changed, transitions);
    }

    public synchronized Snapshot markUnavailable() {
        boolean[] changed = new boolean[states.length];
        for (int index = 0; index < states.length; index++) {
            if (cellMap.getCell(index).hasDoorInput() && !"unknown".equals(states[index])) {
                states[index] = "unknown";
                changed[index] = true;
            }
        }
        initialized = false;
        return snapshot(changed, new boolean[states.length]);
    }

    public synchronized String getState(int index) {
        return states[index];
    }

    private Snapshot snapshot(boolean[] changed, boolean[] transitions) {
        return new Snapshot(states.clone(), changed, transitions);
    }
}
